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

    /** Tous les mouvements manuels d'un tenant depuis une date. */
    @Query("""
            SELECT m FROM MouvementCaisseManuelEntity m
            WHERE m.tenant = :tenant AND m.dateMouvement >= :debut
            ORDER BY m.dateMouvement DESC, m.id DESC
            """)
    List<MouvementCaisseManuelEntity> findByTenantSince(@Param("tenant") TenantEntity tenant,
                                                       @Param("debut") LocalDateTime debut);

    /** Somme des mouvements d'un type et d'un compte depuis une date. */
    @Query("""
            SELECT COALESCE(SUM(m.montant), 0) FROM MouvementCaisseManuelEntity m
            WHERE m.tenant = :tenant
              AND m.compte = :compte
              AND m.typeMouvement = :type
              AND m.dateMouvement >= :debut
            """)
    BigDecimal sumByCompteAndTypeSince(@Param("tenant") TenantEntity tenant,
                                       @Param("compte") CompteCaisse compte,
                                       @Param("type") TypeMouvement type,
                                       @Param("debut") LocalDateTime debut);
}
