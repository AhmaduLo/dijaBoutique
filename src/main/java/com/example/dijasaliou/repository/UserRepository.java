package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}