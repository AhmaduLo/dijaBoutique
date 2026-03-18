package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité Facture — historique des paiements d'abonnement
 *
 * Créée automatiquement lors de :
 * - Changement de plan par le SUPER_ADMIN (statut MANUELLE)
 * - Confirmation de paiement Wave (statut PAYEE)
 *
 * Les données entreprise sont snapshotées au moment de la création
 * pour que la facture reste valide même si le client modifie ses infos.
 */
@Entity
@Table(name = "factures", indexes = {
    @Index(name = "idx_facture_tenant", columnList = "tenant_id"),
    @Index(name = "idx_facture_numero", columnList = "numero_facture", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numéro unique de facture : FAC-YYYYMM-XXXX */
    @Column(name = "numero_facture", nullable = false, unique = true, length = 30)
    private String numeroFacture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    // ==================== SNAPSHOT CLIENT (au moment de la facture) ====================

    @Column(name = "nom_entreprise", length = 150)
    private String nomEntreprise;

    @Column(name = "adresse_client", length = 255)
    private String adresse;

    @Column(name = "ville_client", length = 100)
    private String ville;

    @Column(name = "pays_client", length = 100)
    private String pays;

    @Column(name = "ninea_siret_client", length = 50)
    private String nineaSiret;

    @Column(name = "admin_email", length = 150)
    private String adminEmail;

    @Column(name = "admin_nom", length = 100)
    private String adminNom;

    @Column(name = "admin_prenom", length = 100)
    private String adminPrenom;

    @Column(name = "admin_telephone", length = 30)
    private String adminTelephone;

    // ==================== DÉTAILS FACTURATION ====================

    /** Plan souscrit : BASIC, PREMIUM, ENTREPRISE */
    @Column(name = "plan", nullable = false, length = 30)
    private String plan;

    @Column(name = "montant_cfa")
    private Double montantCFA;

    @Column(name = "montant_euro")
    private Double montantEuro;

    @Column(name = "date_facture", nullable = false)
    private LocalDateTime dateFacture;

    @Column(name = "date_debut_periode")
    private LocalDateTime dateDebutPeriode;

    @Column(name = "date_fin_periode")
    private LocalDateTime dateFinPeriode;

    /** PAYEE = paiement Wave | MANUELLE = ajustée par SUPER_ADMIN */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutFacture statut = StatutFacture.MANUELLE;

    /** ID transaction Wave (null si plan modifié manuellement) */
    @Column(name = "wave_transaction_id", length = 100)
    private String waveTransactionId;

    /** Indique si la facture a été envoyée par email */
    @Column(name = "email_envoye")
    @Builder.Default
    private Boolean emailEnvoye = false;

    @Column(name = "date_envoi_email")
    private LocalDateTime dateEnvoiEmail;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    // ==================== ENUM ====================

    public enum StatutFacture {
        PAYEE,     // Paiement Wave confirmé
        MANUELLE,  // Plan ajusté manuellement par SUPER_ADMIN
        ANNULEE    // Facture annulée
    }
}
