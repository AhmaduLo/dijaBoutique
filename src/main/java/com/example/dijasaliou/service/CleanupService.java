package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.PasswordResetTokenRepository;
import com.example.dijasaliou.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de nettoyage automatique des données expirées.
 *
 * Évite l'accumulation en BDD de tokens inutilisés.
 * Rétrograde les essais BUSINESS expirés vers GRATUIT.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CleanupService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TenantRepository tenantRepository;
    private final TenantCacheService tenantCacheService;

    /**
     * Supprime les tokens de reset expirés — s'exécute toutes les nuits à 2h00.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void supprimerTokensExpires() {
        passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        passwordResetTokenRepository.deleteUsedTokens();
        log.info("[CLEANUP] Tokens de reset expirés/utilisés supprimés");
    }

    /**
     * Rétrograde les essais BUSINESS expirés (> 14 jours) vers GRATUIT.
     * S'exécute toutes les nuits à 3h00.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void retrograderEssaisExpires() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(14);
        List<TenantEntity> expiredTrials = tenantRepository.findExpiredTrials(
                TenantEntity.Plan.BUSINESS, cutoff);

        for (TenantEntity tenant : expiredTrials) {
            tenant.setPlan(TenantEntity.Plan.GRATUIT);
            tenantRepository.save(tenant);
            tenantCacheService.evict(tenant.getTenantUuid());
            log.info("[CLEANUP] Essai BUSINESS expiré → GRATUIT : tenant {}", tenant.getTenantUuid());
        }

        if (!expiredTrials.isEmpty()) {
            log.info("[CLEANUP] {} essai(s) BUSINESS rétrogradé(s) vers GRATUIT", expiredTrials.size());
        }
    }
}
