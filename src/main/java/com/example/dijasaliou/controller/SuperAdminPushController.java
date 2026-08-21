package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.NotificationPreferenceDto;
import com.example.dijasaliou.dto.PushSubscriptionRequest;
import com.example.dijasaliou.entity.NotificationType;
import com.example.dijasaliou.entity.SuperAdminPushSubscription;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.SuperAdminPushSubscriptionRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.service.NotificationPreferenceService;
import com.example.dijasaliou.service.PushNotificationService;
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
 * Endpoints d'abonnement et de préférences push pour les super admins.
 *
 * Push subscription :
 * - GET    /superadmin/push/public-key   → récupère la clé VAPID publique
 * - POST   /superadmin/push/subscribe    → enregistre un appareil
 * - DELETE /superadmin/push/subscribe    → désabonne un appareil
 * - POST   /superadmin/push/test         → envoie une notif de test
 *
 * Préférences notifications :
 * - GET    /superadmin/push/preferences         → liste de tous les types + leur état
 * - PUT    /superadmin/push/preferences/{type}  → active / désactive un type
 */
@RestController
@RequestMapping("/superadmin/push")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SuperAdminPushController {

    private final PushNotificationService pushService;
    private final SuperAdminPushSubscriptionRepository repository;
    private final UserRepository userRepository;
    private final NotificationPreferenceService preferenceService;

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

        SuperAdminPushSubscription existing = repository.findByEndpoint(req.endpoint()).orElse(null);
        if (existing != null) {
            existing.setP256dh(req.p256dh());
            existing.setAuthKey(req.auth());
            existing.setUserAgent(req.userAgent());
            existing.setUser(user);
            existing.setDerniereUtilisation(LocalDateTime.now());
            repository.save(existing);
        } else {
            repository.save(SuperAdminPushSubscription.builder()
                    .user(user)
                    .endpoint(req.endpoint())
                    .p256dh(req.p256dh())
                    .authKey(req.auth())
                    .userAgent(req.userAgent())
                    .dateCreation(LocalDateTime.now())
                    .build());
            log.info("[PUSH] Nouvelle subscription super admin enregistrée pour {}", auth.getName());
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
    public ResponseEntity<Map<String, String>> sendTest() {
        pushService.notifyAllSuperAdmins(
                "🔔 EasyStock — Test",
                "Tes notifications fonctionnent correctement !",
                "/superadmin/dashboard"
        );
        return ResponseEntity.ok(Map.of("message", "Notification de test envoyée"));
    }

    // ─── Préférences notifications ─────────────────────────────────────────

    @GetMapping("/preferences")
    public ResponseEntity<List<NotificationPreferenceDto>> getPreferences(Authentication auth) {
        return ResponseEntity.ok(preferenceService.getPreferences(auth.getName()));
    }

    @PutMapping("/preferences/{type}")
    public ResponseEntity<Map<String, String>> updatePreference(
            @PathVariable String type,
            @RequestBody Map<String, Boolean> body,
            Authentication auth) {
        NotificationType notifType;
        try {
            notifType = NotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Type de notification inconnu : " + type));
        }
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Champ 'enabled' obligatoire"));
        }
        preferenceService.updatePreference(auth.getName(), notifType, enabled);
        return ResponseEntity.ok(Map.of("message", "Préférence mise à jour"));
    }
}
