package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des devises.
 *
 * Une devise est soit "système" (tenant = null, partagée en lecture par tout
 * le monde), soit "personnalisée" (tenant = une boutique précise, visible
 * uniquement par elle). Les méthodes ci-dessous scopent systématiquement sur
 * "système + ce tenant", jamais sur les devises personnalisées des autres.
 */
@Repository
public interface DeviseRepository extends JpaRepository<DeviseEntity, Long> {

    /**
     * Vérifie si une devise avec ce code existe déjà (système ou globalement,
     * toutes boutiques confondues). Utilisé uniquement par le seed initial —
     * préférer {@link #existsByCodeAndTenantIsNull} / {@link #existsByCodeAndTenant}
     * partout ailleurs.
     */
    boolean existsByCode(String code);

    /**
     * Une devise système avec ce code existe-t-elle déjà ?
     */
    boolean existsByCodeAndTenantIsNull(String code);

    /**
     * Cette boutique a-t-elle déjà sa propre devise avec ce code ?
     */
    boolean existsByCodeAndTenant(String code, TenantEntity tenant);

    /**
     * Trouve une devise par son code, uniquement parmi les devises système
     * (tenant = null). Utilisé par le seed initial.
     */
    Optional<DeviseEntity> findByCode(String code);

    /**
     * Trouve une devise par son code, visible pour ce tenant (sa propre
     * version personnalisée en priorité, sinon la version système).
     */
    @Query("""
            SELECT d FROM DeviseEntity d
            WHERE d.code = :code
              AND (d.tenant = :tenant OR d.tenant IS NULL)
            ORDER BY CASE WHEN d.tenant IS NULL THEN 1 ELSE 0 END
            """)
    List<DeviseEntity> findByCodeVisiblePourTenant(@Param("code") String code, @Param("tenant") TenantEntity tenant);

    /**
     * Liste toutes les devises visibles par ce tenant : les devises système
     * (tenant = null) + les devises personnalisées de ce tenant uniquement.
     */
    @Query("SELECT d FROM DeviseEntity d WHERE d.tenant = :tenant OR d.tenant IS NULL")
    List<DeviseEntity> findVisiblesPourTenant(@Param("tenant") TenantEntity tenant);

    /**
     * Trouve la devise par défaut (mécanisme legacy — voir DeviseService).
     *
     * @return Optional contenant la devise par défaut si elle existe
     */
    Optional<DeviseEntity> findByIsDefaultTrue();

    /**
     * Vérifie si une devise est la devise par défaut
     *
     * @return true si une devise par défaut existe
     */
    boolean existsByIsDefaultTrue();
}
