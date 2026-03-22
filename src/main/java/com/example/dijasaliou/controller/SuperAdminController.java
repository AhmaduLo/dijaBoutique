package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.FactureDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.TenantAdminDto;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.service.SuperAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller Super Admin — gestion de tous les tenants de la plateforme
 *
 * SÉCURITÉ : Toutes les routes nécessitent l'autorité SUPER_ADMIN
 * Configuré aussi dans SecurityConfig : /superadmin/** → hasAuthority("SUPER_ADMIN")
 *
 * Routes :
 * GET  /superadmin/stats                     → Stats globales
 * GET  /superadmin/tenants                   → Liste tous les tenants
 * GET  /superadmin/tenants/{id}              → Détail d'un tenant
 * PUT  /superadmin/tenants/{id}/activate     → Activer
 * PUT  /superadmin/tenants/{id}/deactivate   → Désactiver
 * PUT  /superadmin/tenants/{id}/plan         → Changer plan + durée
 * DELETE /superadmin/tenants/{id}            → Supprimer (soft delete)
 */
@RestController
@RequestMapping("/superadmin")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Slf4j
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    /**
     * GET /superadmin/stats
     * Stats globales de la plateforme
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Authentication auth) {
        log.info("[SUPER_ADMIN] {} consulte les stats globales", auth.getName());
        return ResponseEntity.ok(superAdminService.getGlobalStats());
    }

    /**
     * GET /superadmin/tenants
     * Liste tous les tenants avec leurs stats
     */
    @GetMapping("/tenants")
    public ResponseEntity<PagedResponse<TenantAdminDto>> getAllTenants(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[SUPER_ADMIN] {} consulte tous les tenants", auth.getName());
        return ResponseEntity.ok(superAdminService.getAllTenants(page, size));
    }

    /**
     * GET /superadmin/tenants/{id}
     * Détail d'un tenant
     */
    @GetMapping("/tenants/{id}")
    public ResponseEntity<TenantAdminDto> getTenant(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} consulte le tenant {}", auth.getName(), id);
        return ResponseEntity.ok(superAdminService.getTenantById(id));
    }

    /**
     * PUT /superadmin/tenants/{id}/activate
     * Activer un tenant
     */
    @PutMapping("/tenants/{id}/activate")
    public ResponseEntity<Map<String, String>> activateTenant(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} active le tenant {}", auth.getName(), id);
        superAdminService.activerTenant(id);
        return ResponseEntity.ok(Map.of("message", "Tenant activé avec succès"));
    }

    /**
     * PUT /superadmin/tenants/{id}/deactivate
     * Désactiver un tenant
     */
    @PutMapping("/tenants/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateTenant(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} désactive le tenant {}", auth.getName(), id);
        superAdminService.desactiverTenant(id);
        return ResponseEntity.ok(Map.of("message", "Tenant désactivé avec succès"));
    }

    /**
     * PUT /superadmin/tenants/{id}/plan
     * Changer le plan d'un tenant
     * Body: { "plan": "ENTREPRISE", "jours": 30 }
     */
    @PutMapping("/tenants/{id}/plan")
    public ResponseEntity<TenantAdminDto> changePlan(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        String planStr = (String) body.get("plan");
        int jours = body.containsKey("jours") ? (Integer) body.get("jours") : 30;

        TenantEntity.Plan plan = TenantEntity.Plan.valueOf(planStr);

        log.info("[SUPER_ADMIN] {} change le plan du tenant {} → {} ({} jours)",
                auth.getName(), id, plan, jours);

        TenantAdminDto updated = superAdminService.changerPlan(id, plan, jours);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /superadmin/tenants/{id}
     * Supprimer un tenant (soft delete)
     */
    @DeleteMapping("/tenants/{id}")
    public ResponseEntity<Map<String, String>> deleteTenant(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} supprime le tenant {}", auth.getName(), id);
        superAdminService.supprimerTenant(id);
        return ResponseEntity.ok(Map.of("message", "Tenant supprimé avec succès"));
    }

    /**
     * GET /superadmin/tenants/{id}/factures
     * Liste toutes les factures d'un tenant (historique des paiements)
     */
    @GetMapping("/tenants/{id}/factures")
    public ResponseEntity<List<FactureDto>> getFactures(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} consulte les factures du tenant {}", auth.getName(), id);
        return ResponseEntity.ok(superAdminService.getFacturesByTenant(id));
    }

    /**
     * POST /superadmin/factures/{factureId}/send
     * Envoie une facture par email à l'admin du tenant
     */
    @PostMapping("/factures/{factureId}/send")
    public ResponseEntity<FactureDto> sendFacture(@PathVariable Long factureId, Authentication auth) {
        log.info("[SUPER_ADMIN] {} envoie la facture {} par email", auth.getName(), factureId);
        FactureDto updated = superAdminService.envoyerFacture(factureId);
        return ResponseEntity.ok(updated);
    }
}
