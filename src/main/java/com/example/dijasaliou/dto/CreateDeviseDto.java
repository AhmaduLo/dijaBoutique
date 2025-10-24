package com.example.dijasaliou.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la création d'une nouvelle devise
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDeviseDto {

    @NotBlank(message = "Le code de la devise est obligatoire")
    @Size(max = 10, message = "Le code ne peut pas dépasser 10 caractères")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Le code doit être composé de 3 lettres majuscules (ex: USD, EUR, XOF)")
    private String code;

    @NotBlank(message = "Le nom de la devise est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String nom;

    @NotBlank(message = "Le symbole de la devise est obligatoire")
    @Size(max = 10, message = "Le symbole ne peut pas dépasser 10 caractères")
    private String symbole;

    @NotBlank(message = "Le pays est obligatoire")
    @Size(max = 100, message = "Le pays ne peut pas dépasser 100 caractères")
    private String pays;

    @NotNull(message = "Le taux de change est obligatoire")
    @Positive(message = "Le taux de change doit être positif")
    @DecimalMin(value = "0.0001", message = "Le taux de change doit être supérieur à 0")
    private Double tauxChange;

    private Boolean isDefault;
}
