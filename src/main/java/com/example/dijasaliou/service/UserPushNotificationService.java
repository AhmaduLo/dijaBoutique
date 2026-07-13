package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.UserNotificationType;
import com.example.dijasaliou.entity.UserPushSubscription;
import com.example.dijasaliou.repository.UserPushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Security;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envoi de notifications Web Push aux utilisateurs (admins) d'un tenant.
 *
 * Miroir de {@link PushNotificationService} (super admins) — même VAPID,
 * mais ciblage tenant/user et filtre via {@link UserNotificationPreferenceService}.
 *
 * Configuration : réutilise VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, VAPID_SUBJECT.
 * Si non configuré → désactivé silencieusement.
 */
@Service
@Slf4j
public class UserPushNotificationService {

    private final UserPushSubscriptionRepository repository;
    private final UserNotificationPreferenceService preferenceService;
    private final ObjectMapper objectMapper;
    private final String publicKey;
    private final String privateKey;
    private final String subject;

    private PushService pushService;
    private boolean enabled;

    public UserPushNotificationService(
            UserPushSubscriptionRepository repository,
            UserNotificationPreferenceService preferenceService,
            ObjectMapper objectMapper,
            @Value("${vapid.public.key:}") String publicKey,
            @Value("${vapid.private.key:}") String privateKey,
            @Value("${vapid.subject:mailto:contact@heasystock.com}") String subject) {
        this.repository = repository;
        this.preferenceService = preferenceService;
        this.objectMapper = objectMapper;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    @PostConstruct
    public void init() {
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.warn("[USER_PUSH] VAPID keys non configurées → push notifications utilisateur désactivées.");
            enabled = false;
            return;
        }
        try {
            Security.addProvider(new BouncyCastleProvider());
            pushService = new PushService(publicKey, privateKey, subject);
            enabled = true;
            log.info("[USER_PUSH] Service push utilisateur initialisé (subject={})", subject);
        } catch (Exception e) {
            log.error("[USER_PUSH] Échec d'initialisation du PushService : {}", e.getMessage());
            enabled = false;
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Envoie une notification typée à un utilisateur donné, sur tous ses appareils,
     * uniquement s'il a activé (ou laissé par défaut activé) ce type.
     *
     * Non bloquant : @Async ne ralentit pas la requête appelante.
     *
     * @param user utilisateur destinataire (souvent l'admin d'un tenant)
     * @param type type de notification (utilisé pour filtrer selon les préférences)
     * @param title titre affiché sur la notification
     * @param body corps du message
     * @param url URL vers laquelle rediriger au clic (route Angular, sans domaine)
     */
    @Async
    public void notifyUser(UserEntity user, UserNotificationType type,
                           String title, String body, String url) {
        if (!enabled) return;
        if (user == null) return;
        if (!preferenceService.isEnabled(user, type)) {
            log.debug("[USER_PUSH] Type {} désactivé pour user {}, skip", type, user.getEmail());
            return;
        }

        List<UserPushSubscription> subs = repository.findByUser(user);
        if (subs.isEmpty()) {
            log.debug("[USER_PUSH] Aucune subscription pour user {}", user.getEmail());
            return;
        }

        String payload = buildPayload(title, body, url);
        for (UserPushSubscription sub : subs) {
            sendOne(sub, payload);
        }
    }

    /**
     * Envoie à tous les admins d'un tenant qui ont activé ce type.
     * Utilisé quand l'événement n'est pas rattaché à un user précis
     * (par ex. cron de résumé quotidien).
     */
    @Async
    public void notifyTenantAdmins(TenantEntity tenant, UserNotificationType type,
                                   String title, String body, String url) {
        if (!enabled) return;
        if (tenant == null) return;

        List<UserPushSubscription> subs = repository.findByTenant(tenant);
        if (subs.isEmpty()) return;

        String payload = buildPayload(title, body, url);
        for (UserPushSubscription sub : subs) {
            // Filtrer par préférence de chaque user propriétaire de la subscription
            if (!preferenceService.isEnabled(sub.getUser(), type)) continue;
            sendOne(sub, payload);
        }
    }

    private String buildPayload(String title, String body, String url) {
        // Format attendu par le service worker Angular (ngsw-worker.js)
        // Voir PushNotificationService pour la doc détaillée du bloc onActionClick :
        // navigateLastFocusedOrOpen permet au clic d'ouvrir la PWA directement
        // sur l'URL cible même quand elle est fermée.
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("icon", "/icons/icon-192x192.png");
        notification.put("badge", "/icons/icon-72x72.png");
        notification.put("vibrate", new int[]{200, 100, 200});
        notification.put("tag", "easystock-user");
        notification.put("renotify", true);
        if (url != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("url", url);
            Map<String, Object> onActionClick = new LinkedHashMap<>();
            Map<String, Object> defaultAction = new LinkedHashMap<>();
            defaultAction.put("operation", "navigateLastFocusedOrOpen");
            defaultAction.put("url", url);
            onActionClick.put("default", defaultAction);
            data.put("onActionClick", onActionClick);
            notification.put("data", data);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("notification", notification);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"notification\":{\"title\":\"" + title + "\",\"body\":\"" + body + "\"}}";
        }
    }

    @Transactional
    protected void sendOne(UserPushSubscription sub, String payload) {
        try {
            Subscription.Keys keys = new Subscription.Keys(sub.getP256dh(), sub.getAuthKey());
            Subscription subscription = new Subscription(sub.getEndpoint(), keys);
            Notification notification = new Notification(subscription, payload);

            var response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 410 || statusCode == 404) {
                log.info("[USER_PUSH] Subscription expirée/invalide ({}), suppression endpoint={}",
                        statusCode, sub.getEndpoint().substring(0, Math.min(60, sub.getEndpoint().length())));
                repository.delete(sub);
            } else if (statusCode >= 200 && statusCode < 300) {
                sub.setDerniereUtilisation(LocalDateTime.now());
                repository.save(sub);
            } else {
                log.warn("[USER_PUSH] Envoi échoué ({}) pour subscription {}", statusCode, sub.getId());
            }
        } catch (Exception e) {
            log.warn("[USER_PUSH] Erreur d'envoi pour subscription {} : {}", sub.getId(), e.getMessage());
        }
    }
}
