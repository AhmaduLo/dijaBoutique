package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.DeviseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour représenter une devise
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviseDto {

    private Long id;
    private String code;
    private String nom;
    private String symbole;
    private String pays;
    private Double tauxChange;
    private Boolean isDefault;
    private LocalDateTime dateCreation;

    /**
     * Convertit une entité Devise en DTO
     */
    public static DeviseDto fromEntity(DeviseEntity entity) {
        if (entity == null) {
            return null;
        }

        return DeviseDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .nom(entity.getNom())
                .symbole(entity.getSymbole())
                .pays(entity.getPays())
                .tauxChange(entity.getTauxChange())
                .isDefault(entity.getIsDefault())
                .dateCreation(entity.getDateCreation())
                .build();
    }
}
