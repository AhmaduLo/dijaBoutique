package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.UserNotificationType;
import com.example.dijasaliou.repository.CreditClientRepository;
import com.example.dijasaliou.repository.UserPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Rappel quotidien pour les crédits clients dépassés.
 *
 * Cron : chaque heure pile. Pour chaque admin abonné dont l'heure locale du
 * tenant vaut 8h, on compte les crédits en retard (statut != SOLDE/PERTE,
 * date_echeance < aujourd'hui) et on envoie UN push résumé s'il y en a.
 *
 * Idempotence : un seul tick horaire par jour où heure_locale = 8, donc au
 * plus un push par jour par admin. Redirect au clic : /credits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditRetardScheduler {

    private static final int HEURE_RAPPEL = 8;

    private final UserPushSubscriptionRepository pushSubRepository;
    private final UserNotificationPreferenceService preferenceService;
    private final UserPushNotificationService pushService;
    private final CreditClientRepository creditClientRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void envoyerRappelsCreditsRetard() {
        List<UserEntity> users = pushSubRepository.findDistinctSubscribedUsers();
        if (users.isEmpty()) return;

        int envoyes = 0;
        for (UserEntity user : users) {
            try {
                if (envoyerSiEligible(user)) envoyes++;
            } catch (Exception e) {
                log.warn("[CREDIT_RETARD] Erreur pour user={} : {}", user.getEmail(), e.getMessage());
            }
        }
        if (envoyes > 0) log.info("[CREDIT_RETARD] {} rappel(s) envoyé(s) ce tick", envoyes);
    }

    private boolean envoyerSiEligible(UserEntity user) {
        if (user.getRole() != UserEntity.Role.ADMIN) return false;
        TenantEntity tenant = user.getTenant();
        if (tenant == null) return false;
        if (!preferenceService.isEnabled(user, UserNotificationType.CREDIT_EN_RETARD)) return false;

        ZoneId zone = resolveZone(tenant.getTimezone());
        if (ZonedDateTime.now(zone).getHour() != HEURE_RAPPEL) return false;

        LocalDate today = LocalDate.now(zone);
        // On exclut SOLDE (déjà remboursé). Les crédits PERTE restent affichés mais
        // ne sont pas comptés dans les alertes de retard car ils ont déjà été acceptés
        // comme non recouvrables — inutile de re-notifier.
        long nbRetard = creditClientRepository.countCreditsEnRetard(
                StatutCredit.SOLDE, today, tenant.getTenantUuid());
        if (nbRetard == 0) return false;

        String title = "Crédits en retard — " + tenant.getNomEntreprise();
        String body = nbRetard + " crédit" + (nbRetard > 1 ? "s ont" : " a")
                + " dépassé sa date d'échéance. Il est temps de relancer les clients concernés.";
        pushService.notifyUser(user, UserNotificationType.CREDIT_EN_RETARD, title, body, "/credits");
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
