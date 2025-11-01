package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.PasswordResetToken;
import com.example.dijasaliou.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository pour gérer les tokens de réinitialisation de mot de passe
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Recherche un token par sa valeur
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Recherche tous les tokens d'un utilisateur
     */
    Optional<PasswordResetToken> findByUser(UserEntity user);

    /**
     * Supprime tous les tokens d'un utilisateur
     * Utile pour invalider tous les tokens précédents lors d'une nouvelle demande
     */
    void deleteByUser(UserEntity user);

    /**
     * Supprime tous les tokens expirés
     * Méthode de nettoyage à exécuter périodiquement
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(LocalDateTime now);

    /**
     * Supprime tous les tokens utilisés
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.used = true")
    void deleteUsedTokens();
}
