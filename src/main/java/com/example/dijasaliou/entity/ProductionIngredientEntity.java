package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Une ligne d'ingrédient d'une {@link ProductionEntity} : nom libre +
 * coût total dépensé pour cet ingrédient dans ce batch (pas un prix
 * unitaire — le commerçant saisit ce qu'il a réellement dépensé).
 * <p>
 * Traçabilité uniquement : n'alimente jamais la table {@code achats}
 * (le coût agrégé y est déjà représenté par le lot unique de la
 * production, voir {@link ProductionEntity#getAchat()}).
 */
@Entity
@Table(name = "production_ingredients", indexes = {
    @Index(name = "idx_prod_ingredient_production", columnList = "production_id"),
    @Index(name = "idx_prod_ingredient_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class ProductionIngredientEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prod_ingredient_production"))
    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductionEntity production;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prod_ingredient_tenant"))
    @JsonIgnore
    private TenantEntity tenant;

    @NotBlank(message = "Le nom de l'ingrédient est obligatoire")
    @Size(max = 150, message = "Le nom de l'ingrédient ne peut dépasser 150 caractères")
    @Column(name = "nom_ingredient", nullable = false, length = 150)
    private String nomIngredient;

    @NotNull(message = "Le coût de l'ingrédient est obligatoire")
    @DecimalMin(value = "0.01", message = "Le coût doit être supérieur à 0")
    @Column(name = "cout", nullable = false, precision = 10, scale = 2)
    private BigDecimal cout;

    @Size(max = 20, message = "L'unité ne peut dépasser 20 caractères")
    @Column(name = "unite", length = 20)
    private String unite;

    @Column(name = "ordre", nullable = false)
    @Builder.Default
    private Integer ordre = 0;

    @Override
    protected void beforePersist() {
        if (this.nomIngredient != null) {
            this.nomIngredient = this.nomIngredient.trim();
        }
        if (this.ordre == null) {
            this.ordre = 0;
        }
    }
}
