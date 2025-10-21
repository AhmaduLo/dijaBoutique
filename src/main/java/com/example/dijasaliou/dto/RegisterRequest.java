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
 *   "role": "USER"
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
    private UserEntity.Role role;
}
