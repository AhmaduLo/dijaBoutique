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
     * entre deux dates au sens {@code datePaiement} (date métier saisie par
     * l'utilisateur, peut être antédatée).
     * Utilisé par le module Caisse pour intégrer les remboursements aux soldes.
     */
    @Query("""
            SELECT COALESCE(SUM(p.montantPaye), 0)
            FROM PaiementCreditEntity p
            WHERE p.credit.tenant = :tenant
              AND p.modePaiement = :mode
              AND p.datePaiement >= :debut
              AND p.datePaiement <= :fin
            """)
    BigDecimal sumByModeBetween(@Param("tenant") TenantEntity tenant,
                                @Param("mode") ModePaiement mode,
                                @Param("debut") LocalDate debut,
                                @Param("fin") LocalDate fin);

    /** Optimisation caisse : total paiements crédit GROUPÉ par mode en une query. */
    @Query("""
            SELECT p.modePaiement, COALESCE(SUM(p.montantPaye), 0)
            FROM PaiementCreditEntity p
            WHERE p.credit.tenant = :tenant
              AND p.datePaiement >= :debut
              AND p.datePaiement <= :fin
            GROUP BY p.modePaiement
            """)
    java.util.List<Object[]> sumByModeGrouped(@Param("tenant") TenantEntity tenant,
                                              @Param("debut") LocalDate debut,
                                              @Param("fin") LocalDate fin);

    /**
     * Total des paiements crédit reçus sur une période (tous modes confondus).
     * Sert au CA en comptabilité de caisse : un paiement crédit est de l'argent vraiment encaissé.
     */
    @Query("""
            SELECT COALESCE(SUM(p.montantPaye), 0)
            FROM PaiementCreditEntity p
            WHERE p.credit.tenant.tenantUuid = :tenantUuid
              AND p.datePaiement >= :debut
              AND p.datePaiement <= :fin
            """)
    BigDecimal sumMontantPayeBetweenAndTenant(@Param("debut") LocalDate debut,
                                              @Param("fin") LocalDate fin,
                                              @Param("tenantUuid") String tenantUuid);

    /**
     * Compte le nombre de paiements crédit reçus sur une période.
     * Sert à compter les "ventes encaissées via crédit" pour les stats.
     */
    @Query("""
            SELECT COUNT(p)
            FROM PaiementCreditEntity p
            WHERE p.credit.tenant.tenantUuid = :tenantUuid
              AND p.datePaiement >= :debut
              AND p.datePaiement <= :fin
            """)
    long countByPeriodeAndTenant(@Param("debut") LocalDate debut,
                                 @Param("fin") LocalDate fin,
                                 @Param("tenantUuid") String tenantUuid);

    /**
     * Paiements crédit d'une période, avec la vente associée chargée (pour calcul du coût FIFO prorata).
     * Retourne uniquement les paiements liés à une vente (credit.vente != null).
     */
    @Query("""
            SELECT p
            FROM PaiementCreditEntity p
            JOIN FETCH p.credit c
            JOIN FETCH c.vente v
            WHERE c.tenant.tenantUuid = :tenantUuid
              AND p.datePaiement >= :debut
              AND p.datePaiement <= :fin
            """)
    List<PaiementCreditEntity> findPaiementsAvecVenteBetween(@Param("debut") LocalDate debut,
                                                             @Param("fin") LocalDate fin,
                                                             @Param("tenantUuid") String tenantUuid);
}
