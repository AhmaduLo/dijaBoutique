package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.DepenseEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.DepenseService;
import com.example.dijasaliou.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * GET /api/depenses/utilisateur/{utilisateurId}
     * Récupérer toutes les dépenses d'un utilisateur
     */
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<DepenseEntity>> obtenirDepensesParUtilisateur(
            @PathVariable Long utilisateurId) {

        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);
        List<DepenseEntity> depenses = depenseService.obtenirDepensesParUtilisateur(utilisateur);

        return ResponseEntity.ok(depenses);
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

    /**
     * GET /api/depenses/categorie/{categorie}
     * Récupérer les dépenses par catégorie
     *
     * Exemple : GET /api/depenses/categorie/LOYER
     */
    @GetMapping("/categorie/{categorie}")
    public ResponseEntity<List<DepenseEntity>> obtenirDepensesParCategorie(
            @PathVariable DepenseEntity.CategorieDepense categorie) {

        List<DepenseEntity> depenses = depenseService.obtenirDepensesParCategorie(categorie);
        return ResponseEntity.ok(depenses);
    }

    /**
     * GET /api/depenses/total?debut=2025-01-01&fin=2025-12-31
     * Calculer le total des dépenses sur une période
     */
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> calculerTotalDepenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        BigDecimal total = depenseService.calculerTotalDepenses(debut, fin);
        return ResponseEntity.ok(total);
    }

    /**
     * GET /api/depenses/statistiques?debut=2025-01-01&fin=2025-12-31
     * Obtenir les statistiques des dépenses sur une période
     */
    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> obtenirStatistiques(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        List<DepenseEntity> depenses = depenseService.obtenirDepensesParPeriode(debut, fin);
        BigDecimal total = depenseService.calculerTotalDepenses(debut, fin);

        Map<String, Object> stats = new HashMap<>();
        stats.put("dateDebut", debut);
        stats.put("dateFin", fin);
        stats.put("nombreDepenses", depenses.size());
        stats.put("montantTotal", total);
        stats.put("depenses", depenses);

        return ResponseEntity.ok(stats);
    }


}
