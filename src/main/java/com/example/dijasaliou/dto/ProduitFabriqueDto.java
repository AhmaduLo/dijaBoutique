package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.ProduitFabriqueEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitFabriqueDto {
    private Long id;
    private String nomProduit;
    private String unite;
    private String description;
    private Boolean actif;

    public static ProduitFabriqueDto fromEntity(ProduitFabriqueEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProduitFabriqueDto.builder()
                .id(entity.getId())
                .nomProduit(entity.getNomProduit())
                .unite(entity.getUnite())
                .description(entity.getDescription())
                .actif(entity.getActif())
                .build();
    }
}
