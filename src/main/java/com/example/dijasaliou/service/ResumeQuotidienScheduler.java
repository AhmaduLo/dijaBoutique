package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ResumeHebdoConfig;
import com.example.dijasaliou.dto.ResumeMensuelConfig;
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
 * Envoi automatique des résumés d'activité par push :
 *   - RESUME_QUOTIDIEN  : chaque jour à l'heure choisie
 *   - RESUME_HEBDO      : chaque semaine au jour + heure choisis (défaut lundi 8h)
 *   - RESUME_MENSUEL    : chaque mois au jour du mois + heure choisis (défaut 1er à 8h)
 *
 * Le cron tourne toutes les heures pile. Pour chaque user abonné on vérifie
 * les 3 types dans l'ordre — un même tick peut donc envoyer plusieurs résumés
 * si un cas particulier tombe (par ex. le 1er du mois qui est aussi un lundi
 * à l'heure du quotidien).
 *
 * Timezone : chaque calcul (période couverte, heure/jour de déclenchement)
 * utilise le fuseau du tenant. Fallback Africa/Dakar si absent.
 *
 * Redirect au clic : /dashboard (pour le résumé) ou /rapports (à confirmer plus tard).
 *
 * Le nom de la classe est resté "ResumeQuotidienScheduler" pour compat historique —
 * elle gère désormais les trois périodicités.
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

    /** Toutes les heures pile (0 s, 0 min). */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void envoyerResumes() {
        List<UserEntity> users = pushSubRepository.findDistinctSubscribedUsers();
        if (users.isEmpty()) return;

        int envoyes = 0;
        for (UserEntity user : users) {
            try {
                if (user.getRole() != UserEntity.Role.ADMIN) continue;
                TenantEntity tenant = user.getTenant();
                if (tenant == null) continue;

                ZoneId zone = resolveZone(tenant.getTimezone());
                ZonedDateTime nowLocal = ZonedDateTime.now(zone);

                if (tryEnvoyerQuotidien(user, tenant, zone, nowLocal)) envoyes++;
                if (tryEnvoyerHebdo(user, tenant, zone, nowLocal))     envoyes++;
                if (tryEnvoyerMensuel(user, tenant, zone, nowLocal))   envoyes++;
            } catch (Exception e) {
                log.warn("[RESUME] Erreur pour user={} : {}", user.getEmail(), e.getMessage());
            }
        }
        if (envoyes > 0) log.info("[RESUME] {} résumé(s) envoyé(s) ce tick horaire", envoyes);
    }

    private boolean tryEnvoyerQuotidien(UserEntity user, TenantEntity tenant, ZoneId zone, ZonedDateTime nowLocal) {
        if (!preferenceService.isEnabled(user, UserNotificationType.RESUME_QUOTIDIEN)) return false;
        ResumeQuotidienConfig cfg = preferenceService.getResumeQuotidienConfig(user);
        if (cfg.heure() != nowLocal.getHour()) return false;

        LocalDate today = nowLocal.toLocalDate();
        Stats s = computeStats(tenant, today.atStartOfDay(), today.atTime(LocalTime.MAX), today, today);
        String title = "Résumé du jour — " + tenant.getNomEntreprise();
        pushService.notifyUser(user, UserNotificationType.RESUME_QUOTIDIEN, title,
                buildBody(s.nbVentes, s.ca), "/dashboard");
        return true;
    }

    private boolean tryEnvoyerHebdo(UserEntity user, TenantEntity tenant, ZoneId zone, ZonedDateTime nowLocal) {
        if (!preferenceService.isEnabled(user, UserNotificationType.RESUME_HEBDO)) return false;
        ResumeHebdoConfig cfg = preferenceService.getResumeHebdoConfig(user);
        if (cfg.jour() != nowLocal.getDayOfWeek()) return false;
        if (cfg.heure() != nowLocal.getHour()) return false;

        // Résumé sur les 7 jours précédents (semaine glissante), du J-7 à J-1 inclus.
        LocalDate today = nowLocal.toLocalDate();
        LocalDate debut = today.minusDays(7);
        LocalDate fin = today.minusDays(1);
        Stats s = computeStats(tenant, debut.atStartOfDay(), fin.atTime(LocalTime.MAX), debut, fin);
        String title = "Résumé de la semaine — " + tenant.getNomEntreprise();
        pushService.notifyUser(user, UserNotificationType.RESUME_HEBDO, title,
                buildBody(s.nbVentes, s.ca), "/rapports");
        return true;
    }

    private boolean tryEnvoyerMensuel(UserEntity user, TenantEntity tenant, ZoneId zone, ZonedDateTime nowLocal) {
        if (!preferenceService.isEnabled(user, UserNotificationType.RESUME_MENSUEL)) return false;
        ResumeMensuelConfig cfg = preferenceService.getResumeMensuelConfig(user);
        if (cfg.jourDuMois() != nowLocal.getDayOfMonth()) return false;
        if (cfg.heure() != nowLocal.getHour()) return false;

        // Résumé sur le mois calendaire précédent (du 1er au dernier jour du mois M-1).
        LocalDate today = nowLocal.toLocalDate();
        LocalDate premierJourMoisPrecedent = today.minusMonths(1).withDayOfMonth(1);
        LocalDate dernierJourMoisPrecedent = premierJourMoisPrecedent
                .withDayOfMonth(premierJourMoisPrecedent.lengthOfMonth());
        Stats s = computeStats(tenant,
                premierJourMoisPrecedent.atStartOfDay(),
                dernierJourMoisPrecedent.atTime(LocalTime.MAX),
                premierJourMoisPrecedent, dernierJourMoisPrecedent);
        String title = "Bilan mensuel — " + tenant.getNomEntreprise();
        pushService.notifyUser(user, UserNotificationType.RESUME_MENSUEL, title,
                buildBody(s.nbVentes, s.ca), "/rapports");
        return true;
    }

    /**
     * Calcule CA cash-basis + nombre de ventes sur une période.
     * Les paramètres LocalDateTime et LocalDate sont fournis séparément car
     * les deux repos utilisent des types différents (LocalDateTime pour ventes,
     * LocalDate pour paiements crédit).
     */
    private Stats computeStats(TenantEntity tenant, LocalDateTime debutDt, LocalDateTime finDt,
                                LocalDate debutD, LocalDate finD) {
        String tenantUuid = tenant.getTenantUuid();

        BigDecimal caNonCredit = venteRepository.sumChiffreAffairesNonCreditPeriode(debutDt, finDt, tenantUuid);
        if (caNonCredit == null) caNonCredit = BigDecimal.ZERO;
        BigDecimal caPaiementsCredit = paiementCreditRepository.sumMontantPayeBetweenAndTenant(debutD, finD, tenantUuid);
        if (caPaiementsCredit == null) caPaiementsCredit = BigDecimal.ZERO;

        long nbVentes = venteRepository.countVentesNonCreditPeriode(debutDt, finDt, tenantUuid);
        return new Stats(nbVentes, caNonCredit.add(caPaiementsCredit));
    }

    private ZoneId resolveZone(String tz) {
        if (tz == null || tz.isBlank()) return ZoneId.of("Africa/Dakar");
        try {
            return ZoneId.of(tz);
        } catch (DateTimeParseException | java.time.zone.ZoneRulesException e) {
            log.warn("[RESUME] Timezone invalide '{}', fallback Africa/Dakar", tz);
            return ZoneId.of("Africa/Dakar");
        }
    }

    private String buildBody(long nbVentes, BigDecimal ca) {
        String caStr = String.format("%,d", ca.longValue()).replace(',', ' ');
        String ventesLabel = nbVentes <= 1 ? " vente" : " ventes";
        return nbVentes + ventesLabel + " · " + caStr + " CFA de CA";
    }

    private record Stats(long nbVentes, BigDecimal ca) {}
}
