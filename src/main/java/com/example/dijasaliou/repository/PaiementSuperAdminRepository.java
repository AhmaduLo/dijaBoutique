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

    /** Tous les paiements triés par date DESC — utilisé pour le rapport mensuel détaillé. */
    List<PaiementSuperAdminEntity> findAllByOrderByDatePaiementDesc();

    /** Compte les paiements d'un tenant — utilisé avant rétrogradation vers GRATUIT. */
    long countByTenant(TenantEntity tenant);

    /** Nombre de tenants distincts ayant au moins un paiement enregistré. */
    @Query("SELECT COUNT(DISTINCT p.tenant.id) FROM PaiementSuperAdminEntity p")
    long countDistinctTenants();
}
