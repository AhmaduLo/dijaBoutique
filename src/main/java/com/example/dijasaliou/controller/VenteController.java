package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.VenteDto;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.service.UserService;
import com.example.dijasaliou.service.VenteService;
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
 * Controller REST pour les ventes
 */
@RestController
@RequestMapping("/ventes")
@CrossOrigin(origins = "http://localhost:4200")
public class VenteController {

    private final VenteService venteService;
    private final UserService userService;

    public VenteController(VenteService venteService, UserService userService) {
        this.venteService = venteService;
        this.userService = userService;
    }

    /**
     * GET /api/ventes
     */
    @GetMapping
    public ResponseEntity<List<VenteDto>> obtenirTous() {
        List<VenteEntity> ventes = venteService.obtenirToutesLesVentes();
        List<VenteDto> ventesDto = ventes.stream()
                .map(VenteDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ventesDto);
    }

    /**
     * GET /api/ventes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<VenteDto> obtenirParId(@PathVariable Long id) {
        VenteEntity vente = venteService.obtenirVenteParId(id);
        return ResponseEntity.ok(VenteDto.fromEntity(vente));
    }

    /**
     * POST /api/ventes
     */
    @PostMapping
    public ResponseEntity<VenteDto> creer(
            @RequestBody VenteEntity vente,
            @RequestParam Long utilisateurId) {

        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);
        VenteEntity venteCree = venteService.creerVente(vente, utilisateur);

        return ResponseEntity.status(HttpStatus.CREATED).body(VenteDto.fromEntity(venteCree));
    }

    /**
     * PUT /api/ventes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<VenteDto> modifier(
            @PathVariable Long id,
            @RequestBody VenteEntity venteModifiee,
            @RequestParam Long utilisateurId) {

        // Récupérer l'utilisateur qui modifie
        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);

        // Mettre à jour l'utilisateur de la vente
        venteModifiee.setUtilisateur(utilisateur);

        // Sauvegarder avec le nouvel utilisateur
        VenteEntity vente = venteService.modifierVente(id, venteModifiee);
        return ResponseEntity.ok(VenteDto.fromEntity(vente));
    }

    /**
     * DELETE /api/ventes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        venteService.supprimerVente(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/ventes/utilisateur/{utilisateurId}
     * Récupérer toutes les ventes d'un utilisateur spécifique
     *
     * Exemple : GET /api/ventes/utilisateur/1
     */
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<VenteDto>> obtenirVentesParUtilisateur(
            @PathVariable Long utilisateurId) {

        // Récupérer l'utilisateur
        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);

        // Récupérer ses ventes
        List<VenteEntity> ventes = venteService.obtenirVentesParUtilisateur(utilisateur);

        // Convertir en DTOs
        List<VenteDto> ventesDto = ventes.stream()
                .map(VenteDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ventesDto);
    }

    /**
     * GET /api/ventes/chiffre-affaires?debut=2025-01-01&fin=2025-12-31
     * Calculer le chiffre d'affaires sur une période
     *
     * Exemple : GET /api/ventes/chiffre-affaires?debut=2025-10-01&fin=2025-10-31
     */
    @GetMapping("/chiffre-affaires")
    public ResponseEntity<BigDecimal> calculerChiffreAffaires(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        BigDecimal ca = venteService.calculerChiffreAffaires(debut, fin);
        return ResponseEntity.ok(ca);
    }

    /**
     * GET /api/ventes/statistiques?debut=2025-01-01&fin=2025-12-31
     * Obtenir les statistiques de ventes sur une période
     * (nombre de ventes, CA total, liste des ventes)
     *
     * Exemple : GET /api/ventes/statistiques?debut=2025-10-01&fin=2025-10-31
     */
    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> obtenirStatistiques(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        List<VenteEntity> ventes = venteService.obtenirVentesParPeriode(debut, fin);
        BigDecimal ca = venteService.calculerChiffreAffaires(debut, fin);

        // Convertir en DTOs
        List<VenteDto> ventesDto = ventes.stream()
                .map(VenteDto::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("dateDebut", debut);
        stats.put("dateFin", fin);
        stats.put("nombreVentes", ventesDto.size());
        stats.put("chiffreAffaires", ca);
        stats.put("ventes", ventesDto);

        return ResponseEntity.ok(stats);
    }
}
