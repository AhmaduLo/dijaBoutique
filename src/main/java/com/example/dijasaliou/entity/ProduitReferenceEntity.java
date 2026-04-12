package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Table partagée de codes-barres — accessible par TOUS les tenants.
 * Pas de tenant_id : les données sont communautaires.
 * Les commerçants contribuent automatiquement quand ils renseignent un nouveau produit.
 */
@Entity
@Table(name = "produits_reference", indexes = {
        @Index(name = "idx_prodref_code_barre", columnList = "code_barre", unique = true),
        @Index(name = "idx_prodref_nom", columnList = "nom_produit")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitReferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le code-barre est obligatoire")
    @Size(max = 50)
    @Column(name = "code_barre", nullable = false, unique = true, length = 50)
    private String codeBarre;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(max = 200)
    @Column(name = "nom_produit", nullable = false, length = 200)
    private String nomProduit;

    @Size(max = 500)
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Size(max = 100)
    @Column(name = "categorie", length = 100)
    private String categorie;

    @Column(name = "nb_utilisations", nullable = false)
    @Builder.Default
    private Integer nbUtilisations = 1;

    @Size(max = 100)
    @Column(name = "contribue_par_tenant_nom", length = 100)
    private String contribueParTenantNom;

    @Column(name = "date_creation", nullable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}
