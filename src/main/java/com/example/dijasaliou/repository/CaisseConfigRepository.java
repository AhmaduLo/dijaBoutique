package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.CaisseConfigEntity;
import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaisseConfigRepository extends JpaRepository<CaisseConfigEntity, Long> {

    /** Récupère la config de caisse du tenant (1 seule possible). */
    Optional<CaisseConfigEntity> findByTenant(TenantEntity tenant);

    /**
     * Verrou pessimiste sur la config caisse du tenant — sérialise les
     * opérations parallèles (transferts/sorties) pour éviter le solde négatif
     * en cas de double clic ou multi-onglet.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM CaisseConfigEntity c WHERE c.tenant = :tenant")
    Optional<CaisseConfigEntity> findByTenantForUpdate(
            @org.springframework.data.repository.query.Param("tenant") TenantEntity tenant);

    boolean existsByTenant(TenantEntity tenant);

    /** Supprime la config caisse du tenant. Utilisé par DELETE /api/caisse. */
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM CaisseConfigEntity c WHERE c.tenant = :tenant")
    int deleteAllByTenant(@org.springframework.data.repository.query.Param("tenant") TenantEntity tenant);
}
