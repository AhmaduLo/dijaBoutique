package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ResumeQuotidienConfig;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.UserNotificationType;
import com.example.dijasaliou.repository.PaiementCreditRepository;
import com.example.dijasaliou.repository.UserPushSubscriptionRepository;
import com.example.dijasaliou.repository.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Envoi automatique du résumé quotidien (CA + nombre de ventes du jour) via push
 * aux utilisateurs qui l'ont activé, à l'heure qu'ils ont choisie dans leurs préférences.
 *
 * Le cron tourne toutes les heures pile. Pour chaque user abonné (au moins un appareil),
 * on vérifie :
 *   1. Role ADMIN (seul destinataire en v1)
 *   2. Pref RESUME_QUOTIDIEN activée (défaut = true)
 *   3. L'heure courante dans le fuseau du tenant == config.heure()
 *
 * Si les 3 conditions sont remplies : on calcule les stats du jour et on envoie le push.
 * Redirect au clic : /dashboard.
 *
 * Idempotence : le cron fire une seule fois par heure pile → chaque user peut recevoir
 * au maximum une notif par jour, à l'heure qu'il a choisie.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeQuotidienScheduler {

    private final UserPushSubscriptionRepository pushSubRepository;
    private final UserNotificationPreferenceService preferenceService;
    private final UserPushNotificationService pushService;
    private final VenteRepository venteRepository;
    private final PaiementCreditRepository paiementCreditRepository;

    /**
     * Toutes les heures pile (0 s, 0 min).
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void envoyerResumesQuotidiens() {
        List<UserEntity> users = pushSubRepository.findDistinctSubscribedUsers();
        if (users.isEmpty()) return;

        int envoyes = 0;
        for (UserEntity user : users) {
            try {
                if (envoyerSiEligible(user)) envoyes++;
            } catch (Exception e) {
                log.warn("[RESUME_QUOTIDIEN] Erreur pour user={} : {}",
                        user.getEmail(), e.getMessage());
            }
        }
        if (envoyes > 0) {
            log.info("[RESUME_QUOTIDIEN] {} résumé(s) envoyé(s) ce tick horaire", envoyes);
        }
    }

    private boolean envoyerSiEligible(UserEntity user) {
        // 1. Uniquement les ADMIN en v1 (les gérants/vendeurs ne reçoivent pas)
        if (user.getRole() != UserEntity.Role.ADMIN) return false;

        TenantEntity tenant = user.getTenant();
        if (tenant == null) return false;

        // 2. Filtre préférences
        if (!preferenceService.isEnabled(user, UserNotificationType.RESUME_QUOTIDIEN)) return false;

        // 3. Heure locale du tenant vs config
        ZoneId zone = resolveZone(tenant.getTimezone());
        int heureCourante = ZonedDateTime.now(zone).getHour();
        ResumeQuotidienConfig config = preferenceService.getResumeQuotidienConfig(user);
        if (config.heure() != heureCourante) return false;

        // Calcul des stats du jour (dans le fuseau du tenant)
        LocalDate today = LocalDate.now(zone);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        String tenantUuid = tenant.getTenantUuid();

        BigDecimal caNonCredit = venteRepository.sumChiffreAffairesNonCreditPeriode(start, end, tenantUuid);
        if (caNonCredit == null) caNonCredit = BigDecimal.ZERO;

        BigDecimal caPaiementsCredit = paiementCreditRepository.sumMontantPayeBetweenAndTenant(today, today, tenantUuid);
        if (caPaiementsCredit == null) caPaiementsCredit = BigDecimal.ZERO;

        BigDecimal ca = caNonCredit.add(caPaiementsCredit);
        long nbVentes = venteRepository.countVentesNonCreditPeriode(start, end, tenantUuid);

        String title = "Résumé du jour — " + tenant.getNomEntreprise();
        String body = buildBody(nbVentes, ca);
        pushService.notifyUser(user, UserNotificationType.RESUME_QUOTIDIEN, title, body, "/dashboard");

        log.debug("[RESUME_QUOTIDIEN] Envoyé à {} : {} ventes / {} CFA",
                user.getEmail(), nbVentes, ca);
        return true;
    }

    private ZoneId resolveZone(String tz) {
        if (tz == null || tz.isBlank()) return ZoneId.of("Africa/Dakar");
        try {
            return ZoneId.of(tz);
        } catch (DateTimeParseException | java.time.zone.ZoneRulesException e) {
            log.warn("[RESUME_QUOTIDIEN] Timezone invalide '{}', fallback Africa/Dakar", tz);
            return ZoneId.of("Africa/Dakar");
        }
    }

    private String buildBody(long nbVentes, BigDecimal ca) {
        // Ex : "12 ventes · 87 500 CFA de CA"
        // Format entier avec espace comme séparateur de milliers (français).
        String caStr = String.format("%,d", ca.longValue()).replace(',', ' ');
        String ventesLabel = nbVentes <= 1 ? " vente" : " ventes";
        return nbVentes + ventesLabel + " · " + caStr + " CFA de CA";
    }
}
