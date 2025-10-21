package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour les utilisateurs
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
     */
    @GetMapping
    public ResponseEntity<List<UserEntity>> obtenirTous() {
        List<UserEntity> utilisateurs = userService.obtenirTousLesUtilisateurs();
        return ResponseEntity.ok(utilisateurs);
    }

    /**
     * GET /api/utilisateurs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> obtenirParId(@PathVariable Long id) {
        UserEntity utilisateur = userService.obtenirUtilisateurParId(id);
        return ResponseEntity.ok(utilisateur);
    }

    /**
     * GET /api/utilisateurs/email/{email}
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserEntity> obtenirParEmail(@PathVariable String email) {
        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(email);
        return ResponseEntity.ok(utilisateur);
    }

    /**
     * POST /api/utilisateurs
     */
    @PostMapping
    public ResponseEntity<UserEntity> creer(@RequestBody UserEntity utilisateur) {
        UserEntity utilisateurCree = userService.creerUtilisateur(utilisateur);
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurCree);
    }

    /**
     * PUT /api/utilisateurs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserEntity> modifier(
            @PathVariable Long id,
            @RequestBody UserEntity utilisateurModifie) {

        UserEntity utilisateur = userService.modifierUtilisateur(id, utilisateurModifie);
        return ResponseEntity.ok(utilisateur);
    }

    /**
     * DELETE /api/utilisateurs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        userService.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }
}
