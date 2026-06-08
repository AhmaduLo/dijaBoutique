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

    boolean existsByTenant(TenantEntity tenant);
}
