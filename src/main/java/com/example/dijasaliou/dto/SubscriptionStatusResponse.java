package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour retourner le statut de l'abonnement
 *
 * Exemple de réponse :
 * {
 *   "plan": "BASIC",
 *   "actif": true,
 *   "dateExpiration": "2025-12-31T23:59:59",
 *   "joursRestants": 350,
 *   "essaiGratuit": false,
 *   "estExpire": false
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStatusResponse {

    /**
     * Plan actuel
     */
    private String plan;

    /**
     * Le tenant est actif
     */
    private Boolean actif;

    /**
     * Date d'expiration de l'abonnement
     */
    private LocalDateTime dateExpiration;

    /**
     * Nombre de jours restants avant expiration
     */
    private Long joursRestants;

    /**
     * L'entreprise est en période d'essai gratuit
     */
    private Boolean essaiGratuit;

    /**
     * L'abonnement est expiré
     */
    private Boolean estExpire;

    /**
     * Message pour l'utilisateur
     */
    private String message;
}
