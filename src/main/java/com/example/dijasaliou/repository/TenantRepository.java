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
     * Recherche paginée par nom d'entreprise ou email admin — pour SuperAdmin
     */
    @Query("SELECT DISTINCT t FROM TenantEntity t LEFT JOIN UserEntity u ON u.tenant = t AND u.role = 'ADMIN' AND u.deleted = false " +
           "WHERE t.deleted = false AND " +
           "(:search IS NULL OR LOWER(t.nomEntreprise) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR t.numeroTelephone LIKE CONCAT('%', :search, '%'))")
    Page<TenantEntity> findByDeletedFalseWithSearch(@Param("search") String search, Pageable pageable);

    /**
     * Liste complète sans pagination — pour les stats (exclut les tenants supprimés)
     */
    List<TenantEntity> findByDeletedFalse();

    List<TenantEntity> findByDeletedTrue();

    long countByEssaiUtiliseTrue();

    long countByEssaiUtiliseTrueAndPlanNot(TenantEntity.Plan plan);

    /**
     * Trouve les tenants dont l'essai BUSINESS est expiré (> DUREE_ESSAI_JOURS)
     * et qui n'ont pas encore payé (essaiUtilise=false, pas supprimés)
     * Utilisé par CleanupService pour rétrograder vers GRATUIT chaque nuit.
     */
    @Query("SELECT t FROM TenantEntity t WHERE t.plan = :plan AND t.essaiUtilise = false AND t.deleted = false AND t.dateDebutEssai < :cutoff")
    List<TenantEntity> findExpiredTrials(@Param("plan") TenantEntity.Plan plan, @Param("cutoff") LocalDateTime cutoff);

    /**
     * Tenants actifs (non supprimés, non suspendus) dont l'abonnement expire
     * dans une fenêtre de temps donnée. Utilisé pour notifier le super admin
     * X jours avant l'expiration.
     */
    @Query("SELECT t FROM TenantEntity t WHERE t.deleted = false AND t.actif = true " +
           "AND t.dateExpiration IS NOT NULL " +
           "AND t.dateExpiration BETWEEN :debut AND :fin")
    List<TenantEntity> findExpiringBetween(@Param("debut") LocalDateTime debut,
                                            @Param("fin") LocalDateTime fin);

    /**
     * Tenants dont AU MOINS UN utilisateur s'est connecté après :seuil.
     * Utilisé pour les filtres d'activité (aujourd'hui / semaine / mois).
     */
    @Query("SELECT DISTINCT t FROM TenantEntity t " +
           "JOIN UserEntity u ON u.tenant = t AND u.deleted = false " +
           "WHERE t.deleted = false AND u.derniereConnexion >= :seuil")
    List<TenantEntity> findActiveSince(@Param("seuil") LocalDateTime seuil);

    /**
     * Tenants dont AUCUN utilisateur ne s'est connecté depuis :seuil
     * (ou jamais connecté du tout). Filtre "inactifs depuis X jours".
     */
    @Query("SELECT t FROM TenantEntity t WHERE t.deleted = false AND t.id NOT IN (" +
           "  SELECT DISTINCT t2.id FROM TenantEntity t2 " +
           "  JOIN UserEntity u ON u.tenant = t2 AND u.deleted = false " +
           "  WHERE u.derniereConnexion >= :seuil" +
           ")")
    List<TenantEntity> findInactiveSince(@Param("seuil") LocalDateTime seuil);

    /**
     * Comptes "fantômes" : créés AVANT :seuilCreation et dont AUCUN utilisateur
     * ne s'est JAMAIS connecté (derniereConnexion = null pour tous les users).
     */
    @Query("SELECT t FROM TenantEntity t WHERE t.deleted = false " +
           "AND t.dateCreation < :seuilCreation " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM UserEntity u WHERE u.tenant = t AND u.deleted = false " +
           "  AND u.derniereConnexion IS NOT NULL" +
           ")")
    List<TenantEntity> findFantomes(@Param("seuilCreation") LocalDateTime seuilCreation);
}
