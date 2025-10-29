package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
