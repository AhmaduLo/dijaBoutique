package com.example.dijasaliou.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réinitialisation effective du mot de passe
 * L'utilisateur fournit le token reçu par email et son nouveau mot de passe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Le token est obligatoire")
    private String token;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String nouveauMotDePasse;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    private String confirmationMotDePasse;
}
