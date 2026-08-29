package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Représente la consommation d'un lot d'achat par une vente (méthode FIFO).
 *
 * Une vente peut puiser dans plusieurs lots d'achat si un lot s'épuise au milieu.
 * Chaque ligne de cette table = un lien (vente ↔ lot d'achat) avec la quantité consommée.
 *
 * Exemple :
 *   Lot A : 15 unités à 25 000 (10 restantes)
 *   Lot B : 10 unités à 20 000 (10 restantes)
 *   Vente de 12 unités à 32 000 →
 *     - 1 ligne : 10 unités du lot A → bénéfice = 10 × (32 000 − 25 000) = 70 000
 *     - 1 ligne : 2 unités du lot B  → bénéfice = 2  × (32 000 − 20 000) = 24 000
 *
 * Les snapshots prixAchatUnitaire et beneficeUnitaire sont figés au moment de la vente.
 */
@Entity
@Table(
        name = "vente_lot_consommation",
        indexes = {
                @Index(name = "idx_vlc_vente",   columnList = "vente_id"),
                @Index(name = "idx_vlc_achat",   columnList = "achat_id"),
                @Index(name = "idx_vlc_tenant",  columnList = "tenant_id"),
                @Index(name = "idx_vlc_date_vente", columnList = "date_vente_snapshot")
        }
)
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = (SELECT t.id FROM tenants t WHERE t.tenant_uuid = :tenantId)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class VenteLotConsommationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vlc_vente"))
    @JsonIgnore
    @ToString.Exclude
    private VenteEntity vente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achat_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vlc_achat"))
    @JsonIgnore
    @ToString.Exclude
    private AchatEntity achat;

    @NotNull
    @Positive
    @Column(name = "quantite_consommee", nullable = false)
    private Double quantiteConsommee;

    /**
     * Snapshot du prix d'achat unitaire du lot au moment de la vente.
     * Figé : ne change pas si l'achat est modifié plus tard.
     */
    @NotNull
    @Column(name = "prix_achat_unitaire_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixAchatUnitaireSnapshot;

    /**
     * Snapshot du prix de vente unitaire au moment de la vente.
     * Figé pour la traçabilité.
     */
    @NotNull
    @Column(name = "prix_vente_unitaire_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixVenteUnitaireSnapshot;

    /**
     * Bénéfice unitaire = prix_vente - prix_achat (snapshot).
     * Dénormalisé pour permettre des agrégats SQL rapides.
     */
    @NotNull
    @Column(name = "benefice_unitaire", nullable = false, precision = 10, scale = 2)
    private BigDecimal beneficeUnitaire;

    /**
     * Bénéfice total de cette ligne = quantite_consommee × benefice_unitaire.
     * Dénormalisé pour SUM() rapide sur les rapports.
     */
    @NotNull
    @Column(name = "benefice_total_ligne", nullable = false, precision = 12, scale = 2)
    private BigDecimal beneficeTotalLigne;

    /**
     * Snapshot de la date de vente.
     * Dupliqué depuis VenteEntity pour permettre des filtres par période
     * SANS jointure sur ventes (performance).
     */
    @NotNull
    @Column(name = "date_vente_snapshot", nullable = false)
    private java.time.LocalDateTime dateVenteSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vlc_tenant"))
    @JsonIgnore
    @ToString.Exclude
    private TenantEntity tenant;

    /**
     * Recalcule le bénéfice unitaire et total à partir des snapshots de prix.
     * À appeler avant la persistance.
     *
     * prixAchatUnitaireSnapshot et prixVenteUnitaireSnapshot sont chacun bruts, dans
     * la devise propre de leur transaction d'origine (l'achat et la vente peuvent avoir
     * des deviseCode différents si la devise du tenant a changé entre les deux — courant
     * en usage réel). Les soustraire directement mélange deux devises : ex. un achat de
     * 100 000 XOF et une vente de 150 EUR produirait un "bénéfice" de -99 850 au lieu
     * d'un profit réel. On pivote donc le prix d'achat par XOF (via son propre taux) puis
     * on le reconvertit dans la devise de la VENTE (via le taux de la vente), pour rester
     * cohérent avec beneficeTotalLigne qui est ensuite normalisé en XOF ailleurs via
     * `× vente.tauxChangeApplique` (voir VenteLotConsommationRepository).
     */
    public void recalculerBenefice() {
        if (this.prixVenteUnitaireSnapshot != null && this.prixAchatUnitaireSnapshot != null) {
            double tauxAchat = (this.achat != null && this.achat.getTauxChangeApplique() != null)
                    ? this.achat.getTauxChangeApplique() : 1.0;
            double tauxVente = (this.vente != null && this.vente.getTauxChangeApplique() != null)
                    ? this.vente.getTauxChangeApplique() : 1.0;
            BigDecimal prixAchatXof = this.prixAchatUnitaireSnapshot.multiply(BigDecimal.valueOf(tauxAchat));
            BigDecimal prixAchatEnDeviseVente = prixAchatXof.divide(BigDecimal.valueOf(tauxVente), 6, RoundingMode.HALF_UP);
            this.beneficeUnitaire = this.prixVenteUnitaireSnapshot
                    .subtract(prixAchatEnDeviseVente)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (this.beneficeUnitaire != null && this.quantiteConsommee != null) {
            this.beneficeTotalLigne = this.beneficeUnitaire
                    .multiply(BigDecimal.valueOf(this.quantiteConsommee))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    @Override
    protected void beforePersist() {
        recalculerBenefice();
    }

    @Override
    protected void beforeUpdate() {
        recalculerBenefice();
    }
}
