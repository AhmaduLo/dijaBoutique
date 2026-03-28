package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.AuditLogDto;
import com.example.dijasaliou.dto.FactureDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.TenantAdminDto;
import com.example.dijasaliou.dto.UtilisateurTenantDto;
import com.example.dijasaliou.entity.NoteInterne;
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
        if (size > 100) size = 100;
        return ResponseEntity.ok(superAdminService.getAllTenants(page, size));
    }

    /**
     * GET /superadmin/tenants/supprimes
     * Liste les tenants supprimés (deleted = true)
     */
    @GetMapping("/tenants/supprimes")
    public ResponseEntity<List<TenantAdminDto>> getTenantsSupprimés(Authentication auth) {
        log.info("[SUPER_ADMIN] {} consulte les tenants supprimés", auth.getName());
        return ResponseEntity.ok(superAdminService.getSupprimesTenants());
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
     * Activer un tenant (ancien endpoint — conservé pour compatibilité)
     */
    @PutMapping("/tenants/{id}/activate")
    public ResponseEntity<Map<String, String>> activateTenant(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} active le tenant {}", auth.getName(), id);
        superAdminService.activerTenant(id);
        return ResponseEntity.ok(Map.of("message", "Tenant activé avec succès"));
    }

    /**
     * PUT /superadmin/tenants/{id}/deactivate
     * Désactiver un tenant (ancien endpoint — conservé pour compatibilité)
     */
    @PutMapping("/tenants/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateTenant(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} désactive le tenant {}", auth.getName(), id);
        superAdminService.desactiverTenant(id);
        return ResponseEntity.ok(Map.of("message", "Tenant désactivé avec succès"));
    }

    /**
     * PUT /superadmin/tenants/{id}/suspendre
     * Bascule suspension/réactivation — RÉVERSIBLE
     * Si actif → suspendu ; si suspendu → réactivé
     */
    @PutMapping("/tenants/{id}/suspendre")
    public ResponseEntity<Map<String, String>> suspendTenant(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} bascule suspension du tenant {}", auth.getName(), id);
        superAdminService.basculerSuspensionTenant(id);
        return ResponseEntity.ok(Map.of("message", "Statut du tenant mis à jour (suspension basculée)"));
    }

    /**
     * DELETE /superadmin/tenants/{id}/supprimer
     * Suppression définitive — IRRÉVERSIBLE depuis l'interface
     * deleted=true + dateSuppression + tous les utilisateurs soft-deletés
     */
    @DeleteMapping("/tenants/{id}/supprimer")
    public ResponseEntity<Map<String, String>> supprimerTenantDefinitif(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} supprime définitivement le tenant {}", auth.getName(), id);
        superAdminService.supprimerTenant(id);
        return ResponseEntity.ok(Map.of("message", "Tenant définitivement supprimé"));
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
     * GET /superadmin/tenants/{id}/utilisateurs
     * Liste les utilisateurs actifs d'un tenant
     */
    @GetMapping("/tenants/{id}/utilisateurs")
    public ResponseEntity<List<UtilisateurTenantDto>> getUtilisateurs(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} consulte les utilisateurs du tenant {}", auth.getName(), id);
        return ResponseEntity.ok(superAdminService.getUtilisateursByTenant(id));
    }

    /**
     * GET /superadmin/tenants/{id}/notes
     * Liste les notes internes d'un tenant
     */
    @GetMapping("/tenants/{id}/notes")
    public ResponseEntity<List<NoteInterne>> getNotes(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} consulte les notes du tenant {}", auth.getName(), id);
        return ResponseEntity.ok(superAdminService.getNotesByTenant(id));
    }

    /**
     * POST /superadmin/tenants/{id}/notes
     * Ajouter une note interne sur un tenant
     * Body: { "contenu": "..." }
     */
    @PostMapping("/tenants/{id}/notes")
    public ResponseEntity<NoteInterne> ajouterNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String contenu = body.get("contenu");
        log.info("[SUPER_ADMIN] {} ajoute une note sur le tenant {}", auth.getName(), id);
        return ResponseEntity.ok(superAdminService.ajouterNote(id, contenu, auth.getName()));
    }

    /**
     * DELETE /superadmin/tenants/{id}/notes/{noteId}
     * Supprimer une note interne
     */
    @DeleteMapping("/tenants/{id}/notes/{noteId}")
    public ResponseEntity<Map<String, String>> supprimerNote(
            @PathVariable Long id,
            @PathVariable Long noteId,
            Authentication auth) {
        log.info("[SUPER_ADMIN] {} supprime la note {} du tenant {}", auth.getName(), noteId, id);
        superAdminService.supprimerNote(id, noteId);
        return ResponseEntity.ok(Map.of("message", "Note supprimée avec succès"));
    }

    /**
     * PATCH /superadmin/tenants/{id}/source
     * Mettre à jour la source d'acquisition d'un tenant
     * Body: { "sourceAcquisition": "..." }
     */
    @PatchMapping("/tenants/{id}/source")
    public ResponseEntity<TenantAdminDto> updateSource(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String source = body.get("sourceAcquisition");
        log.info("[SUPER_ADMIN] {} met à jour la source d'acquisition du tenant {}", auth.getName(), id);
        return ResponseEntity.ok(superAdminService.mettreAJourSourceAcquisition(id, source));
    }

    /**
     * GET /superadmin/tenants/{id}/audit
     * Historique des actions super-admin sur un tenant
     */
    @GetMapping("/tenants/{id}/audit")
    public ResponseEntity<List<AuditLogDto>> getAuditLogs(@PathVariable Long id, Authentication auth) {
        log.info("[SUPER_ADMIN] {} consulte l'audit du tenant {}", auth.getName(), id);
        return ResponseEntity.ok(superAdminService.getAuditLogs(id));
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
