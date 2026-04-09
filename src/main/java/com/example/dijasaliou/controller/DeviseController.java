package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.CreateDeviseDto;
import com.example.dijasaliou.dto.DeviseDto;
import com.example.dijasaliou.dto.UpdateDeviseDto;
import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.service.DeviseService;
import com.example.dijasaliou.service.TenantService;
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
 * SÉCURITÉ :
 * - Lecture (GET) : Accessible à tous les utilisateurs authentifiés
 * - Modification (POST/PUT/DELETE) : Réservé aux ADMIN
 *
 * La devise "par défaut" est désormais une PRÉFÉRENCE PAR BOUTIQUE
 * stockée dans tenants.devise_preferee — pas un flag global partagé.
 */
@RestController
@RequestMapping("/devises")
public class DeviseController {

    private final DeviseService deviseService;
    private final TenantService tenantService;
    private final TenantRepository tenantRepository;

    public DeviseController(DeviseService deviseService, TenantService tenantService, TenantRepository tenantRepository) {
        this.deviseService = deviseService;
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
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
     * Récupérer la devise préférée de CETTE boutique (pas le flag global)
     */
    @GetMapping("/default")
    public ResponseEntity<DeviseDto> obtenirDeviseParDefaut() {
        TenantEntity tenant = tenantService.getCurrentTenant();
        String codePreferee = (tenant.getDevisePreferee() != null) ? tenant.getDevisePreferee() : "XOF";
        DeviseEntity devise = deviseService.obtenirDeviseParCode(codePreferee);
        return ResponseEntity.ok(DeviseDto.fromEntity(devise));
    }

    /**
     * POST /api/devises
     * Créer une nouvelle devise
     * ADMIN uniquement
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DeviseDto> creerDevise(@Valid @RequestBody CreateDeviseDto dto) {
        DeviseEntity devise = deviseService.creerDevise(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviseDto.fromEntity(devise));
    }

    /**
     * PUT /api/devises/{id}
     * Modifier une devise
     * ADMIN uniquement
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DeviseDto> modifierDevise(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeviseDto dto) {
        DeviseEntity devise = deviseService.modifierDevise(id, dto);
        return ResponseEntity.ok(DeviseDto.fromEntity(devise));
    }

    /**
     * DELETE /api/devises/{id}
     * Supprimer une devise
     * ADMIN uniquement
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> supprimerDevise(@PathVariable Long id) {
        deviseService.supprimerDevise(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/devises/{id}/set-default
     * Définir la devise préférée de CETTE boutique uniquement — sans affecter les autres.
     * ADMIN uniquement
     */
    @PutMapping("/{id}/set-default")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DeviseDto> definirDeviseParDefaut(@PathVariable Long id) {
        DeviseEntity devise = deviseService.obtenirDeviseParId(id);
        TenantEntity tenant = tenantRepository.findById(tenantService.getCurrentTenant().getId())
                .orElseThrow(() -> new RuntimeException("Tenant introuvable"));
        tenant.setDevisePreferee(devise.getCode());
        tenantRepository.save(tenant);
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
        if (request.get("montant") == null || request.get("deviseSource") == null || request.get("deviseCible") == null) {
            throw new IllegalArgumentException("montant, deviseSource et deviseCible sont obligatoires");
        }
        Double montant;
        try {
            montant = Double.parseDouble(request.get("montant").toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Le montant doit être un nombre valide");
        }
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
