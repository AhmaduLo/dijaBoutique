package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.PaiementCreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.dijasaliou.entity.PaiementCreditEntity.ModePaiement;
import com.example.dijasaliou.entity.TenantEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaiementCreditRepository extends JpaRepository<PaiementCreditEntity, String> {

    List<PaiementCreditEntity> findByCreditOrderByCreatedDateDesc(CreditClientEntity credit);

    /**
     * Agrège les remboursements de crédits par mode de paiement sur une période.
     * Retourne Object[] : [modePaiement, count, sum(montantPaye)]
     */
    @Query("SELECT p.modePaiement, COUNT(p), COALESCE(SUM(p.montantPaye), 0) " +
           "FROM PaiementCreditEntity p " +
           "WHERE p.datePaiement BETWEEN :debut AND :fin " +
           "AND p.credit.tenant.tenantUuid = :tenantUuid " +
           "GROUP BY p.modePaiement")
    List<Object[]> sumParModeEtPeriode(@Param("debut") LocalDate debut,
                                       @Param("fin") LocalDate fin,
                                       @Param("tenantUuid") String tenantUuid);

    /**
     * Somme des remboursements de crédits pour un mode de paiement donné,
     * entre deux dates (au sens createdDate — instant précis du paiement).
     * Utilisé par le module Caisse pour intégrer les remboursements aux soldes.
     */
    @Query("""
            SELECT COALESCE(SUM(p.montantPaye), 0)
            FROM PaiementCreditEntity p
            WHERE p.credit.tenant = :tenant
              AND p.modePaiement = :mode
              AND p.createdDate >= :debut
              AND p.createdDate <= :fin
            """)
    BigDecimal sumByModeBetween(@Param("tenant") TenantEntity tenant,
                                @Param("mode") ModePaiement mode,
                                @Param("debut") LocalDateTime debut,
                                @Param("fin") LocalDateTime fin);
}
