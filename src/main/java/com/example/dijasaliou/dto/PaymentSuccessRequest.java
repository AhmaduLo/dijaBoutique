package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.TenantEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour confirmer un paiement réussi
 *
 * Le frontend envoie après la confirmation Stripe :
 * {
 *   "paymentIntentId": "pi_xxx",
 *   "plan": "BASIC"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessRequest {

    @NotBlank(message = "L'ID du PaymentIntent est obligatoire")
    private String paymentIntentId;

    @NotNull(message = "Le plan est obligatoire")
    private TenantEntity.Plan plan;
}
