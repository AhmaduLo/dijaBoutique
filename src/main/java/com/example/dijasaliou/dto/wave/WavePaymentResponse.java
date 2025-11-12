package com.example.dijasaliou.dto.wave;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse de création de paiement Wave
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WavePaymentResponse {
    /**
     * ID de la transaction Wave
     */
    private String transactionId;

    /**
     * Référence unique de la transaction
     */
    private String reference;

    /**
     * Statut du paiement
     * PENDING, SUCCESS, FAILED, CANCELLED
     */
    private String status;

    /**
     * URL de paiement (si applicable)
     */
    private String paymentUrl;

    /**
     * Montant
     */
    private Long amount;

    /**
     * Devise
     */
    private String currency;

    /**
     * Message de réponse
     */
    private String message;
}
