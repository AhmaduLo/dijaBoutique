package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.TenantEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour initier un paiement Wave
 *
 * Exemple de requête :
 * {
 *   "plan": "BASIC",
 *   "numeroTelephone": "+221771234567"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WavePaymentRequest {

    /**
     * Plan choisi par l'utilisateur
     */
    @NotNull(message = "Le plan est obligatoire")
    private TenantEntity.Plan plan;

    /**
     * Numéro de téléphone Wave du client (format international)
     * Exemple: +221771234567
     */
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    private String numeroTelephone;
}
