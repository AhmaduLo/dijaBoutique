package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.PaiementSuperAdminEntity;
import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaiementSuperAdminRepository extends JpaRepository<PaiementSuperAdminEntity, Long> {

    List<PaiementSuperAdminEntity> findByTenantOrderByDatePaiementDesc(TenantEntity tenant);

    /**
     * Revenus mensuels agrégés — pour le dashboard super admin.
     * Retourne : [mois (YYYY-MM), total_montant, nb_paiements]
     */
    @Query(value = """
            SELECT DATE_FORMAT(date_paiement, '%Y-%m') AS mois,
                   SUM(montant)                        AS total,
                   COUNT(*)                            AS nb_paiements
            FROM paiements_super_admin
            GROUP BY DATE_FORMAT(date_paiement, '%Y-%m')
            ORDER BY mois DESC
            """, nativeQuery = true)
    List<Object[]> findRevenusMenuels();
}
