package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.UpdateTenantRequest;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.service.TenantService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des informations de l'entreprise (tenant)
 *
 * Endpoints disponibles (ADMIN uniquement) :
 * - GET /api/admin/entreprise - Récupérer les informations de l'entreprise
 * - PUT /api/admin/entreprise - Modifier les informations de l'entreprise
 */
@RestController
@RequestMapping("/admin/entreprise")
@Slf4j
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Récupère les informations de l'entreprise actuelle
     *
     * GET /api/admin/entreprise
     *
     * @param authentication Informations de l'utilisateur authentifié
     * @return Informations du tenant (entreprise)
     */
    @GetMapping
    public ResponseEntity<TenantResponse> getEntreprise(Authentication authentication) {
        String emailAdmin = authentication.getName();
        log.info("Admin {} récupère les informations de son entreprise", emailAdmin);

        TenantEntity tenant = tenantService.getCurrentTenant();

        TenantResponse response = new TenantResponse(
                tenant.getTenantUuid(),
                tenant.getNomEntreprise(),
                tenant.getNumeroTelephone(),
                tenant.getAdresse()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Modifie les informations de l'entreprise
     *
     * PUT /api/admin/entreprise
     *
     * Body :
     * {
     *   "nomEntreprise": "Nouveau Nom",
     *   "numeroTelephone": "+221771234567"
     * }
     *
     * @param request Nouvelles informations de l'entreprise
     * @param authentication Informations de l'utilisateur authentifié
     * @return Informations mises à jour
     */
    @PutMapping
    public ResponseEntity<TenantResponse> updateEntreprise(
            @Valid @RequestBody UpdateTenantRequest request,
            Authentication authentication
    ) {
        String emailAdmin = authentication.getName();
        log.info("Admin {} modifie les informations de son entreprise", emailAdmin);

        TenantEntity tenantMisAJour = tenantService.updateTenant(request);

        TenantResponse response = new TenantResponse(
                tenantMisAJour.getTenantUuid(),
                tenantMisAJour.getNomEntreprise(),
                tenantMisAJour.getNumeroTelephone(),
                tenantMisAJour.getAdresse()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * DTO pour les réponses contenant les informations du tenant
     */
    public record TenantResponse(
            String tenantUuid,
            String nomEntreprise,
            String numeroTelephone,
            String adresse
    ) {}
}
