package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.TenantEntity;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer un PaymentIntent Stripe
 *
 * Le frontend envoie :
 * {
 *   "plan": "BASIC",
 *   "devise": "EUR"  // ou "CFA"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentIntentRequest {

    @NotNull(message = "Le plan est obligatoire")
    private TenantEntity.Plan plan;

    /**
     * Devise pour le paiement : "EUR" ou "CFA"
     * Par défaut : EUR
     */
    private String devise = "EUR";
}
