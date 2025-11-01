package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entité pour stocker les tokens de réinitialisation de mot de passe
 *
 * Processus :
 * 1. L'utilisateur demande une réinitialisation -> Un token unique est généré et stocké
 * 2. Un email avec le lien de réinitialisation est envoyé
 * 3. L'utilisateur clique sur le lien -> Le token est vérifié
 * 4. Si valide, l'utilisateur peut définir un nouveau mot de passe
 * 5. Le token est supprimé après utilisation ou expiration
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Token unique généré (UUID)
     * Sera envoyé par email à l'utilisateur
     */
    @Column(name = "token", nullable = false, unique = true, length = 100)
    private String token;

    /**
     * Utilisateur qui a demandé la réinitialisation
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * Date d'expiration du token
     * Par défaut : 1 heure après la création
     */
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    /**
     * Date de création du token
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Indique si le token a déjà été utilisé
     */
    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    /**
     * Vérifie si le token est encore valide
     * Un token est valide si :
     * - Il n'a pas été utilisé
     * - Il n'a pas expiré
     */
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiryDate);
    }

    /**
     * Marque le token comme utilisé
     */
    public void markAsUsed() {
        this.used = true;
    }

    /**
     * Callback JPA : Initialise la date de création avant la persistance
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiryDate == null) {
            // Par défaut : expire 1 heure après la création
            expiryDate = createdAt.plusHours(1);
        }
    }
}
