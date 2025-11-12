package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse du PaymentIntent Stripe
 *
 * Retourne au frontend :
 * {
 *   "clientSecret": "pi_xxx_secret_xxx",
 *   "montant": 999,
 *   "devise": "eur",
 *   "plan": "BASIC"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntentResponse {

    /**
     * Client secret pour confirmer le paiement côté frontend
     */
    private String clientSecret;

    /**
     * Montant en centimes
     */
    private Long montant;

    /**
     * Devise (eur, xof pour CFA)
     */
    private String devise;

    /**
     * Plan sélectionné
     */
    private String plan;

    /**
     * Message pour l'utilisateur
     */
    private String message;
}
