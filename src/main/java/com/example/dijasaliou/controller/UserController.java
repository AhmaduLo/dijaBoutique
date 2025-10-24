package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.UserDto;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller REST pour les utilisateurs
 *
 * ⚠️ RESTRICTIONS ⚠️
 * - Les USER ne peuvent PAS créer, modifier ou supprimer des comptes
 * - Seul l'ADMIN peut gérer les comptes (via AdminController)
 * - Les routes de consultation sont accessibles à tous les utilisateurs authentifiés
 */
@RestController
@RequestMapping("/utilisateurs")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/utilisateurs
     * Accessible à tous les utilisateurs authentifiés
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> obtenirTous() {
        List<UserEntity> utilisateurs = userService.obtenirTousLesUtilisateurs();
        List<UserDto> utilisateursDto = utilisateurs.stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(utilisateursDto);
    }

    /**
     * GET /api/utilisateurs/{id}
     * Accessible à tous les utilisateurs authentifiés
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> obtenirParId(@PathVariable Long id) {
        UserEntity utilisateur = userService.obtenirUtilisateurParId(id);
        return ResponseEntity.ok(UserDto.fromEntity(utilisateur));
    }

    /**
     * GET /api/utilisateurs/email/{email}
     * Accessible à tous les utilisateurs authentifiés
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> obtenirParEmail(@PathVariable String email) {
        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(email);
        return ResponseEntity.ok(UserDto.fromEntity(utilisateur));
    }

    /**
     * GET /api/utilisateurs/moi
     * Obtenir les informations du compte connecté
     */
    @GetMapping("/moi")
    public ResponseEntity<UserDto> obtenirMonProfil(Authentication authentication) {
        String email = authentication.getName();
        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(email);
        return ResponseEntity.ok(UserDto.fromEntity(utilisateur));
    }

    /**
     * POST /api/utilisateurs
     * ❌ DÉSACTIVÉ - Seul l'ADMIN peut créer des comptes via /api/admin/utilisateurs
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> creer(@RequestBody UserEntity utilisateur) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "La création de compte est réservée aux administrateurs");
        error.put("endpoint", "Utilisez /api/admin/utilisateurs (ADMIN uniquement)");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * PUT /api/utilisateurs/{id}
     * ❌ DÉSACTIVÉ - Seul l'ADMIN peut modifier des comptes via /api/admin/utilisateurs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> modifier(
            @PathVariable Long id,
            @RequestBody UserEntity utilisateurModifie) {

        Map<String, String> error = new HashMap<>();
        error.put("message", "La modification de compte est réservée aux administrateurs");
        error.put("endpoint", "Utilisez /api/admin/utilisateurs/{id} (ADMIN uniquement)");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * DELETE /api/utilisateurs/{id}
     * ❌ DÉSACTIVÉ - Seul l'ADMIN peut supprimer des comptes via /api/admin/utilisateurs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "La suppression de compte est réservée aux administrateurs");
        error.put("endpoint", "Utilisez /api/admin/utilisateurs/{id} (ADMIN uniquement)");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
