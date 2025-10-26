package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.DepenseDto;
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
import java.util.stream.Collectors;

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
    public ResponseEntity<List<DepenseDto>> obtenirTous() {
        List<DepenseEntity> depenses = depenseService.obtenirToutesLesDepenses();
        List<DepenseDto> depensesDto = depenses.stream()
                .map(DepenseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(depensesDto);
    }

    /**
     * GET /api/depenses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DepenseDto> obtenirParId(@PathVariable Long id) {
        DepenseEntity depense = depenseService.obtenirDepenseParId(id);
        return ResponseEntity.ok(DepenseDto.fromEntity(depense));
    }

    /**
     * GET /api/depenses/utilisateur/{utilisateurId}
     * Récupérer toutes les dépenses d'un utilisateur
     */
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<DepenseDto>> obtenirDepensesParUtilisateur(
            @PathVariable Long utilisateurId) {

        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);
        List<DepenseEntity> depenses = depenseService.obtenirDepensesParUtilisateur(utilisateur);

        List<DepenseDto> depensesDto = depenses.stream()
                .map(DepenseDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(depensesDto);
    }

    /**
     * POST /api/depenses
     */
    @PostMapping
    public ResponseEntity<DepenseDto> creer(
            @RequestBody DepenseEntity depense,
            @RequestParam Long utilisateurId) {

        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);
        DepenseEntity depenseCree = depenseService.creerDepense(depense, utilisateur);

        return ResponseEntity.status(HttpStatus.CREATED).body(DepenseDto.fromEntity(depenseCree));
    }

    /**
     * PUT /api/depenses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<DepenseDto> modifier(
            @PathVariable Long id,
            @RequestBody DepenseEntity depenseModifiee,
            @RequestParam Long utilisateurId) {

        // Récupérer l'utilisateur qui modifie
        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);

        // Mettre à jour l'utilisateur de la dépense
        depenseModifiee.setUtilisateur(utilisateur);

        // Sauvegarder avec le nouvel utilisateur
        DepenseEntity depense = depenseService.modifierDepense(id, depenseModifiee);
        return ResponseEntity.ok(DepenseDto.fromEntity(depense));
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
    public ResponseEntity<List<DepenseDto>> obtenirDepensesParCategorie(
            @PathVariable DepenseEntity.CategorieDepense categorie) {

        List<DepenseEntity> depenses = depenseService.obtenirDepensesParCategorie(categorie);
        List<DepenseDto> depensesDto = depenses.stream()
                .map(DepenseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(depensesDto);
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

        // Convertir en DTOs
        List<DepenseDto> depensesDto = depenses.stream()
                .map(DepenseDto::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("dateDebut", debut);
        stats.put("dateFin", fin);
        stats.put("nombreDepenses", depensesDto.size());
        stats.put("montantTotal", total);
        stats.put("depenses", depensesDto);

        return ResponseEntity.ok(stats);
    }


}
