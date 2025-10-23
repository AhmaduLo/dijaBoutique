package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'utilisateur renvoyé dans AuthResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private UserEntity.Role role;
}