package com.example.dijasaliou.service;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantService tenantService;

    @InjectMocks
    private UserService userService;

    private UserEntity utilisateur;
    private TenantEntity tenantTest;

    @BeforeEach
    void setUp() {
        tenantTest = new TenantEntity();
        tenantTest.setTenantUuid("uuid-tenant-test");

        utilisateur = UserEntity.builder()
                .id(1L).nom("Diop").prenom("Amadou")
                .email("amadou@example.com")
                .motDePasse("password")
                .role(UserEntity.Role.USER)
                .tenant(tenantTest)
                .build();
    }

    // =========================================================
    // obtenirTousLesUtilisateurs
    // =========================================================

    @Test
    @DisplayName("obtenirTousLesUtilisateurs() — retourne les utilisateurs actifs")
    void obtenirTousLesUtilisateurs_retourneListe() {
        when(userRepository.findByDeletedFalse()).thenReturn(Arrays.asList(utilisateur));

        List<UserEntity> resultat = userService.obtenirTousLesUtilisateurs();

        assertThat(resultat).hasSize(1);
        verify(userRepository).findByDeletedFalse();
    }

    @Test
    @DisplayName("obtenirTousLesUtilisateurs() — retourne liste vide si aucun utilisateur actif")
    void obtenirTousLesUtilisateurs_retourneListeVide() {
        when(userRepository.findByDeletedFalse()).thenReturn(Collections.emptyList());

        assertThat(userService.obtenirTousLesUtilisateurs()).isEmpty();
    }

    // =========================================================
    // obtenirUtilisateurParId
    // =========================================================

    @Test
    @DisplayName("obtenirUtilisateurParId() — retourne l'utilisateur si trouvé")
    void obtenirUtilisateurParId_retourneUtilisateur() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        UserEntity resultat = userService.obtenirUtilisateurParId(1L);

        assertThat(resultat.getId()).isEqualTo(1L);
        assertThat(resultat.getEmail()).isEqualTo("amadou@example.com");
    }

    @Test
    @DisplayName("obtenirUtilisateurParId() — lève exception si non trouvé")
    void obtenirUtilisateurParId_leveException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.obtenirUtilisateurParId(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }

    // =========================================================
    // obtenirUtilisateurParEmail
    // =========================================================

    @Test
    @DisplayName("obtenirUtilisateurParEmail() — retourne l'utilisateur actif")
    void obtenirUtilisateurParEmail_retourneUtilisateur() {
        when(userRepository.findByEmailAndDeletedFalse("amadou@example.com"))
                .thenReturn(Optional.of(utilisateur));

        UserEntity resultat = userService.obtenirUtilisateurParEmail("amadou@example.com");

        assertThat(resultat.getEmail()).isEqualTo("amadou@example.com");
    }

    @Test
    @DisplayName("obtenirUtilisateurParEmail() — lève exception si email inconnu")
    void obtenirUtilisateurParEmail_leveException() {
        when(userRepository.findByEmailAndDeletedFalse("inconnu@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.obtenirUtilisateurParEmail("inconnu@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }

    // =========================================================
    // creerUtilisateur — validations
    // =========================================================

    @Test
    @DisplayName("creerUtilisateur() — lève exception si email déjà utilisé")
    void creerUtilisateur_leveExceptionEmailExiste() {
        when(userRepository.existsByEmailAndDeletedFalse("amadou@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.creerUtilisateur(utilisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email existe déjà");
    }

    @Test
    @DisplayName("creerUtilisateur() — lève exception si nom vide")
    void creerUtilisateur_leveExceptionNomVide() {
        UserEntity u = UserEntity.builder().nom("").prenom("Test")
                .email("test@example.com").build();
        when(userRepository.existsByEmailAndDeletedFalse("test@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.creerUtilisateur(u))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nom");
    }

    @Test
    @DisplayName("creerUtilisateur() — lève exception si format email invalide")
    void creerUtilisateur_leveExceptionEmailInvalide() {
        UserEntity u = UserEntity.builder().nom("Test").prenom("User")
                .email("email-invalide").build();
        when(userRepository.existsByEmailAndDeletedFalse("email-invalide")).thenReturn(false);

        assertThatThrownBy(() -> userService.creerUtilisateur(u))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("creerUtilisateur() — crée l'utilisateur et assigne le tenant")
    void creerUtilisateur_succes() {
        when(userRepository.existsByEmailAndDeletedFalse("amadou@example.com")).thenReturn(false);
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(userRepository.save(any())).thenReturn(utilisateur);

        UserEntity resultat = userService.creerUtilisateur(utilisateur);

        assertThat(resultat).isNotNull();
        verify(userRepository).save(any());
    }

    // =========================================================
    // supprimerUtilisateur
    // =========================================================

    @Test
    @DisplayName("supprimerUtilisateur() — supprime si l'utilisateur existe")
    void supprimerUtilisateur_succes() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.supprimerUtilisateur(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("supprimerUtilisateur() — lève exception si non trouvé")
    void supprimerUtilisateur_leveException() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> userService.supprimerUtilisateur(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }

    // =========================================================
    // modifierUtilisateur
    // =========================================================

    @Test
    @DisplayName("modifierUtilisateur() — modifie et sauvegarde l'utilisateur")
    void modifierUtilisateur_succes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(userRepository.save(any())).thenReturn(utilisateur);

        UserEntity resultat = userService.modifierUtilisateur(1L, utilisateur);

        assertThat(resultat).isNotNull();
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("modifierUtilisateur() — lève exception si utilisateur non trouvé")
    void modifierUtilisateur_leveExceptionSiAbsent() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.modifierUtilisateur(999L, utilisateur))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }
}
