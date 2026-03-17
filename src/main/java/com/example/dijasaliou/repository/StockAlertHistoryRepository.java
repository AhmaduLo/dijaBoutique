package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.StockAlertHistory;
import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StockAlertHistoryRepository extends JpaRepository<StockAlertHistory, Long> {

    /**
     * Vérifie si une alerte a déjà été envoyée pour ce produit à ce seuil
     * dans les dernières 24 heures
     *
     * @param nomProduit Nom du produit
     * @param seuilAlerte Seuil (15, 10, 5, ou 0)
     * @param tenant Tenant concerné
     * @param since Date à partir de laquelle chercher
     * @return true si une alerte a déjà été envoyée récemment
     */
    @Query("SELECT COUNT(h) > 0 FROM StockAlertHistory h " +
           "WHERE h.nomProduit = :nomProduit " +
           "AND h.seuilAlerte = :seuilAlerte " +
           "AND h.tenant = :tenant " +
           "AND h.dateEnvoi > :since")
    boolean existsRecentAlert(
            @Param("nomProduit") String nomProduit,
            @Param("seuilAlerte") Integer seuilAlerte,
            @Param("tenant") TenantEntity tenant,
            @Param("since") LocalDateTime since
    );

    /**
     * Récupère la dernière alerte envoyée pour un produit
     */
    Optional<StockAlertHistory> findFirstByNomProduitAndTenantOrderByDateEnvoiDesc(
            String nomProduit,
            TenantEntity tenant
    );
}
