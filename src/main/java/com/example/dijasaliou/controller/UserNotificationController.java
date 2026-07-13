package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.PushSubscriptionRequest;
import com.example.dijasaliou.dto.UserNotificationPreferenceDto;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.UserNotificationType;
import com.example.dijasaliou.entity.UserPushSubscription;
import com.example.dijasaliou.repository.UserPushSubscriptionRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.service.UserNotificationPreferenceService;
import com.example.dijasaliou.service.UserPushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Endpoints Web Push et préférences de notifications pour les utilisateurs (admins d'un tenant).
 *
 * Réservé au rôle ADMIN en Phase 1 — les gérants/vendeurs ne configurent rien
 * de leur côté et reçoivent uniquement les alertes opérationnelles (stock, vente à perte).
 *
 * Push subscription :
 * - GET    /notifications/push/public-key   → récupère la clé VAPID publique
 * - POST   /notifications/push/subscribe    → enregistre un appareil
 * - DELETE /notifications/push/subscribe    → désabonne un appareil
 * - POST   /notifications/push/test         → envoie une notif de test
 *
 * Préférences :
 * - GET    /notifications/push/preferences         → liste de tous les types + leur état
 * - PUT    /notifications/push/preferences/{type}  → active / désactive un type
 */
@RestController
@RequestMapping("/notifications/push")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class UserNotificationController {

    private final UserPushNotificationService pushService;
    private final UserPushSubscriptionRepository repository;
    private final UserRepository userRepository;
    private final UserNotificationPreferenceService preferenceService;

    @GetMapping("/public-key")
    public ResponseEntity<Map<String, Object>> getPublicKey() {
        return ResponseEntity.ok(Map.of(
                "publicKey", pushService.getPublicKey() != null ? pushService.getPublicKey() : "",
                "enabled", pushService.isEnabled()
        ));
    }

    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<Map<String, String>> subscribe(
            @Valid @RequestBody PushSubscriptionRequest req,
            Authentication auth) {

        UserEntity user = userRepository.findByEmailAndDeletedFalse(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        TenantEntity tenant = user.getTenant();
        if (tenant == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "L'utilisateur n'appartient à aucun tenant"));
        }

        UserPushSubscription existing = repository.findByEndpoint(req.endpoint()).orElse(null);
        if (existing != null) {
            existing.setP256dh(req.p256dh());
            existing.setAuthKey(req.auth());
            existing.setUserAgent(req.userAgent());
            existing.setUser(user);
            existing.setTenant(tenant);
            existing.setDerniereUtilisation(LocalDateTime.now());
            repository.save(existing);
        } else {
            repository.save(UserPushSubscription.builder()
                    .user(user)
                    .tenant(tenant)
                    .endpoint(req.endpoint())
                    .p256dh(req.p256dh())
                    .authKey(req.auth())
                    .userAgent(req.userAgent())
                    .dateCreation(LocalDateTime.now())
                    .build());
            log.info("[USER_PUSH] Nouvelle subscription enregistrée pour {} (tenant={})",
                    auth.getName(), tenant.getNomEntreprise());
        }
        return ResponseEntity.ok(Map.of("message", "Notifications activées sur cet appareil"));
    }

    @DeleteMapping("/subscribe")
    @Transactional
    public ResponseEntity<Map<String, String>> unsubscribe(@RequestParam String endpoint) {
        repository.deleteByEndpoint(endpoint);
        return ResponseEntity.ok(Map.of("message", "Notifications désactivées sur cet appareil"));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTest(Authentication auth) {
        UserEntity user = userRepository.findByEmailAndDeletedFalse(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Envoi de test sans passer par les préférences : on veut valider l'infra
        // même si aucun type "test" n'existe. On envoie directement à cet user.
        pushService.notifyUser(
                user,
                UserNotificationType.STOCK_BAS, // type technique pour passer le filtre — l'utilisateur en test l'a probablement activé
                "🔔 EasyStock — Test",
                "Tes notifications fonctionnent correctement !",
                "/notifications"
        );
        return ResponseEntity.ok(Map.of("message", "Notification de test envoyée"));
    }

    // ─── Préférences ───────────────────────────────────────────────────────

    @GetMapping("/preferences")
    public ResponseEntity<List<UserNotificationPreferenceDto>> getPreferences(Authentication auth) {
        return ResponseEntity.ok(preferenceService.getPreferences(auth.getName()));
    }

    /**
     * Body attendu : { "enabled": boolean, "config": object|null }
     *
     * Le champ config est optionnel. Quand présent, il est validé selon le schéma
     * du type puis sérialisé en JSON pour persistance. Un config invalide → 400.
     */
    @PutMapping("/preferences/{type}")
    public ResponseEntity<Map<String, String>> updatePreference(
            @PathVariable String type,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        UserNotificationType notifType;
        try {
            notifType = UserNotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Type de notification inconnu : " + type));
        }
        Object enabledRaw = body.get("enabled");
        if (!(enabledRaw instanceof Boolean enabled)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Champ 'enabled' obligatoire (boolean)"));
        }

        // Validation optionnelle du bloc config selon le type.
        // Pour l'instant, seul RESUME_QUOTIDIEN a une config typée ; les autres
        // types acceptent le champ mais ne le lisent pas (à câbler en Phase 2).
        String configJson = null;
        Object configRaw = body.get("config");
        if (configRaw != null) {
            try {
                configJson = preferenceService.validateAndSerializeConfig(notifType, configRaw);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Config invalide : " + e.getMessage()));
            }
        }

        preferenceService.updatePreference(auth.getName(), notifType, enabled, configJson);
        return ResponseEntity.ok(Map.of("message", "Préférence mise à jour"));
    }
}
