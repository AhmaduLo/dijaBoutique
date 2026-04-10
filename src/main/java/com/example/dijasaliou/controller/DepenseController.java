package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.DepenseDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.entity.DepenseEntity;
import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.DepenseService;
import com.example.dijasaliou.service.DeviseService;
import com.example.dijasaliou.service.TenantService;
import com.example.dijasaliou.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

public class DepenseController {

    private final DepenseService depenseService;
    private final UserService userService;
    private final TenantService tenantService;
    private final DeviseService deviseService;

    public DepenseController(DepenseService depenseService, UserService userService,
                             TenantService tenantService, DeviseService deviseService) {
        this.depenseService = depenseService;
        this.userService = userService;
        this.tenantService = tenantService;
        this.deviseService = deviseService;
    }

    /**
     * GET /api/depenses?page=0&size=10&search=xxx&categorie=LOYER
     */
    @GetMapping
    public ResponseEntity<PagedResponse<DepenseDto>> obtenirTous(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categorie) {
        if (size > 100) size = 100;
        return ResponseEntity.ok(depenseService.obtenirDepensesPaginees(page, size, search, categorie));
    }

    /**
     * GET /api/depenses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DepenseDto> obtenirParId(@PathVariable String id) {
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
     * SÉCURITÉ : L'utilisateur est extrait du token JWT (Authentication),
     * pas d'un paramètre fourni par le client (protection IDOR).
     */
    @PostMapping
    public ResponseEntity<DepenseDto> creer(
            @Valid @RequestBody DepenseEntity depense,
            Authentication authentication) {

        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(authentication.getName());
        DepenseEntity depenseCree = depenseService.creerDepense(depense, utilisateur);

        return ResponseEntity.status(HttpStatus.CREATED).body(DepenseDto.fromEntity(depenseCree));
    }

    /**
     * PUT /api/depenses/{id}
     * SÉCURITÉ : L'utilisateur est extrait du token JWT (Authentication),
     * pas d'un paramètre fourni par le client (protection IDOR).
     */
    @PutMapping("/{id}")
    public ResponseEntity<DepenseDto> modifier(
            @PathVariable String id,
            @Valid @RequestBody DepenseEntity depenseModifiee,
            Authentication authentication) {

        // Récupérer l'utilisateur authentifié (depuis le JWT, pas d'un paramètre client)
        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(authentication.getName());

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
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) String devise) {

        List<DepenseEntity> depenses = depenseService.obtenirDepensesParPeriode(debut, fin);

        TenantEntity tenant = tenantService.getCurrentTenant();
        String codeDevise = (devise != null && !devise.isBlank())
                ? devise.toUpperCase().trim()
                : (tenant.getDevisePreferee() != null ? tenant.getDevisePreferee() : "XOF");
        double tauxTemp = 1.0;
        try {
            DeviseEntity deviseRapport = deviseService.obtenirDeviseParCode(codeDevise);
            if (deviseRapport != null && deviseRapport.getTauxChange() != null) {
                tauxTemp = deviseRapport.getTauxChange().doubleValue();
            }
        } catch (RuntimeException e) {
            // fallback to default 1.0
        }
        final double tauxFinal = tauxTemp;

        BigDecimal total = depenses.stream()
                .filter(d -> d.getMontant() != null && d.getTauxChangeApplique() != null)
                .map(d -> d.getMontant()
                        .multiply(BigDecimal.valueOf(d.getTauxChangeApplique()))
                        .divide(BigDecimal.valueOf(tauxFinal), 2, java.math.RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        return ResponseEntity.ok(total);
    }

    /**
     * GET /api/depenses/statistiques?debut=2025-01-01&fin=2025-12-31
     * Obtenir les statistiques des dépenses sur une période
     */
    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> obtenirStatistiques(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) String devise) {

        List<DepenseEntity> depenses = depenseService.obtenirDepensesParPeriode(debut, fin);

        // Convertir en DTOs
        List<DepenseDto> depensesDto = depenses.stream()
                .map(DepenseDto::fromEntity)
                .collect(Collectors.toList());

        // Devise de rapport : paramètre explicite ou devise préférée du tenant
        TenantEntity tenant = tenantService.getCurrentTenant();
        String codeDevise = (devise != null && !devise.isBlank())
                ? devise.toUpperCase().trim()
                : (tenant.getDevisePreferee() != null ? tenant.getDevisePreferee() : "XOF");
        double tauxTemp2 = 1.0;
        try {
            DeviseEntity deviseRapport = deviseService.obtenirDeviseParCode(codeDevise);
            if (deviseRapport != null && deviseRapport.getTauxChange() != null) {
                tauxTemp2 = deviseRapport.getTauxChange().doubleValue();
            }
        } catch (RuntimeException e) {
            // fallback to default 1.0
        }
        final double tauxFinal = tauxTemp2;

        // Calculer le total dans la devise du tenant : montant × tauxChangeApplique → XOF → / tauxTenant
        BigDecimal total = depenses.stream()
                .filter(d -> d.getMontant() != null && d.getTauxChangeApplique() != null)
                .map(d -> d.getMontant()
                        .multiply(BigDecimal.valueOf(d.getTauxChangeApplique()))
                        .divide(BigDecimal.valueOf(tauxFinal), 2, java.math.RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        // Répartition par catégorie (attendue par le frontend pour les rapports)
        Map<String, BigDecimal> repartitionParCategorie = new HashMap<>();
        for (DepenseEntity d : depenses) {
            if (d.getCategorie() != null && d.getMontant() != null && d.getTauxChangeApplique() != null) {
                String cat = d.getCategorie().name();
                BigDecimal montantConverti = d.getMontant()
                        .multiply(BigDecimal.valueOf(d.getTauxChangeApplique()))
                        .divide(BigDecimal.valueOf(tauxFinal), 2, java.math.RoundingMode.HALF_UP);
                repartitionParCategorie.merge(cat, montantConverti, BigDecimal::add);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("dateDebut", debut);
        stats.put("dateFin", fin);
        stats.put("nombreDepenses", depensesDto.size());
        stats.put("montantTotal", total);
        stats.put("deviseCode", codeDevise);
        stats.put("depenses", depensesDto);
        stats.put("repartitionParCategorie", repartitionParCategorie);

        return ResponseEntity.ok(stats);
    }


}
