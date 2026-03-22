package com.example.dijasaliou.service;

import com.example.dijasaliou.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service de nettoyage automatique des données expirées.
 *
 * Évite l'accumulation en BDD de tokens inutilisés.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CleanupService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Supprime les tokens de reset expirés — s'exécute toutes les nuits à 2h00.
     * Cron : seconde minute heure jour-du-mois mois jour-de-la-semaine
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void supprimerTokensExpires() {
        passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        passwordResetTokenRepository.deleteUsedTokens();
        log.info("[CLEANUP] Tokens de reset expirés/utilisés supprimés");
    }
}
