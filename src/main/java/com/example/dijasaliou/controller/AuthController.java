package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.AuthResponse;
import com.example.dijasaliou.dto.LoginRequest;
import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.service.AuthService;
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
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "type": "Bearer",
     *   "id": 1,
     *   "email": "dija@boutique.com",
     *   "nom": "Saliou",
     *   "prenom": "Dija",
     *   "role": "ADMIN"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
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
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "type": "Bearer",
     *   "id": 1,
     *   "email": "dija@boutique.com",
     *   "nom": "Saliou",
     *   "prenom": "Dija",
     *   "role": "ADMIN"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
