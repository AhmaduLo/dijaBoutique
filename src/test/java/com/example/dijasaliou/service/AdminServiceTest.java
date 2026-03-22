package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.dto.UserDto;
import com.example.dijasaliou.entity.TenantEntity;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — AdminService")
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TenantService tenantService;

    @InjectMocks
    private AdminService adminService;

    private UserEntity admin;
    private UserEntity user;
    private TenantEntity tenantTest;

    @BeforeEach
    void setUp() {
        tenantTest = new TenantEntity();
        tenantTest.setTenantUuid("uuid-tenant-test");
        tenantTest.setPlan(TenantEntity.Plan.PREMIUM); // PREMIUM = max 5 users

        admin = UserEntity.builder()
                .id(1L).nom("Admin").prenom("Super")
                .email("admin@example.com").motDePasse("encodedPassword")
                .role(UserEntity.Role.ADMIN)
                .tenant(tenantTest)
                .dateCreation(LocalDateTime.now())
                .deleted(false)
                .build();

        user = UserEntity.builder()
                .id(2L).nom("User").prenom("Normal")
                .email("user@example.com").motDePasse("encodedPassword")
                .role(UserEntity.Role.USER)
                .tenant(tenantTest)
                .dateCreation(LocalDateTime.now())
                .deleted(false)
                .build();
    }

    // =========================================================
    // obtenirTousLesUtilisateurs
    // =========================================================

    @Test
    @DisplayName("obtenirTousLesUtilisateurs() — retourne tous les utilisateurs actifs")
    void obtenirTousLesUtilisateurs_retourneListe() {
        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByDeletedFalse()).thenReturn(Arrays.asList(admin, user));

        var resultat = adminService.obtenirTousLesUtilisateurs("admin@example.com");

        assertThat(resultat).hasSize(2);
    }

    @Test
    @DisplayName("obtenirTousLesUtilisateurs() — lève exception si l'appelant n'est pas ADMIN")
    void obtenirTousLesUtilisateurs_leveExceptionSiNonAdmin() {
        when(userRepository.findByEmailAndDeletedFalse("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.obtenirTousLesUtilisateurs("user@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Accès refusé");
    }

    // =========================================================
    // creerUtilisateur
    // =========================================================

    @Test
    @DisplayName("creerUtilisateur() — crée un utilisateur USER dans le tenant")
    void creerUtilisateur_succes() {
        RegisterRequest request = new RegisterRequest();
        request.setNom("Nouveau"); request.setPrenom("User");
        request.setEmail("nouveau@example.com"); request.setMotDePasse("password");
        request.setRole(UserEntity.Role.USER);

        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailAndDeletedFalse("nouveau@example.com")).thenReturn(false);
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(userRepository.countByTenantAndDeletedFalse(tenantTest)).thenReturn(1L);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(user);

        UserDto resultat = adminService.creerUtilisateur(request, "admin@example.com");

        assertThat(resultat).isNotNull();
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("creerUtilisateur() — lève exception si email déjà utilisé")
    void creerUtilisateur_leveExceptionEmailExiste() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");

        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailAndDeletedFalse("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> adminService.creerUtilisateur(request, "admin@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("existe déjà");
    }

    // =========================================================
    // modifierUtilisateur
    // =========================================================

    @Test
    @DisplayName("modifierUtilisateur() — modifie le nom et le prénom")
    void modifierUtilisateur_succes() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("nom", "Nouveau Nom");
        updates.put("prenom", "Nouveau Prénom");

        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(userRepository.saveAndFlush(any())).thenReturn(user);

        UserDto resultat = adminService.modifierUtilisateur(2L, updates, "admin@example.com");

        assertThat(resultat).isNotNull();
        verify(userRepository).saveAndFlush(any());
    }

    // =========================================================
    // supprimerUtilisateur
    // =========================================================

    @Test
    @DisplayName("supprimerUtilisateur() — effectue une suppression logique (anonymisation)")
    void supprimerUtilisateur_suppressionLogique() {
        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        adminService.supprimerUtilisateur(2L, "admin@example.com");

        verify(userRepository).save(argThat(u -> Boolean.TRUE.equals(u.getDeleted())));
    }

    @Test
    @DisplayName("supprimerUtilisateur() — lève exception si l'admin tente de se supprimer lui-même")
    void supprimerUtilisateur_leveExceptionAutoSuppression() {
        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        assertThatThrownBy(() -> adminService.supprimerUtilisateur(1L, "admin@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("votre propre compte");
    }

    // =========================================================
    // modifierRole
    // =========================================================

    @Test
    @DisplayName("modifierRole() — change le rôle USER → ADMIN")
    void modifierRole_succes() {
        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserDto resultat = adminService.modifierRole(2L, "ADMIN", "admin@example.com");

        assertThat(resultat).isNotNull();
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("modifierRole() — lève exception si rôle invalide")
    void modifierRole_leveExceptionRoleInvalide() {
        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.modifierRole(2L, "INVALID_ROLE", "admin@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rôle invalide");
    }

    // =========================================================
    // obtenirStatistiques
    // =========================================================

    // =========================================================
    // obtenirUtilisateurParId
    // =========================================================

    @Test
    @DisplayName("obtenirUtilisateurParId() — retourne l'utilisateur si trouvé")
    void obtenirUtilisateurParId_retourneUser() {
        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        var resultat = adminService.obtenirUtilisateurParId(2L, "admin@example.com");

        assertThat(resultat).isNotNull();
    }

    @Test
    @DisplayName("obtenirUtilisateurParId() — lève exception si utilisateur non trouvé")
    void obtenirUtilisateurParId_leveException() {
        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.obtenirUtilisateurParId(999L, "admin@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }

    // =========================================================
    // obtenirStatistiques
    // =========================================================

    @Test
    @DisplayName("obtenirStatistiques() — retourne les clés nombreTotal, nombreAdmins, nombreUsers")
    void obtenirStatistiques_retourneStats() {
        when(userRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByDeletedFalse()).thenReturn(Arrays.asList(admin, user));

        Map<String, Object> stats = adminService.obtenirStatistiques("admin@example.com");

        assertThat(stats).containsKeys("nombreTotal", "nombreAdmins", "nombreUsers");
        assertThat(stats.get("nombreTotal")).isEqualTo(2L);
    }
}
