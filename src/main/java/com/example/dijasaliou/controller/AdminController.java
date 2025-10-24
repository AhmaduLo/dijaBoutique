package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.dto.UserDto;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST pour l'administration des utilisateurs
 *
 * ACCÈS : Réservé aux ADMIN uniquement
 *
 * Permet de :
 * - Créer des comptes employés/clients
 * - Voir tous les comptes
 * - Modifier les comptes
 * - Supprimer les comptes
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * GET /api/admin/utilisateurs
     * Obtenir tous les utilisateurs créés par l'admin
     *
     * Retourne la liste complète des comptes avec leurs infos (sauf mot de passe)
     */
    @GetMapping("/utilisateurs")
    public ResponseEntity<List<UserDto>> obtenirTousLesUtilisateurs(Authentication authentication) {
        String emailAdmin = authentication.getName();
        List<UserDto> utilisateurs = adminService.obtenirTousLesUtilisateurs(emailAdmin);
        return ResponseEntity.ok(utilisateurs);
    }

    /**
     * GET /api/admin/utilisateurs/{id}
     * Obtenir un utilisateur spécifique
     */
    @GetMapping("/utilisateurs/{id}")
    public ResponseEntity<UserDto> obtenirUtilisateurParId(
            @PathVariable Long id,
            Authentication authentication) {
        String emailAdmin = authentication.getName();
        UserDto utilisateur = adminService.obtenirUtilisateurParId(id, emailAdmin);
        return ResponseEntity.ok(utilisateur);
    }

    /**
     * POST /api/admin/utilisateurs
     * Créer un nouveau compte employé/client
     *
     * IMPORTANT : Seul l'ADMIN peut créer des comptes
     *
     * Body JSON :
     * {
     *   "nom": "Diop",
     *   "prenom": "Mamadou",
     *   "email": "mamadou@dijaboutique.com",
     *   "motDePasse": "password123",
     *   "role": "USER"
     * }
     */
    @PostMapping("/utilisateurs")
    public ResponseEntity<UserDto> creerUtilisateur(
            @RequestBody RegisterRequest request,
            Authentication authentication) {

        String emailAdmin = authentication.getName();
        UserDto utilisateur = adminService.creerUtilisateur(request, emailAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateur);
    }

    /**
     * PUT /api/admin/utilisateurs/{id}
     * Modifier les informations d'un utilisateur
     *
     * L'ADMIN peut modifier n'importe quel compte (y compris le sien)
     */
    @PutMapping("/utilisateurs/{id}")
    public ResponseEntity<UserDto> modifierUtilisateur(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {

        String emailAdmin = authentication.getName();
        UserDto utilisateur = adminService.modifierUtilisateur(id, updates, emailAdmin);
        return ResponseEntity.ok(utilisateur);
    }

    /**
     * DELETE /api/admin/utilisateurs/{id}
     * Supprimer un utilisateur
     *
     * L'ADMIN peut supprimer n'importe quel compte (sauf le sien)
     */
    @DeleteMapping("/utilisateurs/{id}")
    public ResponseEntity<Map<String, String>> supprimerUtilisateur(
            @PathVariable Long id,
            Authentication authentication) {

        String emailAdmin = authentication.getName();
        adminService.supprimerUtilisateur(id, emailAdmin);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/statistiques
     * Obtenir des statistiques sur les utilisateurs
     */
    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> obtenirStatistiques(Authentication authentication) {
        String emailAdmin = authentication.getName();
        Map<String, Object> stats = adminService.obtenirStatistiques(emailAdmin);
        return ResponseEntity.ok(stats);
    }

    /**
     * PUT /api/admin/utilisateurs/{id}/role
     * Modifier le rôle d'un utilisateur (USER <-> ADMIN)
     *
     * ATTENTION : Permet de promouvoir un USER en ADMIN
     */
    @PutMapping("/utilisateurs/{id}/role")
    public ResponseEntity<UserDto> modifierRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> roleData,
            Authentication authentication) {

        String emailAdmin = authentication.getName();
        String nouveauRole = roleData.get("role");

        UserDto utilisateur = adminService.modifierRole(id, nouveauRole, emailAdmin);
        return ResponseEntity.ok(utilisateur);
    }
}
