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
import com.example.dijasaliou.repository.PasswordResetTokenRepository;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
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

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
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
            throw new RuntimeException("Cette adresse email est déjà utilisée. Veuillez vous connecter ou utiliser une autre adresse.");
        }


        // 3. Déterminer le rôle de l'utilisateur
        // SÉCURITÉ : À l'inscription publique, seul le rôle ADMIN est autorisé.
        // SUPER_ADMIN ne peut être créé que manuellement en base (jamais via l'API).
        UserEntity.Role userRole = UserEntity.Role.ADMIN;
        if (request.getRole() != null && request.getRole() != UserEntity.Role.SUPER_ADMIN) {
            userRole = request.getRole();
        }

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

        // 3. Créer le TENANT (entreprise) - ESSAI GRATUIT DE 14 JOURS
        LocalDateTime now = LocalDateTime.now();

        TenantEntity tenant = TenantEntity.builder()
                .nomEntreprise(request.getNomEntreprise())
                .numeroTelephone(request.getNumeroTelephone())
                .adresse(request.getAdresseEntreprise())
                .nineaSiret(request.getNineaSiret()) // OPTIONNEL - peut être null
                .actif(true)
                .plan(TenantEntity.Plan.BUSINESS) // Essai BUSINESS complet pendant 14 jours
                .dateDebutEssai(now) // Début de l'essai
                .essaiUtilise(false) // L'essai n'a pas encore été utilisé
                .dateExpiration(now.plusDays(14)) // Expire dans 14 jours → rétrograde vers GRATUIT
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

        // 6. Générer le token JWT avec tenant_id et rôle (évite la requête BDD à chaque appel API)
        String token = jwtService.generateToken(savedUser.getEmail(), savedTenant.getTenantUuid(), savedUser.getRole());

        // 7. Créer et envoyer le token de vérification email (non bloquant)
        PasswordResetToken verificationToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(savedUser)
                .type("EMAIL_VERIFICATION")
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        passwordResetTokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken.getToken(), savedUser.getPrenom());

        // 8. Retourner la réponse - L'utilisateur a 14 jours d'essai gratuit
        return AuthResponse.builder()
                .token(token)
                .user(savedUser)
                .emailVerifie(false)
                .requiresPayment(false)
                .plan(savedTenant.getPlan())
                .build();
    }

    /**
     * Renvoie l'email de vérification
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Aucun compte trouvé pour cet email."));

        if (Boolean.TRUE.equals(user.getEmailVerifie())) {
            throw new RuntimeException("Votre adresse email est déjà vérifiée.");
        }

        // Supprimer les anciens tokens EMAIL_VERIFICATION de cet utilisateur
        passwordResetTokenRepository.deleteByUserAndType(user, "EMAIL_VERIFICATION");

        // Créer un nouveau token
        PasswordResetToken verificationToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .type("EMAIL_VERIFICATION")
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        passwordResetTokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken(), user.getPrenom());
    }

    /**
     * Retourne les infos à jour de l'utilisateur connecté
     */
    public AuthResponse getMe(String email) {
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (user.getRole() == UserEntity.Role.SUPER_ADMIN) {
            return AuthResponse.builder()
                    .user(user)
                    .emailVerifie(true)
                    .requiresPayment(false)
                    .plan(null)
                    .build();
        }

        TenantEntity tenant = user.getTenant();
        boolean requiresPayment = false;
        if (tenant != null) {
            if (tenant.getPlan() == TenantEntity.Plan.GRATUIT) {
                requiresPayment = !tenant.essaiGratuitValide();
            } else {
                requiresPayment = tenant.getDateExpiration() != null &&
                        tenant.getDateExpiration().isBefore(LocalDateTime.now());
            }
        }

        return AuthResponse.builder()
                .user(user)
                .emailVerifie(Boolean.TRUE.equals(user.getEmailVerifie()))
                .requiresPayment(requiresPayment)
                .plan(tenant != null ? tenant.getPlan() : null)
                .build();
    }

    /**
     * Vérifie l'email via le token reçu par email
     */
    @Transactional
    public void verifyEmail(String token) {
        PasswordResetToken verificationToken = passwordResetTokenRepository
                .findByTokenAndType(token, "EMAIL_VERIFICATION")
                .orElseThrow(() -> new RuntimeException("Lien de vérification invalide ou expiré."));

        if (!verificationToken.isValid()) {
            throw new RuntimeException("Ce lien de vérification a expiré. Veuillez vous reconnecter pour en recevoir un nouveau.");
        }

        UserEntity user = verificationToken.getUser();
        user.setEmailVerifie(true);
        userRepository.save(user);

        verificationToken.markAsUsed();
        passwordResetTokenRepository.save(verificationToken);
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
            throw new RuntimeException("Email ou mot de passe incorrect. Vérifiez vos informations et réessayez.");
        }

        // 3. SUPER_ADMIN : pas de tenant, accès direct
        if (user.getRole() == UserEntity.Role.SUPER_ADMIN) {
            String token = jwtService.generateToken(user.getEmail(), null, user.getRole());
            return AuthResponse.builder()
                    .token(token)
                    .user(user)
                    .emailVerifie(true)
                    .requiresPayment(false)
                    .plan(null)
                    .build();
        }

        // 4. Vérifier que le tenant existe et est actif
        TenantEntity tenant = user.getTenant();
        if (tenant == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        // IMPORTANT : On permet la connexion même si l'abonnement est expiré
        // Le SubscriptionExpirationFilter bloquera l'accès aux routes protégées
        // Mais l'utilisateur doit pouvoir se connecter pour accéder à /payment/**
        if (!tenant.getActif()) {
            throw new RuntimeException("Votre compte a été suspendu. Contactez le support sur WhatsApp.");
        }

        // 4. Enregistrer la date de dernière connexion
        user.setDerniereConnexion(LocalDateTime.now());
        userRepository.save(user);

        // 5. Générer le token JWT avec tenant_id et rôle
        String token = jwtService.generateToken(user.getEmail(), tenant.getTenantUuid(), user.getRole());

        // 5. Vérifier si un paiement est requis
        // Un paiement est requis si :
        // - Le tenant est en plan GRATUIT ET l'essai gratuit est terminé
        // - OU le tenant a un plan payant mais l'abonnement est expiré
        boolean requiresPayment = false;

        if (tenant.getPlan() == TenantEntity.Plan.GRATUIT) {
            // Si plan gratuit, vérifier si l'essai gratuit est encore valide
            requiresPayment = !tenant.essaiGratuitValide();
        } else {
            // Si plan payant, vérifier si l'abonnement est expiré
            requiresPayment = tenant.getDateExpiration() != null &&
                             tenant.getDateExpiration().isBefore(LocalDateTime.now());
        }

        // 6. Retourner la réponse avec les informations de paiement
        return AuthResponse.builder()
                .token(token)
                .user(user)
                .emailVerifie(Boolean.TRUE.equals(user.getEmailVerifie()))
                .requiresPayment(requiresPayment)
                .plan(tenant.getPlan())
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

        // 4. Soft delete de tous les utilisateurs du tenant
        LocalDateTime maintenant = LocalDateTime.now();
        List<UserEntity> utilisateurs = userRepository.findByTenantIdAndDeletedFalse(tenant.getId());
        for (UserEntity utilisateur : utilisateurs) {
            utilisateur.setDeleted(true);
            utilisateur.setDateSuppression(maintenant);
            passwordResetTokenRepository.deleteByUser(utilisateur);
        }
        userRepository.saveAll(utilisateurs);

        // 5. Soft delete du tenant — données conservées mais inaccessibles
        tenant.setDeleted(true);
        tenant.setDateSuppression(maintenant);
        tenant.setActif(false);
        tenantRepository.save(tenant);
    }
}
