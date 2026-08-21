package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Subscription Web Push d'un utilisateur (admin d'un tenant).
 * Un utilisateur peut avoir plusieurs subscriptions (un par appareil : PC, mobile).
 *
 * Contrairement à {@link SuperAdminPushSubscription}, on stocke aussi le tenant
 * pour permettre le filtrage direct au moment d'un envoi ciblé par tenant.
 */
@Entity
@Table(name = "user_push_subscriptions", indexes = {
        @Index(name = "idx_user_push_user", columnList = "user_id"),
        @Index(name = "idx_user_push_tenant", columnList = "tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Column(name = "p256dh", nullable = false, length = 255)
    private String p256dh;

    @Column(name = "auth_key", nullable = false, length = 255)
    private String authKey;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "derniere_utilisation")
    private LocalDateTime derniereUtilisation;
}
