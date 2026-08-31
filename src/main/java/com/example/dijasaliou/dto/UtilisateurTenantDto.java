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
        /** Début de la session en cours — voir UserEntity.dateDebutSession pour le détail du calcul côté serveur. */
        OffsetDateTime dateDebutSession,
        boolean emailVerifie
) {
    public static UtilisateurTenantDto fromEntity(UserEntity user) {
        LocalDateTime dc = user.getDerniereConnexion();
        LocalDateTime dds = user.getDateDebutSession();
        return new UtilisateurTenantDto(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getRole().name(),
                !Boolean.TRUE.equals(user.getDeleted()),
                dc != null ? dc.atOffset(ZoneOffset.UTC) : null,
                dds != null ? dds.atOffset(ZoneOffset.UTC) : null,
                Boolean.TRUE.equals(user.getEmailVerifie())
        );
    }
}
