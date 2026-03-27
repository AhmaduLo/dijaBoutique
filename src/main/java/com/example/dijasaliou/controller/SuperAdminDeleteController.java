package com.example.dijasaliou.controller;

import com.example.dijasaliou.service.SuperAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller complémentaire pour les routes Super Admin avec tiret (/super-admin/)
 * Le frontend Angular utilise /super-admin/ pour certaines opérations (ex: suppression).
 * Ce controller délègue au SuperAdminService.
 */
@RestController
@RequestMapping("/super-admin")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Slf4j
public class SuperAdminDeleteController {

    private final SuperAdminService superAdminService;

    public SuperAdminDeleteController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    /**
     * DELETE /super-admin/tenants/{id}/supprimer
     * Suppression définitive — IRRÉVERSIBLE depuis l'interface
     */
    @DeleteMapping("/tenants/{id}/supprimer")
    public ResponseEntity<Map<String, String>> supprimerTenant(
            @PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} supprime définitivement le tenant {}", auth.getName(), id);
        superAdminService.supprimerTenant(id);
        return ResponseEntity.ok(Map.of("message", "Tenant définitivement supprimé"));
    }

    /**
     * PUT /super-admin/tenants/{id}/suspendre
     * Bascule suspension/réactivation — RÉVERSIBLE
     */
    @PutMapping("/tenants/{id}/suspendre")
    public ResponseEntity<Map<String, String>> suspendTenant(
            @PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} bascule suspension du tenant {}", auth.getName(), id);
        superAdminService.basculerSuspensionTenant(id);
        return ResponseEntity.ok(Map.of("message", "Statut du tenant mis à jour"));
    }
}
