package com.example.dijasaliou.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la mise à jour d'une devise
 * Tous les champs sont optionnels
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDeviseDto {

    @Size(max = 10, message = "Le code ne peut pas dépasser 10 caractères")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Le code doit être composé de 3 lettres majuscules (ex: USD, EUR, XOF)")
    private String code;

    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String nom;

    @Size(max = 10, message = "Le symbole ne peut pas dépasser 10 caractères")
    private String symbole;

    @Size(max = 100, message = "Le pays ne peut pas dépasser 100 caractères")
    private String pays;

    @Positive(message = "Le taux de change doit être positif")
    @DecimalMin(value = "0.0001", message = "Le taux de change doit être supérieur à 0")
    private Double tauxChange;

    private Boolean isDefault;
}
