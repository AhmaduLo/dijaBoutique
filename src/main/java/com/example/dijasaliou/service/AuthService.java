package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.AuthResponse;
import com.example.dijasaliou.dto.ForgotPasswordRequest;
import com.example.dijasaliou.dto.LoginRequest;
import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.dto.ResetPasswordRequest;
import com.example.dijasaliou.entity.PasswordResetToken;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.jwt.JwtService;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.repository.DepenseRepository;
import com.example.dijasaliou.repository.PasswordResetTokenRepository;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.repository.VenteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service pour l'authentification
 *
 * Gère :
 * - Inscription (register)
 * - Connexion (login)
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final AchatRepository achatRepository;
    private final VenteRepository venteRepository;
    private final DepenseRepository depenseRepository;
    private final StripeService stripeService;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService,
                       AchatRepository achatRepository,
                       VenteRepository venteRepository,
                       DepenseRepository depenseRepository,
                       StripeService stripeService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.achatRepository = achatRepository;
        this.venteRepository = venteRepository;
        this.depenseRepository = depenseRepository;
        this.stripeService = stripeService;
    }

    /**
     * INSCRIPTION - MODE SAAS MULTI-TENANT
     *
     * IMPORTANT : Chaque inscription crée :
     * 1. Un nouveau TENANT (entreprise)
     * 2. Un premier utilisateur ADMIN pour ce tenant
     *
     * SÉCURITÉ :
     * - L'UUID du tenant est généré automatiquement (impossible à deviner)
     * - Le tenant_id est inclus dans le JWT
     * - Toutes les données futures seront isolées par tenant_id
     *
     * FLUX D'INSCRIPTION ET PAIEMENT :
     * 1. L'utilisateur s'inscrit → Compte créé avec plan GRATUIT (expiré)
     * 2. L'utilisateur reçoit un JWT et est connecté
     * 3. Le SubscriptionExpirationFilter bloque l'accès aux routes sauf /payment/**
     * 4. L'utilisateur est redirigé vers la page de paiement
     * 5. Après paiement réussi → L'abonnement est activé
     *
     * PROCESSUS :
     * 1. Vérifier que l'email n'existe pas déjà
     * 2. Vérifier l'acceptation des CGU et de la Politique de Confidentialité pour les ADMIN
     * 3. Créer le TENANT (entreprise) avec plan GRATUIT et dateExpiration = now
     * 4. Créer l'utilisateur ADMIN lié au tenant
     * 5. Générer un token JWT avec tenant_id
     * 6. Retourner la réponse avec le token
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Vérifier si l'email existe déjà (parmi les utilisateurs actifs)
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        // 2. Vérifier si l'entreprise existe déjà (optionnel, mais recommandé)
        if (tenantRepository.existsByNomEntreprise(request.getNomEntreprise())) {
            throw new RuntimeException("Une entreprise avec ce nom existe déjà");
        }

        // 3. Déterminer le rôle de l'utilisateur
        // Si aucun rôle n'est spécifié, utiliser ADMIN par défaut (premier utilisateur)
        UserEntity.Role userRole = request.getRole() != null ? request.getRole() : UserEntity.Role.ADMIN;

        // 4. VALIDATION DES DOCUMENTS LÉGAUX pour les ADMIN
        // L'acceptation des CGU et de la Politique de Confidentialité est OBLIGATOIRE pour les ADMIN
        if (userRole == UserEntity.Role.ADMIN) {
            if (request.getAcceptationCGU() == null || !request.getAcceptationCGU()) {
                throw new RuntimeException("Vous devez accepter les Conditions Générales d'Utilisation pour créer un compte administrateur");
            }
            if (request.getAcceptationPolitiqueConfidentialite() == null || !request.getAcceptationPolitiqueConfidentialite()) {
                throw new RuntimeException("Vous devez accepter la Politique de Confidentialité pour créer un compte administrateur");
            }
        }

        // 3. Créer le TENANT (entreprise) - PAIEMENT REQUIS APRÈS INSCRIPTION
        LocalDateTime now = LocalDateTime.now();

        TenantEntity tenant = TenantEntity.builder()
                .nomEntreprise(request.getNomEntreprise())
                .numeroTelephone(request.getNumeroTelephone())
                .adresse(request.getAdresseEntreprise())
                .nineaSiret(request.getNineaSiret()) // OPTIONNEL - peut être null
                .actif(true)
                .plan(TenantEntity.Plan.GRATUIT) // Plan par défaut - doit payer pour accéder
                .dateExpiration(now) // Expiré immédiatement - l'utilisateur doit payer
                .build();

        TenantEntity savedTenant = tenantRepository.save(tenant);

        // 5. Créer l'utilisateur ADMIN pour ce tenant avec les acceptations des documents légaux

        UserEntity user = UserEntity.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse())) // Hash !
                .nomEntreprise(request.getNomEntreprise())
                .numeroTelephone(request.getNumeroTelephone())
                .tenant(savedTenant) // CRITIQUE : Lier au tenant
                .role(userRole) // Utiliser le rôle spécifié ou ADMIN par défaut
                .acceptationCGU(userRole == UserEntity.Role.ADMIN ? request.getAcceptationCGU() : false)
                .dateAcceptationCGU(userRole == UserEntity.Role.ADMIN && request.getAcceptationCGU() ? now : null)
                .acceptationPolitiqueConfidentialite(userRole == UserEntity.Role.ADMIN ? request.getAcceptationPolitiqueConfidentialite() : false)
                .dateAcceptationPolitique(userRole == UserEntity.Role.ADMIN && request.getAcceptationPolitiqueConfidentialite() ? now : null)
                .build();

        UserEntity savedUser = userRepository.save(user);

        // 6. Générer le token JWT avec tenant_id
        String token = jwtService.generateToken(savedUser.getEmail(), savedTenant.getTenantUuid());

        // 7. Retourner la réponse
        return AuthResponse.builder()
                .token(token)
                .user(savedUser)
                .build();
    }

    /**
     * CONNEXION - MODE MULTI-TENANT
     *
     * IMPORTANT : Le token JWT inclut maintenant le tenant_id
     * Cela garantit que l'utilisateur ne voit que les données de son entreprise
     *
     * PROCESSUS :
     * 1. Vérifier que l'utilisateur existe
     * 2. Vérifier le mot de passe
     * 3. Vérifier que le tenant est actif
     * 4. Générer un token JWT avec tenant_id
     * 5. Retourner la réponse avec le token
     */
    public AuthResponse login(LoginRequest request) {
        // 1. Trouver l'utilisateur par email (non supprimé uniquement)
        UserEntity user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        // 2. Vérifier le mot de passe
        if (!passwordEncoder.matches(request.getMotDePasse(), user.getMotDePasse())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        // 3. Vérifier que le tenant existe et est actif
        TenantEntity tenant = user.getTenant();
        if (tenant == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        if (!tenant.estValide()) {
            throw new RuntimeException("Votre compte entreprise est désactivé ou expiré. Contactez le support.");
        }

        // 4. Générer le token JWT avec tenant_id
        String token = jwtService.generateToken(user.getEmail(), tenant.getTenantUuid());

        // 5. Retourner la réponse
        return AuthResponse.builder()
                .token(token)
                .user(user)
                .build();
    }

    /**
     * MOT DE PASSE OUBLIÉ - Étape 1 : Demande de réinitialisation
     *
     * PROCESSUS :
     * 1. Vérifier que l'utilisateur existe
     * 2. Supprimer les anciens tokens de réinitialisation (s'il y en a)
     * 3. Générer un nouveau token unique (UUID)
     * 4. Stocker le token avec une date d'expiration (1 heure)
     * 5. Envoyer un email avec le lien de réinitialisation
     *
     * SÉCURITÉ :
     * - Ne pas révéler si l'email existe ou non (protection contre l'énumération)
     * - Token unique et impossible à deviner (UUID)
     * - Expiration automatique après 1 heure
     * - Un seul token valide à la fois par utilisateur
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // 1. Rechercher l'utilisateur par email (non supprimé uniquement)
        UserEntity user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElse(null);

        // SÉCURITÉ : Même si l'utilisateur n'existe pas ou est supprimé, on ne révèle pas cette information
        // pour éviter l'énumération des comptes
        if (user == null) {
            // On fait semblant que tout s'est bien passé
            return;
        }

        // 2. Supprimer les anciens tokens de réinitialisation de cet utilisateur
        passwordResetTokenRepository.deleteByUser(user);

        // 3. Générer un nouveau token unique
        String token = UUID.randomUUID().toString();

        // 4. Créer et sauvegarder le token avec expiration dans 1 heure
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // 5. Envoyer l'email avec le lien de réinitialisation
        String userName = user.getPrenom() + " " + user.getNom();
        emailService.sendPasswordResetEmail(user.getEmail(), token, userName);
    }

    /**
     * MOT DE PASSE OUBLIÉ - Étape 2 : Réinitialisation avec le token
     *
     * PROCESSUS :
     * 1. Vérifier que le token existe
     * 2. Vérifier que le token n'a pas expiré
     * 3. Vérifier que le token n'a pas déjà été utilisé
     * 4. Vérifier que les mots de passe correspondent
     * 5. Hasher et sauvegarder le nouveau mot de passe
     * 6. Marquer le token comme utilisé
     * 7. Supprimer tous les autres tokens de cet utilisateur
     *
     * SÉCURITÉ :
     * - Validation stricte du token (existence, expiration, utilisation unique)
     * - Hash du mot de passe avec BCrypt
     * - Invalidation de tous les tokens après utilisation
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Vérifier que les mots de passe correspondent
        if (!request.getNouveauMotDePasse().equals(request.getConfirmationMotDePasse())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        // 2. Rechercher le token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token de réinitialisation invalide ou expiré"));

        // 3. Vérifier que le token est encore valide (non utilisé et non expiré)
        if (!resetToken.isValid()) {
            throw new RuntimeException("Token de réinitialisation invalide ou expiré");
        }

        // 4. Récupérer l'utilisateur
        UserEntity user = resetToken.getUser();

        // 5. Mettre à jour le mot de passe (avec hash)
        user.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        userRepository.save(user);

        // 6. Marquer le token comme utilisé
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        // 7. Supprimer tous les autres tokens de cet utilisateur
        passwordResetTokenRepository.deleteByUser(user);
    }

    /**
     * SUPPRESSION DU COMPTE ADMIN ET DE TOUTES LES DONNÉES ASSOCIÉES
     *
     * ATTENTION : Cette opération est IRRÉVERSIBLE !
     *
     * PROCESSUS :
     * 1. Vérifier que l'utilisateur est un ADMIN
     * 2. Récupérer le tenant de l'admin
     * 3. Récupérer tous les utilisateurs du tenant
     * 4. Pour chaque utilisateur, supprimer manuellement :
     *    - Tous les achats
     *    - Toutes les ventes
     *    - Toutes les dépenses
     *    - Tous les tokens de réinitialisation
     * 5. Supprimer le tenant (la cascade supprimera les utilisateurs)
     *
     * SÉCURITÉ :
     * - Seul l'admin peut supprimer son propre compte
     * - Vérification du rôle ADMIN obligatoire
     * - Suppression manuelle des données avant suppression du tenant
     *
     * @param emailAdmin Email de l'admin qui souhaite supprimer son compte
     * @throws RuntimeException si l'utilisateur n'est pas un ADMIN ou n'existe pas
     */
    @Transactional
    public void deleteAdminAccount(String emailAdmin) {
        // 1. Trouver l'utilisateur par email
        UserEntity user = userRepository.findByEmailAndDeletedFalse(emailAdmin)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 2. Vérifier que l'utilisateur est bien un ADMIN
        if (user.getRole() != UserEntity.Role.ADMIN) {
            throw new RuntimeException("Seul un administrateur peut supprimer le compte de l'entreprise");
        }

        // 3. Récupérer le tenant
        TenantEntity tenant = user.getTenant();
        if (tenant == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        // 4. Récupérer tous les utilisateurs du tenant
        List<UserEntity> utilisateurs = tenant.getUtilisateurs();

        // 5. Pour chaque utilisateur, supprimer manuellement toutes les données associées
        for (UserEntity utilisateur : utilisateurs) {
            // Supprimer tous les achats
            achatRepository.deleteByUtilisateur(utilisateur);

            // Supprimer toutes les ventes
            venteRepository.deleteByUtilisateur(utilisateur);

            // Supprimer toutes les dépenses
            depenseRepository.deleteByUtilisateur(utilisateur);

            // Supprimer tous les tokens de réinitialisation
            passwordResetTokenRepository.deleteByUser(utilisateur);
        }

        // 6. Supprimer le tenant (la cascade supprimera automatiquement tous les utilisateurs)
        tenantRepository.delete(tenant);
    }
}
