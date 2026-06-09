package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.NotificationType;
import com.example.dijasaliou.entity.SuperAdminPushSubscription;
import com.example.dijasaliou.repository.SuperAdminPushSubscriptionRepository;
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
 * Envoi de notifications Web Push aux super admins.
 *
 * Configuration : nécessite VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, VAPID_SUBJECT
 * dans les variables d'environnement. Si non configuré → désactivé silencieusement.
 */
@Service
@Slf4j
public class PushNotificationService {

    private final SuperAdminPushSubscriptionRepository repository;
    private final NotificationPreferenceService preferenceService;
    private final ObjectMapper objectMapper;
    private final String publicKey;
    private final String privateKey;
    private final String subject;

    private PushService pushService;
    private boolean enabled;

    public PushNotificationService(
            SuperAdminPushSubscriptionRepository repository,
            NotificationPreferenceService preferenceService,
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
            log.warn("[PUSH] VAPID keys non configurées → push notifications désactivées.");
            enabled = false;
            return;
        }
        try {
            Security.addProvider(new BouncyCastleProvider());
            pushService = new PushService(publicKey, privateKey, subject);
            enabled = true;
            log.info("[PUSH] Service push initialisé (subject={})", subject);
        } catch (Exception e) {
            log.error("[PUSH] Échec d'initialisation du PushService : {}", e.getMessage());
            enabled = false;
        }
    }

    /**
     * Retourne la clé publique VAPID pour le frontend (nécessaire pour s'abonner).
     */
    public String getPublicKey() {
        return publicKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Envoie une notification typée à tous les super admins abonnés
     * qui ont activé ce type dans leurs préférences.
     * Non bloquant : @Async ne ralentit pas la requête appelante.
     */
    @Async
    public void notify(NotificationType type, String title, String body, String url) {
        if (!enabled) return;
        if (!preferenceService.isAnyoneSubscribedTo(type)) {
            log.debug("[PUSH] Type {} désactivé pour tous les super admins, skip", type);
            return;
        }

        List<SuperAdminPushSubscription> subs = repository.findAll();
        if (subs.isEmpty()) return;

        String payload = buildPayload(title, body, url);
        for (SuperAdminPushSubscription sub : subs) {
            sendOne(sub, payload);
        }
    }

    /**
     * @deprecated utiliser {@link #notify(NotificationType, String, String, String)}.
     * Conservée pour le test endpoint et pour ne pas casser d'éventuels appels existants.
     */
    @Deprecated
    @Async
    public void notifyAllSuperAdmins(String title, String body, String url) {
        if (!enabled) return;
        List<SuperAdminPushSubscription> subs = repository.findAll();
        if (subs.isEmpty()) return;
        String payload = buildPayload(title, body, url);
        for (SuperAdminPushSubscription sub : subs) {
            sendOne(sub, payload);
        }
    }

    private String buildPayload(String title, String body, String url) {
        // Format attendu par le service worker Angular (ngsw-worker.js)
        // pour afficher la notification au niveau système.
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("icon", "/icons/icon-192x192.png");
        notification.put("badge", "/icons/icon-72x72.png");
        notification.put("vibrate", new int[]{200, 100, 200});
        notification.put("tag", "easystock-superadmin");
        notification.put("renotify", true);
        if (url != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("url", url);
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
    protected void sendOne(SuperAdminPushSubscription sub, String payload) {
        try {
            Subscription.Keys keys = new Subscription.Keys(sub.getP256dh(), sub.getAuthKey());
            Subscription subscription = new Subscription(sub.getEndpoint(), keys);
            Notification notification = new Notification(subscription, payload);

            var response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 410 || statusCode == 404) {
                log.info("[PUSH] Subscription expirée/invalide ({}), suppression endpoint={}",
                        statusCode, sub.getEndpoint().substring(0, Math.min(60, sub.getEndpoint().length())));
                repository.delete(sub);
            } else if (statusCode >= 200 && statusCode < 300) {
                sub.setDerniereUtilisation(LocalDateTime.now());
                repository.save(sub);
            } else {
                log.warn("[PUSH] Envoi échoué ({}) pour subscription {}", statusCode, sub.getId());
            }
        } catch (Exception e) {
            log.warn("[PUSH] Erreur d'envoi pour subscription {} : {}", sub.getId(), e.getMessage());
        }
    }
}
