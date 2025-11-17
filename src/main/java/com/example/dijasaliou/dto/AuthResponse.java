package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse d'authentification
 *
 * Backend renvoie :
 * {
 *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "type": "Bearer",
 *   "id": 1,
 *   "email": "dija@boutique.com",
 *   "nom": "Saliou",
 *   "prenom": "Dija",
 *   "role": "ADMIN",
 *   "requiresPayment": true,  // Si true, rediriger vers page de paiement
 *   "plan": "GRATUIT"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private UserEntity user;

    /**
     * Indique si l'utilisateur doit payer avant d'accéder à l'application
     * true si plan = GRATUIT ou dateExpiration dépassée
     */
    private boolean requiresPayment;

    /**
     * Plan d'abonnement actuel du tenant
     */
    private TenantEntity.Plan plan;
}
