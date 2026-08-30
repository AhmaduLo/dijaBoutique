package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.ModePaiementCaisse;
import com.example.dijasaliou.entity.ProductionEntity;
import com.example.dijasaliou.entity.ProductionIngredientEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionDto {
    private String id;
    private Long produitFabriqueId;
    private String nomProduit;
    private String achatId;
    private Double quantiteProduite;
    private BigDecimal coutIngredientsTotal;
    private BigDecimal coutUnitaire;
    private LocalDateTime dateProduction;
    private String notes;
    private String photoUrl;
    private BigDecimal prixVenteSuggere;
    private ModePaiementCaisse modePaiement;
    /** Devise dans laquelle les montants de CETTE production ont été enregistrés (celle du lot d'achat lié, figée à la création) — pas la devise courante de la boutique. */
    private String deviseCode;
    private List<IngredientLigneDto> ingredients;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngredientLigneDto {
        private String nomIngredient;
        private String unite;
        private BigDecimal cout;
    }

    public static ProductionDto fromEntity(ProductionEntity production) {
        if (production == null) {
            return null;
        }
        List<IngredientLigneDto> lignes = production.getIngredients() == null
                ? List.of()
                : production.getIngredients().stream()
                    .map(ProductionDto::toIngredientDto)
                    .collect(Collectors.toList());

        return ProductionDto.builder()
                .id(production.getId())
                .produitFabriqueId(production.getProduitFabrique() != null ? production.getProduitFabrique().getId() : null)
                .nomProduit(production.getProduitFabrique() != null ? production.getProduitFabrique().getNomProduit() : null)
                .achatId(production.getAchat() != null ? production.getAchat().getId() : null)
                .quantiteProduite(production.getQuantiteProduite())
                .coutIngredientsTotal(production.getCoutIngredientsTotal())
                .coutUnitaire(production.getCoutUnitaire())
                .dateProduction(production.getDateProduction())
                .notes(production.getNotes())
                .photoUrl(production.getAchat() != null ? production.getAchat().getPhotoUrl() : null)
                .prixVenteSuggere(production.getAchat() != null ? production.getAchat().getPrixVenteSuggere() : null)
                .modePaiement(production.getAchat() != null ? production.getAchat().getModePaiement() : null)
                .deviseCode(production.getAchat() != null ? production.getAchat().getDeviseCode() : null)
                .ingredients(lignes)
                .build();
    }

    private static IngredientLigneDto toIngredientDto(ProductionIngredientEntity ligne) {
        return IngredientLigneDto.builder()
                .nomIngredient(ligne.getNomIngredient())
                .unite(ligne.getUnite())
                .cout(ligne.getCout())
                .build();
    }
}
