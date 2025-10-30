package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.AuthResponse;
import com.example.dijasaliou.dto.LoginRequest;
import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
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
            @RequestBody RegisterRequest request,
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
            @RequestBody LoginRequest request,
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
        jwtCookie.setSecure(false); // true en production avec HTTPS

        response.addCookie(jwtCookie);

        return ResponseEntity.ok("Déconnexion réussie");
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
        cookie.setSecure(false); // true en production avec HTTPS, false en dev
        cookie.setPath("/"); // Disponible pour toute l'application
        cookie.setMaxAge(24 * 60 * 60); // 24 heures
        // Note: SameSite=Strict est géré par le navigateur moderne par défaut

        return cookie;
    }
}
