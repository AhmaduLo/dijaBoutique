package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.AchatEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO simplifié pour afficher uniquement les informations nécessaires
 * à la création d'une vente (pour les utilisateurs de type USER)
 *
 * Ce DTO ne contient que le strict nécessaire :
 * - Le nom du produit
 * - Le prix de vente suggéré (si renseigné)
 *
 * Cela permet aux USER de créer des ventes avec des prix pré-remplis
 * sans avoir accès aux informations sensibles des achats (prix d'achat, fournisseur, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitPourVenteDto {

    private String nomProduit;
    private BigDecimal prixVenteSuggere;

    /**
     * Convertit un achat en DTO simplifié pour la vente
     */
    public static ProduitPourVenteDto fromAchat(AchatEntity achat) {
        if (achat == null) {
            return null;
        }

        return ProduitPourVenteDto.builder()
                .nomProduit(achat.getNomProduit())
                .prixVenteSuggere(achat.getPrixVenteSuggere())
                .build();
    }
}
