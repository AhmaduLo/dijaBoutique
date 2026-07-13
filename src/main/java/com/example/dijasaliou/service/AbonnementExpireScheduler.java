package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.UserNotificationType;
import com.example.dijasaliou.repository.UserPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Rappels d'expiration d'abonnement — envoie un push à l'admin propriétaire
 * quand l'abonnement de son tenant expire dans exactement 7, 3 ou 1 jour(s).
 *
 * Cron : chaque heure pile. Pour chaque admin abonné, on ne notifie que si
 * l'heure locale de son tenant est 8h — assure un envoi par jour maximum,
 * à une heure raisonnable en local, quel que soit le fuseau du client.
 *
 * Redirect au clic : /subscription (page de renouvellement).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AbonnementExpireScheduler {

    private static final int HEURE_RAPPEL = 8;

    private final UserPushSubscriptionRepository pushSubRepository;
    private final UserNotificationPreferenceService preferenceService;
    private final UserPushNotificationService pushService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void envoyerRappelsExpiration() {
        List<UserEntity> users = pushSubRepository.findDistinctSubscribedUsers();
        if (users.isEmpty()) return;

        int envoyes = 0;
        for (UserEntity user : users) {
            try {
                if (envoyerSiEligible(user)) envoyes++;
            } catch (Exception e) {
                log.warn("[ABONNEMENT_EXPIRE] Erreur pour user={} : {}", user.getEmail(), e.getMessage());
            }
        }
        if (envoyes > 0) log.info("[ABONNEMENT_EXPIRE] {} rappel(s) envoyé(s) ce tick", envoyes);
    }

    private boolean envoyerSiEligible(UserEntity user) {
        if (user.getRole() != UserEntity.Role.ADMIN) return false;
        TenantEntity tenant = user.getTenant();
        if (tenant == null) return false;
        if (tenant.getDateExpiration() == null) return false;
        if (Boolean.TRUE.equals(tenant.getDeleted()) || !Boolean.TRUE.equals(tenant.getActif())) return false;

        ZoneId zone = resolveZone(tenant.getTimezone());
        if (ZonedDateTime.now(zone).getHour() != HEURE_RAPPEL) return false;

        LocalDate today = LocalDate.now(zone);
        LocalDateTime expiration = tenant.getDateExpiration();
        long jours = ChronoUnit.DAYS.between(today, expiration.toLocalDate());

        // Match strict : 7, 3 ou 1 jour(s) — pas d'intervalle
        UserNotificationType type;
        String titre;
        String corps;
        if (jours == 7) {
            type = UserNotificationType.ABONNEMENT_EXPIRE_7J;
            titre = "Ton abonnement expire dans 7 jours";
            corps = "Pense à renouveler pour continuer à utiliser EasyStock sans interruption.";
        } else if (jours == 3) {
            type = UserNotificationType.ABONNEMENT_EXPIRE_3J;
            titre = "Ton abonnement expire dans 3 jours";
            corps = "Renouvelle maintenant pour éviter le blocage de l'accès à ta boutique.";
        } else if (jours == 1) {
            type = UserNotificationType.ABONNEMENT_EXPIRE_1J;
            titre = "Ton abonnement expire demain";
            corps = "Dernier rappel : renouvelle aujourd'hui pour ne pas perdre l'accès demain.";
        } else {
            return false;
        }

        if (!preferenceService.isEnabled(user, type)) return false;
        pushService.notifyUser(user, type, titre, corps, "/subscription");
        return true;
    }

    private ZoneId resolveZone(String tz) {
        if (tz == null || tz.isBlank()) return ZoneId.of("Africa/Dakar");
        try {
            return ZoneId.of(tz);
        } catch (DateTimeParseException | java.time.zone.ZoneRulesException e) {
            return ZoneId.of("Africa/Dakar");
        }
    }
}
