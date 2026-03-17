package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Historique des alertes de stock envoyées
 *
 * Cette table permet de :
 * - Suivre quelles alertes ont été envoyées pour quel produit
 * - Éviter d'envoyer plusieurs fois la même alerte au même seuil
 * - Garder un historique des alertes pour audit
 *
 * FONCTIONNEMENT :
 * - Quand le stock atteint un seuil (15, 10, 5, 0), on envoie une alerte
 * - On enregistre l'alerte envoyée dans cette table
 * - Si le stock remonte puis redescend, on peut renvoyer une alerte
 */
@Entity
@Table(name = "stock_alert_history", indexes = {
    @Index(name = "idx_alert_produit_seuil", columnList = "nom_produit, seuil_alerte, tenant_id"),
    @Index(name = "idx_alert_tenant", columnList = "tenant_id"),
    @Index(name = "idx_alert_date", columnList = "date_envoi")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom du produit pour lequel l'alerte a été envoyée
     */
    @Column(name = "nom_produit", nullable = false, length = 100)
    private String nomProduit;

    /**
     * Seuil qui a déclenché l'alerte (15, 10, 5, ou 0)
     */
    @Column(name = "seuil_alerte", nullable = false)
    private Integer seuilAlerte;

    /**
     * Stock actuel au moment de l'alerte
     */
    @Column(name = "stock_actuel", nullable = false)
    private Integer stockActuel;

    /**
     * Email de l'admin qui a reçu l'alerte
     */
    @Column(name = "email_destinataire", nullable = false, length = 100)
    private String emailDestinataire;

    /**
     * Date et heure d'envoi de l'alerte
     */
    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    /**
     * Référence au tenant (entreprise)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_stock_alert_tenant"))
    private TenantEntity tenant;

    @PrePersist
    protected void onCreate() {
        if (this.dateEnvoi == null) {
            this.dateEnvoi = LocalDateTime.now();
        }
    }
}
