package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.PaiementCreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
}
