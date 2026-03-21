package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.UserDto;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller REST pour les utilisateurs
 *
 * RESTRICTIONS :
 * - Les USER ne peuvent PAS créer, modifier ou supprimer des comptes
 * - Seul l'ADMIN peut gérer les comptes (via AdminController)
 * - GET /utilisateurs et /utilisateurs/{id} réservés aux GERANT et ADMIN
 * - GET /utilisateurs/moi accessible à tous (chaque user consulte son propre profil)
 */
@RestController
@RequestMapping("/utilisateurs")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/utilisateurs
     * Réservé aux GERANT et ADMIN (liste des employés)
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    public ResponseEntity<List<UserDto>> obtenirTous() {
        List<UserEntity> utilisateurs = userService.obtenirTousLesUtilisateurs();
        List<UserDto> utilisateursDto = utilisateurs.stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(utilisateursDto);
    }

    /**
     * GET /api/utilisateurs/{id}
     * Réservé aux GERANT et ADMIN
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    public ResponseEntity<UserDto> obtenirParId(@PathVariable Long id) {
        UserEntity utilisateur = userService.obtenirUtilisateurParId(id);
        return ResponseEntity.ok(UserDto.fromEntity(utilisateur));
    }

    /**
     * GET /api/utilisateurs/email/{email}
     * Réservé aux GERANT et ADMIN
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    public ResponseEntity<UserDto> obtenirParEmail(@PathVariable String email) {
        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(email);
        return ResponseEntity.ok(UserDto.fromEntity(utilisateur));
    }

    /**
     * GET /api/utilisateurs/moi
     * Accessible à tous : chaque utilisateur consulte son propre profil
     */
    @GetMapping("/moi")
    public ResponseEntity<UserDto> obtenirMonProfil(Authentication authentication) {
        String email = authentication.getName();
        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(email);
        return ResponseEntity.ok(UserDto.fromEntity(utilisateur));
    }

    /**
     * POST /api/utilisateurs
     * DESACTIVE - Seul l'ADMIN peut créer des comptes via /api/admin/utilisateurs
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> creer(@Valid @RequestBody UserEntity utilisateur) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "La création de compte est réservée aux administrateurs");
        error.put("endpoint", "Utilisez /api/admin/utilisateurs (ADMIN uniquement)");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * PUT /api/utilisateurs/{id}
     * DESACTIVE - Seul l'ADMIN peut modifier des comptes via /api/admin/utilisateurs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody UserEntity utilisateurModifie) {

        Map<String, String> error = new HashMap<>();
        error.put("message", "La modification de compte est réservée aux administrateurs");
        error.put("endpoint", "Utilisez /api/admin/utilisateurs/{id} (ADMIN uniquement)");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * DELETE /api/utilisateurs/{id}
     * DESACTIVE - Seul l'ADMIN peut supprimer des comptes via /api/admin/utilisateurs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "La suppression de compte est réservée aux administrateurs");
        error.put("endpoint", "Utilisez /api/admin/utilisateurs/{id} (ADMIN uniquement)");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
