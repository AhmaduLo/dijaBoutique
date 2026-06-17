package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.UpdateTenantRequest;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.TenantService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des informations de l'entreprise (tenant)
 *
 * Endpoints disponibles :
 * - GET /api/tenant/info - Récupérer les informations de l'entreprise (TOUS les utilisateurs authentifiés)
 * - GET /api/admin/entreprise - Récupérer les informations de l'entreprise (ADMIN uniquement)
 * - PUT /api/admin/entreprise - Modifier les informations de l'entreprise (ADMIN uniquement)
 */
@RestController
@Slf4j
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Récupère les informations de l'entreprise actuelle (accessible à TOUS les utilisateurs authentifiés)
     *
     * GET /api/tenant/info
     *
     * Cet endpoint permet à tous les utilisateurs (USER, GERANT, ADMIN) de récupérer
     * les informations de l'entreprise pour les afficher sur les factures et documents.
     *
     * @param authentication Informations de l'utilisateur authentifié
     * @return Informations du tenant (entreprise) avec les informations du propriétaire
     */
    @GetMapping("/tenant/info")
    public ResponseEntity<TenantResponse> getEntrepriseInfo(Authentication authentication) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        UserEntity admin = tenantService.getAdminProprietaire(tenant);

        TenantResponse response = new TenantResponse(
                tenant.getTenantUuid(),
                tenant.getNomEntreprise(),
                tenant.getNumeroTelephone(),
                tenant.getAdresse(),
                tenant.getVille(),
                tenant.getPays(),
                tenant.getNineaSiret(),
                tenant.getLogoUrl(),
                admin != null ? admin.getNom() : null,
                admin != null ? admin.getPrenom() : null,
                admin != null ? admin.getEmail() : null,
                tenant.getTimezone(),
                tenant.getConditionsGaranties(),
                tenant.getMentionsLegales()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Récupère les informations de l'entreprise actuelle (ADMIN uniquement)
     *
     * GET /api/admin/entreprise
     *
     * @param authentication Informations de l'utilisateur authentifié
     * @return Informations du tenant (entreprise) avec les informations du propriétaire
     */
    @GetMapping("/admin/entreprise")
    public ResponseEntity<TenantResponse> getEntreprise(Authentication authentication) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        UserEntity admin = tenantService.getAdminProprietaire(tenant);

        TenantResponse response = new TenantResponse(
                tenant.getTenantUuid(),
                tenant.getNomEntreprise(),
                tenant.getNumeroTelephone(),
                tenant.getAdresse(),
                tenant.getVille(),
                tenant.getPays(),
                tenant.getNineaSiret(),
                tenant.getLogoUrl(),
                admin != null ? admin.getNom() : null,
                admin != null ? admin.getPrenom() : null,
                admin != null ? admin.getEmail() : null,
                tenant.getTimezone(),
                tenant.getConditionsGaranties(),
                tenant.getMentionsLegales()
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
     * @return Informations mises à jour avec les informations du propriétaire
     */
    @PutMapping("/admin/entreprise")
    public ResponseEntity<TenantResponse> updateEntreprise(
            @Valid @RequestBody UpdateTenantRequest request,
            Authentication authentication
    ) {
        String emailAdmin = authentication.getName();
        log.info("Admin {} modifie les informations de son entreprise", emailAdmin);

        TenantEntity tenantMisAJour = tenantService.updateTenant(request);
        UserEntity admin = tenantService.getAdminProprietaire(tenantMisAJour);

        TenantResponse response = new TenantResponse(
                tenantMisAJour.getTenantUuid(),
                tenantMisAJour.getNomEntreprise(),
                tenantMisAJour.getNumeroTelephone(),
                tenantMisAJour.getAdresse(),
                tenantMisAJour.getVille(),
                tenantMisAJour.getPays(),
                tenantMisAJour.getNineaSiret(),
                tenantMisAJour.getLogoUrl(),
                admin != null ? admin.getNom() : null,
                admin != null ? admin.getPrenom() : null,
                admin != null ? admin.getEmail() : null,
                tenantMisAJour.getTimezone(),
                tenantMisAJour.getConditionsGaranties(),
                tenantMisAJour.getMentionsLegales()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Met à jour le fuseau horaire du tenant (multi-pays).
     *
     * PUT /api/admin/entreprise/timezone
     * Body : { "timezone": "Africa/Dakar" }
     *
     * Le fuseau (format IANA) est utilisé pour toutes les dates métier
     * server-side : activation caisse, transferts, mouvements, paiements crédit.
     */
    @PutMapping("/admin/entreprise/timezone")
    public ResponseEntity<TenantTimezoneResponse> updateTimezone(
            @RequestBody UpdateTimezoneRequest request,
            Authentication authentication
    ) {
        log.info("Admin {} modifie le fuseau horaire → {}",
                authentication.getName(), request.timezone());
        TenantEntity updated = tenantService.updateTimezone(request.timezone());
        return ResponseEntity.ok(new TenantTimezoneResponse(updated.getTimezone()));
    }

    public record UpdateTimezoneRequest(String timezone) {}
    public record TenantTimezoneResponse(String timezone) {}

    /**
     * DTO pour les réponses contenant les informations du tenant et du propriétaire
     */
    public record TenantResponse(
            String tenantUuid,
            String nomEntreprise,
            String numeroTelephone,
            String adresse,
            String ville,
            String pays,
            String nineaSiret,
            String logoUrl,
            String nomProprietaire,
            String prenomProprietaire,
            String emailProprietaire,
            String timezone,
            String conditionsGaranties,
            String mentionsLegales
    ) {}
}
