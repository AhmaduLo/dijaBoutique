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
     * PROCESSUS :
     * 1. Vérifier que l'email n'existe pas déjà
     * 2. Créer le TENANT (entreprise)
     * 3. Créer l'utilisateur ADMIN lié au tenant
     * 4. Générer un token JWT avec tenant_id
     * 5. Retourner la réponse avec le token
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

        // 3. Créer le TENANT (entreprise)
        TenantEntity tenant = TenantEntity.builder()
                .nomEntreprise(request.getNomEntreprise())
                .numeroTelephone(request.getNumeroTelephone())
                .actif(true)
                .plan(TenantEntity.Plan.GRATUIT) // Par défaut, plan gratuit
                .build();

        TenantEntity savedTenant = tenantRepository.save(tenant);

        // 4. Créer l'utilisateur ADMIN pour ce tenant
        UserEntity user = UserEntity.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse())) // Hash !
                .nomEntreprise(request.getNomEntreprise())
                .numeroTelephone(request.getNumeroTelephone())
                .tenant(savedTenant) // CRITIQUE : Lier au tenant
                .role(UserEntity.Role.ADMIN) // Premier utilisateur = ADMIN
                .build();

        UserEntity savedUser = userRepository.save(user);

        // 5. Générer le token JWT avec tenant_id
        String token = jwtService.generateToken(savedUser.getEmail(), savedTenant.getTenantUuid());

        // 6. Retourner la réponse
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
}
