package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse des opérations de réinitialisation de mot de passe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponse {
    private String message;
}
