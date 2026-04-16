package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.NotificationEntity;
import com.example.dijasaliou.entity.NotificationLueEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.NotificationLueRepository;
import com.example.dijasaliou.repository.NotificationRepository;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationLueRepository notificationLueRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Envoyer une notification (in-app + email optionnel).
     *
     * @param objet       Objet du message
     * @param message     Corps du message
     * @param tenantId    null = tous les tenants, sinon ciblé
     * @param filtrePlan  null = tous les plans, sinon "PRO", "BUSINESS", etc.
     * @param canalApp    Afficher dans l'app
     * @param canalEmail  Envoyer par email
     * @param envoyePar   Nom du super admin
     */
    @Transactional
    public NotificationEntity envoyer(String objet, String message, Long tenantId,
                                       String filtrePlan, boolean canalApp, boolean canalEmail,
                                       String envoyePar) {

        TenantEntity tenant = null;
        if (tenantId != null) {
            tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + tenantId));
        }

        // Trouver les destinataires
        List<TenantEntity> destinataires;
        if (tenant != null) {
            destinataires = List.of(tenant);
        } else {
            destinataires = tenantRepository.findByDeletedFalse();
            if (filtrePlan != null && !filtrePlan.isBlank()) {
                TenantEntity.Plan plan = TenantEntity.Plan.valueOf(filtrePlan.toUpperCase());
                destinataires = destinataires.stream()
                        .filter(t -> t.getPlan() == plan)
                        .toList();
            }
        }

        // Sauvegarder la notification
        NotificationEntity notification = NotificationEntity.builder()
                .objet(objet)
                .message(message)
                .tenant(tenant)
                .filtrePlan(filtrePlan)
                .canalApp(canalApp)
                .canalEmail(canalEmail)
                .nbDestinataires(destinataires.size())
                .envoyePar(envoyePar)
                .build();
        notification = notificationRepository.save(notification);
        if (tenant != null) {
            notification.setTenantNom(tenant.getNomEntreprise());
        }

        // Envoyer par email si demandé
        if (canalEmail) {
            envoyerEmails(destinataires, objet, message);
        }

        log.info("[NOTIFICATION] '{}' envoyée à {} destinataires (app={}, email={})",
                objet, destinataires.size(), canalApp, canalEmail);

        return notification;
    }

    /**
     * Envoyer les emails en arrière-plan
     */
    @Async
    protected void envoyerEmails(List<TenantEntity> destinataires, String objet, String message) {
        for (TenantEntity t : destinataires) {
            try {
                // Trouver l'admin du tenant
                UserEntity admin = userRepository.findFirstByTenantAndRole(t, UserEntity.Role.ADMIN)
                        .orElse(null);
                if (admin == null || admin.getEmail() == null) continue;

                String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px'>"
                        + "<h2 style='color:#2563eb'>📢 " + objet + "</h2>"
                        + "<p>" + message.replace("\n", "<br>") + "</p>"
                        + "<hr style='border:none;border-top:1px solid #eee;margin:20px 0'>"
                        + "<p style='color:#666;font-size:12px'>HeasyStock — Gestion commerciale</p>"
                        + "</div>";

                emailService.sendHtmlEmailPublic(admin.getEmail(), objet, htmlContent);
            } catch (Exception e) {
                log.warn("Erreur envoi email notification à tenant {} : {}", t.getNomEntreprise(), e.getMessage());
            }
        }
    }

    /**
     * Notifications non lues pour le tenant courant
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNotificationsNonLues(Long tenantId, String plan) {
        List<NotificationEntity> notifs = notificationRepository.findNonLuesParTenant(tenantId, plan);
        List<Map<String, Object>> result = new ArrayList<>();
        for (NotificationEntity n : notifs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", n.getId());
            entry.put("objet", n.getObjet());
            entry.put("message", n.getMessage());
            entry.put("dateEnvoi", n.getDateEnvoi());
            result.add(entry);
        }
        return result;
    }

    /**
     * Marquer une notification comme lue pour un tenant
     */
    @Transactional
    public void marquerCommeLue(Long notificationId, Long tenantId) {
        if (notificationLueRepository.existsByNotificationIdAndTenantId(notificationId, tenantId)) {
            return; // Déjà lue
        }

        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        NotificationLueEntity lue = NotificationLueEntity.builder()
                .notification(notification)
                .tenant(tenant)
                .build();
        notificationLueRepository.save(lue);
    }

    /**
     * Historique des notifications envoyées (super admin)
     */
    @Transactional(readOnly = true)
    public Page<NotificationEntity> getHistorique(int page, int size) {
        Page<NotificationEntity> notifs = notificationRepository.findAllByOrderByDateEnvoiDesc(PageRequest.of(page, size));
        // Remplir tenantNom pour l'affichage JSON
        notifs.forEach(n -> {
            if (n.getTenant() != null) {
                n.setTenantNom(n.getTenant().getNomEntreprise());
            }
        });
        return notifs;
    }
}
