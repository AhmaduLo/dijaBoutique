package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Trace qu'un tenant a lu/fermé une notification.
 * Permet de ne pas réafficher une notification déjà vue.
 */
@Entity
@Table(name = "notifications_lues", indexes = {
        @Index(name = "idx_notif_lue_tenant", columnList = "tenant_id"),
        @Index(name = "idx_notif_lue_notif", columnList = "notification_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_notif_tenant", columnNames = {"notification_id", "tenant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private NotificationEntity notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Column(name = "date_lecture", nullable = false)
    @Builder.Default
    private LocalDateTime dateLecture = LocalDateTime.now();
}
