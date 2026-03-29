package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Trouver un utilisateur par email
     * IMPORTANT : Optional car email est unique (0 ou 1 résultat)
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Vérifier si un email existe
     * Utile pour validation avant création
     */
    boolean existsByEmail(String email);

    /**
     * Trouver par email (ignore la casse)
     */
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    /**
     * Trouver tous les utilisateurs d'un rôle
     */
    List<UserEntity> findByRole(UserEntity.Role role);

    /**
     * Trouver par nom et prénom
     */
    Optional<UserEntity> findByNomAndPrenom(String nom, String prenom);

    /**
     * Compter le nombre d'utilisateurs pour un tenant
     * Utilisé pour vérifier les limites du plan
     */
    long countByTenant(TenantEntity tenant);

    // =============== MÉTHODES POUR SUPPRESSION LOGIQUE ===============

    /**
     * Trouver un utilisateur par email (non supprimé uniquement)
     */
    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    /**
     * Trouver tous les utilisateurs actifs (non supprimés)
     */
    List<UserEntity> findByDeletedFalse();

    /**
     * Version paginée — pour éviter de charger tous les utilisateurs en mémoire
     */
    Page<UserEntity> findByDeletedFalse(Pageable pageable);

    /**
     * Trouver tous les utilisateurs d'un rôle (non supprimés)
     */
    List<UserEntity> findByRoleAndDeletedFalse(UserEntity.Role role);

    /**
     * Compter le nombre d'utilisateurs actifs pour un tenant
     */
    long countByTenantAndDeletedFalse(TenantEntity tenant);

    /**
     * Vérifier si un email existe parmi les utilisateurs actifs
     */
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * Trouver le premier utilisateur d'un tenant avec un rôle spécifique
     * Utilisé pour trouver l'admin d'un tenant pour les notifications
     */
    Optional<UserEntity> findFirstByTenantAndRole(TenantEntity tenant, UserEntity.Role role);

    List<UserEntity> findByTenantIdAndDeletedFalse(Long tenantId);

    List<UserEntity> findByTenantId(Long tenantId);

    @Query("SELECT MAX(u.derniereConnexion) FROM UserEntity u WHERE u.tenant.id = :tenantId AND u.deleted = false")
    Optional<LocalDateTime> findDerniereActiviteByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Vérifie si un email existe globalement (tous tenants, incl. comptes supprimés).
     * Requête native pour bypasser le filtre Hibernate tenant — nécessaire pour respecter
     * la contrainte UNIQUE sur email qui est globale en base.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM utilisateurs WHERE email = :email", nativeQuery = true)
    boolean existsByEmailGlobal(@Param("email") String email);

    /**
     * Met à jour derniereConnexion sans charger l'entité complète.
     * Appelé par ActivityTrackingFilter à chaque requête authentifiée (throttlé à 5 min).
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.derniereConnexion = :now WHERE u.email = :email")
    void updateDerniereConnexion(@Param("email") String email, @Param("now") LocalDateTime now);
}