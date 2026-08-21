package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Notification in-app envoyée par le super admin.
 * Si tenantId est null → notification pour TOUS les tenants.
 * Si tenantId est renseigné → notification ciblée pour une boutique.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_tenant", columnList = "tenant_id"),
        @Index(name = "idx_notif_date", columnList = "date_envoi")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "objet", nullable = false, length = 200)
    private String objet;

    @NotBlank
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * null = notification globale (tous les tenants)
     * renseigné = notification ciblée pour un tenant
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private TenantEntity tenant;

    /** Nom de la boutique ciblée (pour affichage JSON sans charger le proxy) */
    @Transient
    private String tenantNom;

    /**
     * Filtre par plan (ex: "PRO", "BUSINESS", "GRATUIT")
     * null = tous les plans
     */
    @Size(max = 20)
    @Column(name = "filtre_plan", length = 20)
    private String filtrePlan;

    @Column(name = "canal_email", nullable = false)
    @Builder.Default
    private Boolean canalEmail = false;

    @Column(name = "canal_app", nullable = false)
    @Builder.Default
    private Boolean canalApp = true;

    @Column(name = "canal_whatsapp", nullable = false)
    @Builder.Default
    private Boolean canalWhatsapp = false;

    @Column(name = "nb_destinataires", nullable = false)
    @Builder.Default
    private Integer nbDestinataires = 0;

    @Column(name = "date_envoi", nullable = false)
    @Builder.Default
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Size(max = 100)
    @Column(name = "envoye_par", length = 100)
    private String envoyePar;
}
