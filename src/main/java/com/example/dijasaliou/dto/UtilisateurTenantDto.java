package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.UserEntity;

import java.time.LocalDateTime;

public record UtilisateurTenantDto(
        Long id,
        String nom,
        String prenom,
        String email,
        String role,
        boolean actif,
        LocalDateTime derniereConnexion
) {
    public static UtilisateurTenantDto fromEntity(UserEntity user) {
        return new UtilisateurTenantDto(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getRole().name(),
                !Boolean.TRUE.equals(user.getDeleted()),
                user.getDerniereConnexion()
        );
    }
}
