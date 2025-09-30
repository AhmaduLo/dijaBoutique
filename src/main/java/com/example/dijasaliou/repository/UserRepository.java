package com.example.dijasaliou.repository;

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
}