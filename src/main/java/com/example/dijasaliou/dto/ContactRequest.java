package com.example.dijasaliou.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour le formulaire de contact
 *
 * Permet aux administrateurs d'envoyer un message de contact
 * à l'équipe de support
 *
 * Exemple :
 * {
 *   "nom": "Dija Saliou",
 *   "email": "admin@boutique.com",
 *   "entreprise": "Boutique DijaSaliou",
 *   "sujet": "Demande d'assistance technique",
 *   "message": "Bonjour, j'ai besoin d'aide pour..."
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequest {

    /**
     * Nom complet de l'utilisateur qui contacte
     */
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String nom;

    /**
     * Email de l'utilisateur (pour pouvoir lui répondre)
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    /**
     * Nom de l'entreprise de l'utilisateur
     */
    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    @Size(max = 100, message = "Le nom de l'entreprise ne peut pas dépasser 100 caractères")
    private String entreprise;

    /**
     * Sujet du message
     */
    @NotBlank(message = "Le sujet est obligatoire")
    @Size(max = 200, message = "Le sujet ne peut pas dépasser 200 caractères")
    private String sujet;

    /**
     * Message détaillé
     */
    @NotBlank(message = "Le message est obligatoire")
    @Size(min = 10, max = 2000, message = "Le message doit contenir entre 10 et 2000 caractères")
    private String message;
}
