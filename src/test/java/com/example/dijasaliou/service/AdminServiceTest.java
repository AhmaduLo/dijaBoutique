package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.dto.UserDto;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du AdminService")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private UserEntity admin;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        admin = UserEntity.builder()
                .id(1L)
                .nom("Admin")
                .prenom("Super")
                .email("admin@example.com")
                .motDePasse("encodedPassword")
                .role(UserEntity.Role.ADMIN)
                .dateCreation(LocalDateTime.now())
                .build();

        user = UserEntity.builder()
                .id(2L)
                .nom("User")
                .prenom("Normal")
                .email("user@example.com")
                .motDePasse("encodedPassword")
                .role(UserEntity.Role.USER)
                .dateCreation(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("obtenirTousLesUtilisateurs() - Devrait retourner tous les utilisateurs pour un admin")
    void obtenirTousLesUtilisateurs_DevraitRetourner() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(Arrays.asList(admin, user));

        List<UserDto> resultat = adminService.obtenirTousLesUtilisateurs("admin@example.com");

        assertThat(resultat).hasSize(2);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("obtenirTousLesUtilisateurs() - Devrait lancer exception si non admin")
    void obtenirTousLesUtilisateurs_DevraitLancerExceptionSiNonAdmin() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.obtenirTousLesUtilisateurs("user@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Accès refusé");
    }

    @Test
    @DisplayName("creerUtilisateur() - Devrait créer un utilisateur")
    void creerUtilisateur_DevraitCreer() {
        RegisterRequest request = new RegisterRequest();
        request.setNom("Nouveau");
        request.setPrenom("User");
        request.setEmail("nouveau@example.com");
        request.setMotDePasse("password");
        request.setRole(UserEntity.Role.USER);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmail("nouveau@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        UserDto resultat = adminService.creerUtilisateur(request, "admin@example.com");

        assertThat(resultat).isNotNull();
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("creerUtilisateur() - Devrait lancer exception si email existe")
    void creerUtilisateur_DevraitLancerExceptionEmailExiste() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> adminService.creerUtilisateur(request, "admin@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    @DisplayName("modifierUtilisateur() - Devrait modifier un utilisateur")
    void modifierUtilisateur_DevraitModifier() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("nom", "Nouveau Nom");
        updates.put("prenom", "Nouveau Prénom");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        UserDto resultat = adminService.modifierUtilisateur(2L, updates, "admin@example.com");

        assertThat(resultat).isNotNull();
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("supprimerUtilisateur() - Devrait supprimer un utilisateur")
    void supprimerUtilisateur_DevraitSupprimer() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        adminService.supprimerUtilisateur(2L, "admin@example.com");

        verify(userRepository, times(1)).deleteById(2L);
    }

    @Test
    @DisplayName("supprimerUtilisateur() - Ne devrait pas permettre à l'admin de se supprimer")
    void supprimerUtilisateur_NePasPermettreAutoSuppression() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.supprimerUtilisateur(1L, "admin@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("votre propre compte");
    }

    @Test
    @DisplayName("modifierRole() - Devrait modifier le rôle d'un utilisateur")
    void modifierRole_DevraitModifier() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        UserDto resultat = adminService.modifierRole(2L, "ADMIN", "admin@example.com");

        assertThat(resultat).isNotNull();
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("modifierRole() - Devrait lancer exception si rôle invalide")
    void modifierRole_DevraitLancerExceptionRoleInvalide() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.modifierRole(2L, "INVALID_ROLE", "admin@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rôle invalide");
    }

    @Test
    @DisplayName("obtenirStatistiques() - Devrait retourner les statistiques")
    void obtenirStatistiques_DevraitRetourner() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(Arrays.asList(admin, user));

        Map<String, Object> stats = adminService.obtenirStatistiques("admin@example.com");

        assertThat(stats).isNotEmpty();
        assertThat(stats).containsKeys("nombreTotal", "nombreAdmins", "nombreUsers");
        assertThat(stats.get("nombreTotal")).isEqualTo(2L);
    }
}
