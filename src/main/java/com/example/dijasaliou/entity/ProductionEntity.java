package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Un événement de production : le commerçant a combiné des ingrédients
 * pour fabriquer {@code quantiteProduite} unités d'un produit fabriqué.
 * <p>
 * Ne gère aucun stock elle-même : {@link #achat} pointe vers le lot
 * {@link AchatEntity} créé automatiquement (via AchatService.creerAchat)
 * pour représenter ce stock — c'est ce lot que le moteur FIFO existant
 * consomme normalement à la vente. Cette entité et ses
 * {@link ProductionIngredientEntity} ne servent qu'à la traçabilité du
 * détail des coûts.
 */
@Entity
@Table(name = "productions", indexes = {
    @Index(name = "idx_production_tenant", columnList = "tenant_id"),
    @Index(name = "idx_production_produit", columnList = "produit_fabrique_id"),
    @Index(name = "idx_production_date", columnList = "tenant_id, date_production")
})
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = (SELECT t.id FROM tenants t WHERE t.tenant_uuid = :tenantId)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class ProductionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produit_fabrique_id", nullable = false, foreignKey = @ForeignKey(name = "fk_production_produit_fabrique"))
    private ProduitFabriqueEntity produitFabrique;

    /**
     * Lot de stock généré pour cette production (via AchatService.creerAchat).
     * C'est ce lot que le FIFO consomme à la vente — jamais modifié directement ici.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achat_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_production_achat"))
    private AchatEntity achat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, foreignKey = @ForeignKey(name = "fk_production_utilisateur"))
    @ToString.Exclude
    private UserEntity utilisateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_production_tenant"))
    @JsonIgnore
    private TenantEntity tenant;

    @NotNull(message = "La quantité produite est obligatoire")
    @Positive(message = "La quantité produite doit être supérieure à 0")
    @Column(name = "quantite_produite", nullable = false)
    private Double quantiteProduite;

    @NotNull
    @Column(name = "cout_ingredients_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal coutIngredientsTotal;

    @NotNull
    @Column(name = "cout_unitaire", nullable = false, precision = 10, scale = 2)
    private BigDecimal coutUnitaire;

    @Column(name = "date_production", nullable = false)
    private LocalDateTime dateProduction;

    @Column(name = "notes", length = 200)
    private String notes;

    @OneToMany(mappedBy = "production", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductionIngredientEntity> ingredients = new ArrayList<>();

    @Override
    protected void beforePersist() {
        if (this.dateProduction == null) {
            this.dateProduction = LocalDateTime.now();
        }
    }
}
