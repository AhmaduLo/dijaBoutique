package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.AuthResponse;
import com.example.dijasaliou.dto.LoginRequest;
import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.jwt.JwtService;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
        // 1. Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
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
        // 1. Trouver l'utilisateur par email
        UserEntity user = userRepository.findByEmail(request.getEmail())
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
}
