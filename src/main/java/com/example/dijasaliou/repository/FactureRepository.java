package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.FactureEntity;
import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactureRepository extends JpaRepository<FactureEntity, Long> {

    /** Toutes les factures d'un tenant, de la plus récente à la plus ancienne */
    List<FactureEntity> findByTenantOrderByDateFactureDesc(TenantEntity tenant);

    /** Nombre de factures (= nombre de paiements) d'un tenant */
    long countByTenant(TenantEntity tenant);

    /** Prochain numéro de séquence pour le mois courant */
    @Query("SELECT COUNT(f) FROM FactureEntity f WHERE f.numeroFacture LIKE :prefix%")
    long countByNumeroFactureStartingWith(String prefix);
}
