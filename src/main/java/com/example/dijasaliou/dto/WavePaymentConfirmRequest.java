package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.TenantEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour confirmer un paiement Wave après succès
 *
 * Exemple de requête :
 * {
 *   "waveTransactionId": "wave_123456789",
 *   "plan": "BASIC"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WavePaymentConfirmRequest {

    /**
     * ID de la transaction Wave à confirmer
     */
    @NotBlank(message = "L'ID de transaction Wave est obligatoire")
    private String waveTransactionId;

    /**
     * Plan choisi (pour vérification)
     */
    @NotNull(message = "Le plan est obligatoire")
    private TenantEntity.Plan plan;
}
