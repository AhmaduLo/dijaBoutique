package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Paiement validé manuellement par le super admin.
 * Enregistré après confirmation WhatsApp / mobile money / cash.
 */
@Entity
@Table(name = "paiements_super_admin", indexes = {
    @Index(name = "idx_psa_tenant", columnList = "tenant_id"),
    @Index(name = "idx_psa_date",   columnList = "date_paiement"),
    @Index(name = "idx_psa_mois",   columnList = "mois_debut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementSuperAdminEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private TenantEntity.Plan plan;

    @Column(name = "montant", nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement;

    /** Format YYYY-MM, ex : "2026-04" — début de la période couverte */
    @Column(name = "mois_debut", length = 7)
    private String moisDebut;

    @Column(name = "mode_paiement", length = 50)
    private String modePaiement;

    /** MENSUEL ou ANNUEL (annuel = -15% appliqué côté frontend) */
    @Column(name = "periode", nullable = false, length = 10)
    @Builder.Default
    private String periode = "MENSUEL";

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** ID du super admin ayant validé le paiement */
    @Column(name = "valide_par")
    private Long validePar;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
