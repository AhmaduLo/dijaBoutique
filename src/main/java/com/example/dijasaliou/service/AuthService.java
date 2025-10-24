package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.AuthResponse;
import com.example.dijasaliou.dto.LoginRequest;
import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.jwt.JwtService;
import com.example.dijasaliou.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * INSCRIPTION (TEMPORAIREMENT ACTIVÉE)
     *
     * ⚠️ MODE DÉVELOPPEMENT : INSCRIPTION PUBLIQUE ACTIVÉE ⚠️
     *
     * En production, désactivez ceci et utilisez uniquement /api/admin/utilisateurs
     *
     * 1. Vérifier que l'email n'existe pas déjà
     * 2. Hasher le mot de passe
     * 3. Créer l'utilisateur
     * 4. Générer un token JWT
     * 5. Retourner la réponse avec le token
     */
    public AuthResponse register(RegisterRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        // Créer l'utilisateur
        UserEntity user = UserEntity.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse())) // Hash !
                .role(request.getRole() != null ? request.getRole() : UserEntity.Role.USER)
                .build();

        // Sauvegarder
        UserEntity savedUser = userRepository.save(user);

        // Générer le token JWT
        String token = jwtService.generateToken(savedUser.getEmail());

        // Retourner la réponse
        return AuthResponse.builder()
                .token(token)
                .user(savedUser)
                .build();
    }

    /**
     * CONNEXION
     *
     * 1. Vérifier que l'utilisateur existe
     * 2. Vérifier le mot de passe
     * 3. Générer un token JWT
     * 4. Retourner la réponse avec le token
     */
    public AuthResponse login(LoginRequest request) {
        // Trouver l'utilisateur par email
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(request.getMotDePasse(), user.getMotDePasse())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        // Générer le token JWT
        String token = jwtService.generateToken(user.getEmail());

        // Retourner la réponse
        return AuthResponse.builder()
                .token(token)
                .user(user)
                .build();
    }
}
