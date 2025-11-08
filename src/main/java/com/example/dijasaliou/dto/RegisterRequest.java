package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête d'inscription
 *
 * Angular envoie :
 * {
 *   "nom": "Saliou",
 *   "prenom": "Dija",
 *   "email": "dija@boutique.com",
 *   "motDePasse": "password123",
 *   "nomEntreprise": "Boutique DijaSaliou",
 *   "numeroTelephone": "+221771234567",
 *   "adresseEntreprise": "123 Rue de la République, Dakar",
 *   "role": "USER",
 *   "acceptationCGU": true,
 *   "acceptationPolitiqueConfidentialite": true
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String nomEntreprise;
    private String numeroTelephone;
    private String adresseEntreprise;
    private UserEntity.Role role;

    /**
     * Acceptation des CGU (Conditions Générales d'Utilisation)
     * Obligatoire pour les ADMIN lors de la création de compte
     */
    private Boolean acceptationCGU;

    /**
     * Acceptation de la Politique de Confidentialité
     * Obligatoire pour les ADMIN lors de la création de compte
     */
    private Boolean acceptationPolitiqueConfidentialite;
}
