package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Préférence de notification d'un utilisateur (admin d'un tenant) pour un type donné.
 *
 * Unique par (user, type). Absent = on applique le défaut de UserNotificationType.
 *
 * Le champ {@code config} accueille une configuration spécifique par type
 * sous forme de JSON (heure d'envoi pour les résumés, seuil de montant, etc.).
 * Absent en Phase 1 : on ajoute les configs par type dans les phases suivantes.
 */
@Entity
@Table(name = "user_notification_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type_notification"}),
       indexes = {
               @Index(name = "idx_user_notif_pref_user", columnList = "user_id"),
               @Index(name = "idx_user_notif_pref_tenant", columnList = "tenant_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * Redondant avec user.tenant mais permet un filtre direct sans jointure
     * (utile pour les crons qui balaient toutes les préférences d'un tenant).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_notification", nullable = false, length = 50)
    private UserNotificationType type;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /**
     * Configuration spécifique au type, sérialisée en JSON.
     * Exemple : { "heure": 20 } pour RESUME_QUOTIDIEN, { "seuilMontant": 50000 } pour SORTIE_CAISSE_IMPORTANTE.
     * Peut rester NULL — l'appelant applique alors des défauts.
     */
    @Column(name = "config", columnDefinition = "TEXT")
    private String config;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
