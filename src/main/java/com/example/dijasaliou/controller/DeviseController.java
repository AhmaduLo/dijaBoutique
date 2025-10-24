package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.CreateDeviseDto;
import com.example.dijasaliou.dto.DeviseDto;
import com.example.dijasaliou.dto.UpdateDeviseDto;
import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.service.DeviseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller REST pour la gestion des devises
 *
 * SÉCURITÉ : Tous les endpoints sont réservés aux ADMIN
 */
@RestController
@RequestMapping("/devises")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAuthority('ADMIN')")
public class DeviseController {

    private final DeviseService deviseService;

    public DeviseController(DeviseService deviseService) {
        this.deviseService = deviseService;
    }

    /**
     * GET /api/devises
     * Lister toutes les devises
     */
    @GetMapping
    public ResponseEntity<List<DeviseDto>> obtenirToutesLesDevises() {
        List<DeviseEntity> devises = deviseService.obtenirToutesLesDevises();
        List<DeviseDto> devisesDto = devises.stream()
                .map(DeviseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(devisesDto);
    }

    /**
     * GET /api/devises/{id}
     * Récupérer une devise par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeviseDto> obtenirDeviseParId(@PathVariable Long id) {
        DeviseEntity devise = deviseService.obtenirDeviseParId(id);
        return ResponseEntity.ok(DeviseDto.fromEntity(devise));
    }

    /**
     * GET /api/devises/default
     * Récupérer la devise par défaut
     */
    @GetMapping("/default")
    public ResponseEntity<DeviseDto> obtenirDeviseParDefaut() {
        DeviseEntity devise = deviseService.obtenirDeviseParDefaut();
        return ResponseEntity.ok(DeviseDto.fromEntity(devise));
    }

    /**
     * POST /api/devises
     * Créer une nouvelle devise
     */
    @PostMapping
    public ResponseEntity<DeviseDto> creerDevise(@Valid @RequestBody CreateDeviseDto dto) {
        DeviseEntity devise = deviseService.creerDevise(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviseDto.fromEntity(devise));
    }

    /**
     * PUT /api/devises/{id}
     * Modifier une devise
     */
    @PutMapping("/{id}")
    public ResponseEntity<DeviseDto> modifierDevise(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeviseDto dto) {
        DeviseEntity devise = deviseService.modifierDevise(id, dto);
        return ResponseEntity.ok(DeviseDto.fromEntity(devise));
    }

    /**
     * DELETE /api/devises/{id}
     * Supprimer une devise
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerDevise(@PathVariable Long id) {
        deviseService.supprimerDevise(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/devises/{id}/set-default
     * Définir une devise comme devise par défaut
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<DeviseDto> definirDeviseParDefaut(@PathVariable Long id) {
        DeviseEntity devise = deviseService.definirDeviseParDefaut(id);
        return ResponseEntity.ok(DeviseDto.fromEntity(devise));
    }

    /**
     * POST /api/devises/convertir
     * Convertir un montant d'une devise à une autre
     *
     * Body JSON :
     * {
     *   "montant": 100.0,
     *   "deviseSource": "USD",
     *   "deviseCible": "XOF"
     * }
     */
    @PostMapping("/convertir")
    public ResponseEntity<Map<String, Object>> convertir(@RequestBody Map<String, Object> request) {
        Double montant = Double.parseDouble(request.get("montant").toString());
        String deviseSource = request.get("deviseSource").toString();
        String deviseCible = request.get("deviseCible").toString();

        Double montantConverti = deviseService.convertir(montant, deviseSource, deviseCible);

        Map<String, Object> response = new HashMap<>();
        response.put("montant", montant);
        response.put("deviseSource", deviseSource);
        response.put("deviseCible", deviseCible);
        response.put("montantConverti", montantConverti);

        return ResponseEntity.ok(response);
    }
}
