package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.DepenseEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.DepenseService;
import com.example.dijasaliou.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour les dépenses
 */
@RestController
@RequestMapping("/depenses")
@CrossOrigin(origins = "http://localhost:4200")
public class DepenseController {

    private final DepenseService depenseService;
    private final UserService userService;

    public DepenseController(DepenseService depenseService, UserService userService) {
        this.depenseService = depenseService;
        this.userService = userService;
    }

    /**
     * GET /api/depenses
     */
    @GetMapping
    public ResponseEntity<List<DepenseEntity>> obtenirTous() {
        List<DepenseEntity> depenses = depenseService.obtenirToutesLesDepenses();
        return ResponseEntity.ok(depenses);
    }

    /**
     * GET /api/depenses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DepenseEntity> obtenirParId(@PathVariable Long id) {
        DepenseEntity depense = depenseService.obtenirDepenseParId(id);
        return ResponseEntity.ok(depense);
    }

    /**
     * POST /api/depenses
     */
    @PostMapping
    public ResponseEntity<DepenseEntity> creer(
            @RequestBody DepenseEntity depense,
            @RequestParam Long utilisateurId) {

        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);
        DepenseEntity depenseCree = depenseService.creerDepense(depense, utilisateur);

        return ResponseEntity.status(HttpStatus.CREATED).body(depenseCree);
    }

    /**
     * PUT /api/depenses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<DepenseEntity> modifier(
            @PathVariable Long id,
            @RequestBody DepenseEntity depenseModifiee) {

        DepenseEntity depense = depenseService.modifierDepense(id, depenseModifiee);
        return ResponseEntity.ok(depense);
    }

    /**
     * DELETE /api/depenses/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        depenseService.supprimerDepense(id);
        return ResponseEntity.noContent().build();
    }
}
