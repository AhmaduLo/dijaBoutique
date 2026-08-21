package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.UserEntity;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record UtilisateurTenantDto(
        Long id,
        String nom,
        String prenom,
        String email,
        String role,
        boolean actif,
        OffsetDateTime derniereConnexion,
        boolean emailVerifie
) {
    public static UtilisateurTenantDto fromEntity(UserEntity user) {
        LocalDateTime dc = user.getDerniereConnexion();
        return new UtilisateurTenantDto(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getRole().name(),
                !Boolean.TRUE.equals(user.getDeleted()),
                dc != null ? dc.atOffset(ZoneOffset.UTC) : null,
                Boolean.TRUE.equals(user.getEmailVerifie())
        );
    }
}
