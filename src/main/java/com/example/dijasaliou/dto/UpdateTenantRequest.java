package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête de modification des informations de l'entreprise (tenant)
 *
 * Utilisé par les admins pour modifier :
 * - Le nom de l'entreprise
 * - Le numéro de téléphone de l'entreprise
 *
 * Angular envoie :
 * {
 *   "nomEntreprise": "Nouveau Nom Entreprise",
 *   "numeroTelephone": "+221771234567"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequest {
    private String nomEntreprise;
    private String numeroTelephone;
}
