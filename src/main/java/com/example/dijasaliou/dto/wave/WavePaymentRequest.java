package com.example.dijasaliou.dto.wave;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer un paiement Wave
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WavePaymentRequest {
    /**
     * Montant en CFA (Wave ne supporte que le XOF)
     */
    private Long amount;

    /**
     * Devise (toujours "XOF" pour Wave)
     */
    private String currency;

    /**
     * Numéro de téléphone du client au format international
     * Exemple: +221771234567
     */
    private String customerPhone;

    /**
     * Description de la transaction
     */
    private String description;

    /**
     * ID unique de la transaction côté application
     */
    private String reference;

    /**
     * URL de callback pour recevoir les notifications
     */
    private String callbackUrl;
}
