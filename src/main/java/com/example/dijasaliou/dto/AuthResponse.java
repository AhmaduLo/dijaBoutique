package com.example.dijasaliou.dto;

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
 *   "role": "ADMIN"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private UserEntity.Role role;
}
