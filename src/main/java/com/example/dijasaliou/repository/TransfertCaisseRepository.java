package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.CompteCaisse;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.TransfertCaisseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransfertCaisseRepository extends JpaRepository<TransfertCaisseEntity, String> {

    /** Tous les transferts d'un tenant entre deux dates, triés par date DESC. */
    @Query("""
            SELECT t FROM TransfertCaisseEntity t
            WHERE t.tenant = :tenant
              AND t.dateTransfert >= :debut
              AND t.dateTransfert <= :fin
            ORDER BY t.dateTransfert DESC, t.id DESC
            """)
    List<TransfertCaisseEntity> findByTenantBetween(@Param("tenant") TenantEntity tenant,
                                                    @Param("debut") LocalDateTime debut,
                                                    @Param("fin") LocalDateTime fin);

    /** Somme des transferts SORTANT d'un compte entre deux dates. */
    @Query("""
            SELECT COALESCE(SUM(t.montant), 0) FROM TransfertCaisseEntity t
            WHERE t.tenant = :tenant
              AND t.compteSource = :compte
              AND t.dateTransfert >= :debut
              AND t.dateTransfert <= :fin
            """)
    BigDecimal sumSortiesByCompteBetween(@Param("tenant") TenantEntity tenant,
                                         @Param("compte") CompteCaisse compte,
                                         @Param("debut") LocalDateTime debut,
                                         @Param("fin") LocalDateTime fin);

    /** Somme des transferts ARRIVANT sur un compte entre deux dates. */
    @Query("""
            SELECT COALESCE(SUM(t.montant), 0) FROM TransfertCaisseEntity t
            WHERE t.tenant = :tenant
              AND t.compteDestination = :compte
              AND t.dateTransfert >= :debut
              AND t.dateTransfert <= :fin
            """)
    BigDecimal sumEntreesByCompteBetween(@Param("tenant") TenantEntity tenant,
                                         @Param("compte") CompteCaisse compte,
                                         @Param("debut") LocalDateTime debut,
                                         @Param("fin") LocalDateTime fin);
}
