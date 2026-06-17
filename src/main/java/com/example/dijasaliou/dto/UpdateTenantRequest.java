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
 * - L'adresse de l'entreprise
 * - Le numéro NINEA/SIRET (optionnel)
 *
 * Angular envoie :
 * {
 *   "nomEntreprise": "Nouveau Nom Entreprise",
 *   "numeroTelephone": "+221771234567",
 *   "adresse": "123 Rue de la République, Dakar",
 *   "nineaSiret": "123456789" (optionnel)
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequest {
    private String nomEntreprise;
    private String numeroTelephone;
    private String adresse;
    private String ville;
    private String pays;

    /**
     * Numéro NINEA (Sénégal) ou SIRET (France) - OPTIONNEL
     */
    private String nineaSiret;

    /**
     * URL du logo de l'entreprise (stocké sur Cloudinary) - OPTIONNEL
     */
    private String logoUrl;

    /**
     * Texte libre imprimé en bas de la facture (conditions, garanties, SAV...).
     * Null ou vide → bloc non imprimé.
     */
    private String conditionsGaranties;

    /**
     * Texte libre imprimé en bas de la facture (mentions légales : pénalités,
     * juridiction, TVA, escompte...). Null ou vide → bloc non imprimé.
     */
    private String mentionsLegales;
}
