package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête de connexion
 *
 * Angular envoie :
 * {
 *   "email": "dija@boutique.com",
 *   "motDePasse": "password123"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String email;
    private String motDePasse;
}
