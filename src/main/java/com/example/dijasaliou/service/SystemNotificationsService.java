package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.NotificationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Notifications système au super admin :
 *  - 💾 Stockage BDD > 80%  (cron quotidien 6h)
 *  - 🔥 Pic d'erreurs serveur (cron horaire, seuil 10/h)
 *
 * Le compteur d'erreurs est incrémenté par {@link com.example.dijasaliou.exception.GlobalExceptionHandler}
 * via {@link #recordError()} et remis à zéro à chaque tick horaire.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemNotificationsService {

    private static final int SEUIL_BDD_POURCENTAGE = 80;
    private static final long BDD_MAX_MB = 1000; // Railway hobby = 1 GB
    private static final int SEUIL_ERREURS_PAR_HEURE = 10;

    private final PushNotificationService pushService;
    private final AtomicInteger erreursDansHeure = new AtomicInteger(0);

    @PersistenceContext
    private EntityManager entityManager;

    /** À appeler depuis le GlobalExceptionHandler à chaque exception serveur (5xx). */
    public void recordError() {
        erreursDansHeure.incrementAndGet();
    }

    /**
     * Tous les jours à 6h : vérifie si la BDD dépasse le seuil de remplissage.
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional(readOnly = true)
    public void verifierStockageBdd() {
        try {
            @SuppressWarnings("deprecation")
            List<Object[]> rows = entityManager.createNativeQuery(
                    "SELECT data_length, index_length FROM information_schema.tables WHERE table_schema = DATABASE()"
            ).getResultList();

            long totalBytes = 0;
            for (Object[] r : rows) {
                if (r[0] != null) totalBytes += ((Number) r[0]).longValue();
                if (r[1] != null) totalBytes += ((Number) r[1]).longValue();
            }
            long totalMB = totalBytes / (1024 * 1024);
            int pourcentage = (int) (totalMB * 100 / BDD_MAX_MB);

            if (pourcentage >= SEUIL_BDD_POURCENTAGE) {
                pushService.notify(
                        NotificationType.STOCKAGE_BDD_PLEIN,
                        "💾 Stockage BDD à " + pourcentage + "%",
                        "Base de données : " + totalMB + " MB / " + BDD_MAX_MB + " MB. Pense à upgrader Railway.",
                        "/superadmin/monitoring"
                );
                log.warn("[SYSTEM] Alerte BDD pleine envoyée ({}%)", pourcentage);
            }
        } catch (Exception e) {
            log.warn("[SYSTEM] Impossible de vérifier la taille BDD : {}", e.getMessage());
        }
    }

    /**
     * Toutes les heures pile : si plus de N erreurs dans l'heure écoulée → notif.
     * Le compteur est ensuite remis à zéro pour la fenêtre suivante.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void verifierPicErreurs() {
        int erreurs = erreursDansHeure.getAndSet(0);
        if (erreurs >= SEUIL_ERREURS_PAR_HEURE) {
            pushService.notify(
                    NotificationType.PIC_ERREURS_SERVEUR,
                    "🔥 Pic d'erreurs serveur",
                    erreurs + " erreurs dans la dernière heure (seuil : " + SEUIL_ERREURS_PAR_HEURE + ")",
                    "/superadmin/monitoring"
            );
            log.warn("[SYSTEM] Alerte pic d'erreurs envoyée ({} erreurs)", erreurs);
        }
    }
}
