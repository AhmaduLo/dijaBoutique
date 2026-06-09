package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Préférence de notification d'un super admin pour un type donné.
 * Unique par (user, type). Absent = on applique le défaut de NotificationType.
 */
@Entity
@Table(name = "super_admin_notification_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type_notification"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_notification", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
