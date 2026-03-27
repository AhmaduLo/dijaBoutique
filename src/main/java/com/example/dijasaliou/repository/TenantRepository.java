package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des Tenants (Entreprises)
 *
 * MULTI-TENANT : Ce repository gère les entreprises clientes
 * Pas de filtre tenant ici car TenantEntity est la table maître
 */
@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, Long> {

    /**
     * Trouver un tenant par son UUID
     * CRITIQUE : Utilisé pour valider les tokens JWT
     *
     * @param tenantUuid UUID du tenant
     * @return Tenant ou Optional.empty()
     */
    Optional<TenantEntity> findByTenantUuid(String tenantUuid);

    /**
     * Vérifier si un UUID de tenant existe
     *
     * @param tenantUuid UUID du tenant
     * @return true si existe
     */
    boolean existsByTenantUuid(String tenantUuid);

    /**
     * Trouver un tenant par nom d'entreprise
     * Utile pour éviter les doublons
     *
     * @param nomEntreprise Nom de l'entreprise
     * @return Tenant ou Optional.empty()
     */
    Optional<TenantEntity> findByNomEntreprise(String nomEntreprise);

    /**
     * Vérifier si un nom d'entreprise existe déjà
     *
     * @param nomEntreprise Nom de l'entreprise
     * @return true si existe
     */
    boolean existsByNomEntreprise(String nomEntreprise);

    /**
     * Trouver un tenant par numéro de téléphone
     *
     * @param numeroTelephone Numéro de téléphone
     * @return Tenant ou Optional.empty()
     */
    Optional<TenantEntity> findByNumeroTelephone(String numeroTelephone);

    /**
     * Version paginée de findAll() — pour SuperAdmin (exclut les tenants supprimés)
     */
    Page<TenantEntity> findByDeletedFalse(Pageable pageable);

    /**
     * Liste complète sans pagination — pour les stats (exclut les tenants supprimés)
     */
    List<TenantEntity> findByDeletedFalse();

    List<TenantEntity> findByDeletedTrue();

    long countByEssaiUtiliseTrue();

    long countByEssaiUtiliseTrueAndPlanNot(TenantEntity.Plan plan);

    /**
     * Trouve les tenants dont l'essai BUSINESS est expiré (> 14 jours)
     * et qui n'ont pas encore payé (essaiUtilise=false, pas supprimés)
     * Utilisé par CleanupService pour rétrograder vers GRATUIT chaque nuit.
     */
    @Query("SELECT t FROM TenantEntity t WHERE t.plan = :plan AND t.essaiUtilise = false AND t.deleted = false AND t.dateDebutEssai < :cutoff")
    List<TenantEntity> findExpiredTrials(@Param("plan") TenantEntity.Plan plan, @Param("cutoff") LocalDateTime cutoff);
}
