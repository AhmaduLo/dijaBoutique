package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.CompteCaisse;
import com.example.dijasaliou.entity.MouvementCaisseManuelEntity;
import com.example.dijasaliou.entity.MouvementCaisseManuelEntity.TypeMouvement;
import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementCaisseManuelRepository extends JpaRepository<MouvementCaisseManuelEntity, String> {

    /** Tous les mouvements manuels d'un tenant entre deux dates. */
    @Query("""
            SELECT m FROM MouvementCaisseManuelEntity m
            WHERE m.tenant = :tenant
              AND m.dateMouvement >= :debut
              AND m.dateMouvement <= :fin
            ORDER BY m.dateMouvement DESC, m.id DESC
            """)
    List<MouvementCaisseManuelEntity> findByTenantBetween(@Param("tenant") TenantEntity tenant,
                                                          @Param("debut") LocalDateTime debut,
                                                          @Param("fin") LocalDateTime fin);

    /** Somme des mouvements d'un type et d'un compte entre deux dates. */
    @Query("""
            SELECT COALESCE(SUM(m.montant), 0) FROM MouvementCaisseManuelEntity m
            WHERE m.tenant = :tenant
              AND m.compte = :compte
              AND m.typeMouvement = :type
              AND m.dateMouvement >= :debut
              AND m.dateMouvement <= :fin
            """)
    BigDecimal sumByCompteAndTypeBetween(@Param("tenant") TenantEntity tenant,
                                         @Param("compte") CompteCaisse compte,
                                         @Param("type") TypeMouvement type,
                                         @Param("debut") LocalDateTime debut,
                                         @Param("fin") LocalDateTime fin);

    /** Optimisation caisse : mouvements GROUPÉS par (compte, type) en une seule query. */
    @Query("""
            SELECT m.compte, m.typeMouvement, COALESCE(SUM(m.montant), 0)
            FROM MouvementCaisseManuelEntity m
            WHERE m.tenant = :tenant
              AND m.dateMouvement >= :debut
              AND m.dateMouvement <= :fin
            GROUP BY m.compte, m.typeMouvement
            """)
    java.util.List<Object[]> sumByCompteAndTypeGrouped(@Param("tenant") TenantEntity tenant,
                                                       @Param("debut") LocalDateTime debut,
                                                       @Param("fin") LocalDateTime fin);

    /** Supprime tous les mouvements manuels d'un tenant. Utilisé par DELETE /api/caisse. */
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM MouvementCaisseManuelEntity m WHERE m.tenant = :tenant")
    int deleteAllByTenant(@Param("tenant") TenantEntity tenant);
}
