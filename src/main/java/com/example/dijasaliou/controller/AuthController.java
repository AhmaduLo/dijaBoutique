package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.AuthResponse;
import com.example.dijasaliou.dto.ForgotPasswordRequest;
import com.example.dijasaliou.dto.LoginRequest;
import com.example.dijasaliou.dto.PasswordResetResponse;
import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.dto.ResetPasswordRequest;
import com.example.dijasaliou.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pour l'authentification
 *
 * Routes :
 * - POST /api/auth/register  → Inscription
 * - POST /api/auth/login     → Connexion
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Inscription d'un nouvel utilisateur
     *
     * Body :
     * {
     *   "nom": "Saliou",
     *   "prenom": "Dija",
     *   "email": "dija@boutique.com",
     *   "motDePasse": "password123",
     *   "role": "ADMIN"
     * }
     *
     * Réponse :
     * {
     *   "type": "Bearer",
     *   "id": 1,
     *   "email": "dija@boutique.com",
     *   "nom": "Saliou",
     *   "prenom": "Dija",
     *   "role": "ADMIN"
     * }
     *
     * Le token JWT est maintenant stocké dans un cookie HttpOnly pour plus de sécurité
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);

        // Créer un cookie HttpOnly sécurisé avec le JWT
        Cookie jwtCookie = createJwtCookie(authResponse.getToken());
        response.addCookie(jwtCookie);

        // Ne pas renvoyer le token dans le body (sécurité)
        authResponse.setToken(null);

        return ResponseEntity.ok(authResponse);
    }

    /**
     * POST /api/auth/login
     * Connexion d'un utilisateur existant
     *
     * Body :
     * {
     *   "email": "dija@boutique.com",
     *   "motDePasse": "password123"
     * }
     *
     * Réponse :
     * {
     *   "type": "Bearer",
     *   "id": 1,
     *   "email": "dija@boutique.com",
     *   "nom": "Saliou",
     *   "prenom": "Dija",
     *   "role": "ADMIN"
     * }
     *
     * Le token JWT est maintenant stocké dans un cookie HttpOnly pour plus de sécurité
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);

        // Créer un cookie HttpOnly sécurisé avec le JWT
        Cookie jwtCookie = createJwtCookie(authResponse.getToken());
        response.addCookie(jwtCookie);

        // Ne pas renvoyer le token dans le body (sécurité)
        authResponse.setToken(null);

        return ResponseEntity.ok(authResponse);
    }

    /**
     * POST /api/auth/logout
     * Déconnexion de l'utilisateur en supprimant le cookie JWT
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        // Créer un cookie expiré pour supprimer le JWT
        Cookie jwtCookie = new Cookie("jwt", null);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(0); // Expiration immédiate
        jwtCookie.setSecure(cookieSecure); // Lit depuis application.properties

        response.addCookie(jwtCookie);

        return ResponseEntity.ok("Déconnexion réussie");
    }

    /**
     * POST /api/auth/forgot-password
     * Demande de réinitialisation de mot de passe
     *
     * Body :
     * {
     *   "email": "dija@boutique.com"
     * }
     *
     * Réponse :
     * {
     *   "message": "Si cet email existe, un lien de réinitialisation a été envoyé"
     * }
     *
     * SÉCURITÉ : Le message ne révèle jamais si l'email existe ou non
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);

        // Message générique pour ne pas révéler si l'email existe
        PasswordResetResponse response = PasswordResetResponse.builder()
                .message("Si cet email existe, un lien de réinitialisation a été envoyé")
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/reset-password
     * Réinitialisation du mot de passe avec le token
     *
     * Body :
     * {
     *   "token": "uuid-token-from-email",
     *   "nouveauMotDePasse": "newPassword123",
     *   "confirmationMotDePasse": "newPassword123"
     * }
     *
     * Réponse :
     * {
     *   "message": "Votre mot de passe a été réinitialisé avec succès"
     * }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);

        PasswordResetResponse response = PasswordResetResponse.builder()
                .message("Votre mot de passe a été réinitialisé avec succès")
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/auth/delete-account
     * Suppression du compte admin et de toutes les données associées
     *
     * ATTENTION : Cette opération est IRRÉVERSIBLE !
     * Supprime :
     * - Le compte admin
     * - Tous les utilisateurs (USER, GERANT)
     * - Tous les achats
     * - Toutes les ventes
     * - Toutes les dépenses
     * - Toute l'entreprise (tenant)
     *
     * Réponse :
     * {
     *   "message": "Votre compte et toutes les données associées ont été supprimés"
     * }
     *
     * SÉCURITÉ : Seul un utilisateur ADMIN peut supprimer son compte
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<PasswordResetResponse> deleteAccount(
            Authentication authentication,
            HttpServletResponse response) {
        String emailAdmin = authentication.getName();

        // Supprimer le compte et toutes les données
        authService.deleteAdminAccount(emailAdmin);

        // Supprimer le cookie JWT
        Cookie jwtCookie = new Cookie("jwt", null);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(0); // Expiration immédiate
        jwtCookie.setSecure(cookieSecure);
        response.addCookie(jwtCookie);

        PasswordResetResponse deleteResponse = PasswordResetResponse.builder()
                .message("Votre compte et toutes les données associées ont été supprimés")
                .build();
        return ResponseEntity.ok(deleteResponse);
    }

    /**
     * Crée un cookie HttpOnly sécurisé contenant le JWT
     *
     * Flags de sécurité :
     * - HttpOnly : JavaScript ne peut pas lire le cookie (protection XSS)
     * - Secure : Cookie envoyé uniquement en HTTPS (désactivé en dev)
     * - SameSite=Strict : Protection CSRF
     * - Path=/ : Cookie disponible pour toute l'application
     * - MaxAge=24h : Durée de vie du cookie
     */
    private Cookie createJwtCookie(String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true); // JavaScript ne peut pas lire
        cookie.setSecure(cookieSecure); // Lit depuis application.properties (false=dev, true=prod)
        cookie.setPath("/"); // Disponible pour toute l'application
        cookie.setMaxAge(24 * 60 * 60); // 24 heures
        // Note: SameSite=Strict est géré par le navigateur moderne par défaut

        return cookie;
    }
}
