package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Ligne d'un bon de livraison
 * Représente un produit à livrer (sans prix)
 */
@Entity
@Table(name = "bons_livraison_lignes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneBLEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bon_livraison_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ligne_bl_bon_livraison"))
    private BonLivraisonEntity bonLivraison;

    @Column(name = "nom_produit", nullable = false, length = 100)
    private String nomProduit;

    @Column(name = "quantite", nullable = false)
    private Double quantite;

    @Column(name = "unite", length = 20)
    @Builder.Default
    private String unite = "pièce";
}
