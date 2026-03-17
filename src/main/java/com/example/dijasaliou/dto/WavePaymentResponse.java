package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse d'initiation de paiement Wave
 *
 * Exemple de réponse :
 * {
 *   "waveTransactionId": "wave_123456789",
 *   "waveUrl": "https://pay.wave.com/checkout/wave_123456789",
 *   "montant": 6555,
 *   "devise": "XOF",
 *   "plan": "BASIC",
 *   "numeroTelephone": "+221771234567",
 *   "statut": "PENDING"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WavePaymentResponse {

    /**
     * ID de la transaction Wave
     */
    private String waveTransactionId;

    /**
     * URL de paiement Wave (pour redirection)
     */
    private String waveUrl;

    /**
     * Montant en XOF (Francs CFA)
     */
    private double montant;

    /**
     * Devise (toujours XOF pour Wave)
     */
    private String devise;

    /**
     * Plan choisi
     */
    private String plan;

    /**
     * Numéro de téléphone du client
     */
    private String numeroTelephone;

    /**
     * Statut de la transaction (PENDING, SUCCESS, FAILED)
     */
    private String statut;
}
