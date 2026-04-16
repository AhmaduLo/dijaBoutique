package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.NotificationEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.service.NotificationService;
import com.example.dijasaliou.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final TenantService tenantService;

    // ═══════════════════════════════════════════════════════════
    // SUPER ADMIN — Envoyer des notifications
    // ═══════════════════════════════════════════════════════════

    /**
     * POST /api/superadmin/notifications
     * Envoyer une notification (in-app + email optionnel)
     *
     * Body: {
     *   "objet": "Nouvelle fonctionnalité",
     *   "message": "Le scan code-barre est disponible...",
     *   "tenantId": null,          // null = tous, sinon ID ciblé
     *   "filtrePlan": null,        // null = tous, "PRO", "BUSINESS", etc.
     *   "canalApp": true,
     *   "canalEmail": true
     * }
     */
    @PostMapping("/superadmin/notifications")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> envoyer(@RequestBody Map<String, Object> body, Authentication auth) {
        String objet = (String) body.get("objet");
        String message = (String) body.get("message");

        if (objet == null || objet.isBlank() || message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Objet et message sont obligatoires"));
        }

        Long tenantId = body.get("tenantId") != null ? ((Number) body.get("tenantId")).longValue() : null;
        String filtrePlan = (String) body.get("filtrePlan");
        boolean canalApp = body.get("canalApp") != null ? (Boolean) body.get("canalApp") : true;
        boolean canalEmail = body.get("canalEmail") != null ? (Boolean) body.get("canalEmail") : false;

        NotificationEntity notif = notificationService.envoyer(
                objet, message, tenantId, filtrePlan, canalApp, canalEmail, auth.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Notification envoyée à " + notif.getNbDestinataires() + " destinataire(s)",
                "nbDestinataires", notif.getNbDestinataires(),
                "id", notif.getId()
        ));
    }

    /**
     * GET /api/superadmin/notifications?page=0&size=20
     * Historique des notifications envoyées
     */
    @GetMapping("/superadmin/notifications")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Page<NotificationEntity>> historique(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size > 100) size = 100;
        return ResponseEntity.ok(notificationService.getHistorique(page, size));
    }

    /**
     * DELETE /api/superadmin/notifications/{id}
     * Supprimer une notification
     */
    @DeleteMapping("/superadmin/notifications/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        notificationService.getHistorique(0, 1); // vérifier existence
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════
    // TENANTS — Recevoir les notifications
    // ═══════════════════════════════════════════════════════════

    /**
     * GET /api/notifications
     * Notifications non lues pour le tenant courant
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<Map<String, Object>>> mesNotifications() {
        TenantEntity tenant = tenantService.getCurrentTenant();
        String plan = tenant.getPlan() != null ? tenant.getPlan().name() : "GRATUIT";
        return ResponseEntity.ok(notificationService.getNotificationsNonLues(tenant.getId(), plan));
    }

    /**
     * POST /api/notifications/{id}/lue
     * Marquer une notification comme lue (fermer la bannière)
     */
    @PostMapping("/notifications/{id}/lue")
    public ResponseEntity<Map<String, String>> marquerLue(@PathVariable Long id) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        notificationService.marquerCommeLue(id, tenant.getId());
        return ResponseEntity.ok(Map.of("message", "Notification marquée comme lue"));
    }
}
