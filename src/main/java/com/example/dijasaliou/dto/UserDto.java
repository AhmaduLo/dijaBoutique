package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour l'utilisateur
 *
 * Utilisé pour :
 * - Réponses d'authentification
 * - Informations utilisateur dans les achats/ventes/dépenses
 * - Évite l'exposition du mot de passe
 * - Évite les boucles de sérialisation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String nom;
    private String prenom;
     private String nomEntreprise;       
    private String numeroTelephone;
    private String email;
    private UserEntity.Role role;
    private LocalDateTime dateCreation;

    /**
     * Convertit une entité User en DTO
     * Pattern Factory Method
     */
    public static UserDto fromEntity(UserEntity user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .nomEntreprise(user.getNomEntreprise())
                .numeroTelephone(user.getNumeroTelephone())
                .email(user.getEmail())
                .role(user.getRole())
                .dateCreation(user.getDateCreation())
                .build();
    }

    /**
     * Version minimaliste sans date de création
     * Utile pour les listes imbriquées
     */
    public static UserDto fromEntityMinimal(UserEntity user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .nomEntreprise(user.getNomEntreprise())
                .numeroTelephone(user.getNumeroTelephone())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}