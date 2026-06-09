package com.example.dijasaliou.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload envoyé par le frontend pour enregistrer un appareil aux push.
 * Correspond à la structure standard PushSubscription du navigateur :
 *   subscription.endpoint
 *   subscription.keys.p256dh
 *   subscription.keys.auth
 */
public record PushSubscriptionRequest(
        @NotBlank String endpoint,
        @NotBlank String p256dh,
        @NotBlank String auth,
        String userAgent
) {}
