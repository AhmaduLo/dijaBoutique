package com.example.dijasaliou.controller;

import com.example.dijasaliou.service.SuperAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    // Token store : tenantId → [token, expiryDateTime]
    // Stockage in-memory suffisant (usage ultra-rare, un seul SUPER_ADMIN)
    private final ConcurrentHashMap<Long, String[]> confirmTokens = new ConcurrentHashMap<>();

    public SuperAdminDeleteController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    /**
     * POST /super-admin/tenants/{id}/demander-suppression
     * Étape 1 — Génère un token de confirmation valide 5 minutes
     * À appeler avant la suppression définitive
     */
    @PostMapping("/tenants/{id}/demander-suppression")
    public ResponseEntity<Map<String, String>> demanderSuppression(
            @PathVariable Long id, Authentication auth) {
        String token = UUID.randomUUID().toString();
        String expiry = LocalDateTime.now().plusMinutes(5).toString();
        confirmTokens.put(id, new String[]{token, expiry});
        log.warn("[SUPER_ADMIN] {} demande suppression tenant {} — token généré (valide 5 min)", auth.getName(), id);
        return ResponseEntity.ok(Map.of(
                "message", "Token de confirmation généré. Valide 5 minutes.",
                "confirmToken", token
        ));
    }

    /**
     * DELETE /super-admin/tenants/{id}/supprimer
     * Étape 2 — Suppression définitive IRRÉVERSIBLE
     * Header requis : X-Confirm-Token (obtenu via POST /demander-suppression)
     */
    @DeleteMapping("/tenants/{id}/supprimer")
    public ResponseEntity<Map<String, String>> supprimerTenant(
            @PathVariable Long id,
            @RequestHeader(value = "X-Confirm-Token", required = false) String confirmToken,
            Authentication auth) {

        String[] tokenData = confirmTokens.get(id);

        if (tokenData == null || confirmToken == null || !confirmToken.equals(tokenData[0])) {
            log.warn("[SUPER_ADMIN] {} — suppression refusée tenant {} : token manquant ou invalide", auth.getName(), id);
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Token de confirmation requis. Appelez d'abord POST /super-admin/tenants/{id}/demander-suppression."
            ));
        }

        if (LocalDateTime.now().isAfter(LocalDateTime.parse(tokenData[1]))) {
            confirmTokens.remove(id);
            log.warn("[SUPER_ADMIN] {} — suppression refusée tenant {} : token expiré", auth.getName(), id);
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Token de confirmation expiré (5 min). Générez-en un nouveau."
            ));
        }

        confirmTokens.remove(id); // usage unique
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

    /**
     * PUT /super-admin/tenants/{id}/restaurer
     * Restaure un tenant supprimé — remet deleted=false + actif=true + utilisateurs actifs
     */
    @PutMapping("/tenants/{id}/restaurer")
    public ResponseEntity<Map<String, String>> restaurerTenant(
            @PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} restaure le tenant {}", auth.getName(), id);
        superAdminService.restaurerTenant(id);
        return ResponseEntity.ok(Map.of("message", "Tenant restauré avec succès"));
    }
}
