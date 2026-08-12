package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Catalogue des noms de produits marqués "fabriqués" (issus de production,
 * par opposition aux produits revendus classiquement).
 * <p>
 * Ne stocke aucune donnée de stock : sert uniquement à savoir quels
 * {@code nomProduit} peuvent être choisis dans le formulaire "Nouvelle
 * production", et à piloter la visibilité de ce bouton côté frontend
 * (masqué tant que le tenant n'a aucune ligne ici).
 */
@Entity
@Table(name = "produits_fabriques", indexes = {
    @Index(name = "idx_produit_fabrique_tenant", columnList = "tenant_id")
})
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = (SELECT t.id FROM tenants t WHERE t.tenant_uuid = :tenantId)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class ProduitFabriqueEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du produit fabriqué est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit faire entre 2 et 100 caractères")
    @Column(name = "nom_produit", nullable = false, length = 100)
    private String nomProduit;

    @Size(max = 20, message = "L'unité ne peut dépasser 20 caractères")
    @Column(name = "unite", length = 20)
    @Builder.Default
    private String unite = "pièce";

    @Size(max = 200, message = "La description ne peut dépasser 200 caractères")
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_produit_fabrique_tenant"))
    @JsonIgnore
    private TenantEntity tenant;

    @Override
    protected void beforePersist() {
        if (this.nomProduit != null) {
            this.nomProduit = this.nomProduit.trim();
        }
        if (this.actif == null) {
            this.actif = true;
        }
    }
}
