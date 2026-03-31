package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
 *   "nineaSiret": "123456789" (optionnel),
 *   "role": "USER",
 *   "acceptationCGU": true,
 *   "acceptationPolitiqueConfidentialite": true
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Nom obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String nom;

    @NotBlank(message = "Prénom obligatoire")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String prenom;

    @NotBlank(message = "Email obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Mot de passe obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au minimum 8 caractères")
    private String motDePasse;

    @Size(max = 100, message = "Le nom de l'entreprise ne peut pas dépasser 100 caractères")
    private String nomEntreprise;

    private String numeroTelephone;

    private String adresseEntreprise;

    private String ville;

    private String pays;

    /**
     * Numéro NINEA (Sénégal) ou SIRET (France) - OPTIONNEL
     * L'entreprise peut ne pas avoir ce numéro lors de l'inscription
     */
    private String nineaSiret;

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
