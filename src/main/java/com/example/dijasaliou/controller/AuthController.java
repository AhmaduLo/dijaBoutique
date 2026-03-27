package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.AuthResponse;
import lombok.extern.slf4j.Slf4j;
import com.example.dijasaliou.dto.ForgotPasswordRequest;
import com.example.dijasaliou.dto.LoginRequest;
import com.example.dijasaliou.dto.PasswordResetResponse;
import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.dto.ResetPasswordRequest;
import com.example.dijasaliou.service.AuthService;
import com.example.dijasaliou.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
@Slf4j
@RestController
@RequestMapping("/auth")

public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Strict}")
    private String cookieSameSite;

    @Value("${app.frontend.url:https://www.heasystock.com}")
    private String frontendUrl;

    public AuthController(AuthService authService, RateLimitService rateLimitService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
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
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        // SÉCURITÉ : Rate limiting pour éviter le spam de création de comptes
        String ipAddress = getClientIpAddress(httpRequest);
        if (!rateLimitService.allowRegister(ipAddress)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(java.util.Map.of("error", "Trop de tentatives de création de compte. Veuillez réessayer dans 1 heure."));
        }

        AuthResponse authResponse = authService.register(request);

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
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        // SÉCURITÉ : Rate limiting pour éviter les attaques par force brute
        String ipAddress = getClientIpAddress(httpRequest);
        if (!rateLimitService.allowLogin(ipAddress)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(java.util.Map.of("error", "Trop de tentatives de connexion. Veuillez réessayer dans 1 minute."));
        }

        AuthResponse authResponse = authService.login(request);

        // SÉCURITÉ : Réinitialiser le compteur après un login réussi
        rateLimitService.resetLoginAttempts(ipAddress);

        return ResponseEntity.ok(authResponse);
    }

    /**
     * POST /api/auth/logout
     * Déconnexion de l'utilisateur en supprimant le cookie JWT
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        // Supprimer le cookie JWT (maxAge=0 + SameSite=Strict)
        removeJwtCookie(response);

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
        if (!rateLimitService.allowPasswordReset(request.getEmail())) {
            PasswordResetResponse response = PasswordResetResponse.builder()
                    .message("Trop de tentatives. Veuillez patienter avant de réessayer.")
                    .build();
            return ResponseEntity.status(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS).body(response);
        }

        authService.forgotPassword(request);

        // Message générique pour ne pas révéler si l'email existe
        PasswordResetResponse response = PasswordResetResponse.builder()
                .message("Si cet email existe, un lien de réinitialisation a été envoyé")
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/verify-email?token=XXX
     * Idempotent : gère le scan Brevo avant l'utilisateur
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        log.info("=== verify-email appelé avec token: {}", token);
        String redirectUrl;
        try {
            authService.verifyEmail(token);
            redirectUrl = frontendUrl + "/email-confirmed";
        } catch (IllegalStateException e) {
            // Token déjà utilisé mais email bien vérifié (scan Brevo)
            redirectUrl = frontendUrl + "/email-confirmed";
        } catch (IllegalArgumentException e) {
            redirectUrl = frontendUrl + "/email-confirmed?error=invalid";
        } catch (Exception e) {
            redirectUrl = frontendUrl + "/email-confirmed?error=invalid";
        }
        log.info("=== Redirection vers: {}", redirectUrl);
        return ResponseEntity.status(302)
                .location(java.net.URI.create(redirectUrl))
                .build();
    }

    /**
     * GET /api/auth/me
     * Retourne les infos de l'utilisateur connecté avec emailVerifie à jour
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(authService.getMe(email));
    }

    /**
     * POST /api/auth/resend-verification-email
     * Renvoie l'email de vérification — utilise le JWT de l'utilisateur connecté
     */
    @PostMapping("/resend-verification-email")
    public ResponseEntity<?> resendVerificationEmail(Authentication authentication) {
        authService.resendVerificationEmail(authentication.getName());
        return ResponseEntity.ok(java.util.Map.of("message", "Email de vérification renvoyé."));
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
        removeJwtCookie(response);

        PasswordResetResponse deleteResponse = PasswordResetResponse.builder()
                .message("Votre compte et toutes les données associées ont été supprimés")
                .build();
        return ResponseEntity.ok(deleteResponse);
    }

    /**
     * Ajoute un cookie JWT HttpOnly avec SameSite=Strict dans la réponse
     *
     * Flags de sécurité :
     * - HttpOnly : JavaScript ne peut pas lire le cookie (protection XSS)
     * - Secure : Cookie envoyé uniquement en HTTPS (désactivé en dev)
    /**
     * Supprime le cookie JWT (expiration immédiate)
     */
    private void removeJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0L)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Récupère l'adresse IP du client directement depuis la socket TCP.
     *
     * SÉCURITÉ : On n'utilise PAS X-Forwarded-For car ce header peut être
     * forgé par n'importe quel client pour contourner le rate limiting.
     * Si l'app est derrière un reverse proxy (nginx, Cloudflare), configurer
     * server.forward-headers-strategy=NATIVE dans application-prod.properties
     * pour que Spring Boot gère les headers proxy de façon sécurisée.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
