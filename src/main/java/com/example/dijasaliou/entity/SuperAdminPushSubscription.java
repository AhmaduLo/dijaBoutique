package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Subscription Web Push d'un super admin.
 * Un super admin peut avoir plusieurs subscriptions (un par appareil).
 *
 * Pas multi-tenant : les super admins n'ont pas de tenant.
 */
@Entity
@Table(name = "super_admin_push_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminPushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

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
