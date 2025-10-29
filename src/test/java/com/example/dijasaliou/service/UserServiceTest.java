package com.example.dijasaliou.service;

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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = UserEntity.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Amadou")
                .email("amadou@example.com")
                .motDePasse("password")
                .role(UserEntity.Role.USER)
                .build();
    }

    @Test
    @DisplayName("obtenirTousLesUtilisateurs() - Devrait retourner tous les utilisateurs")
    void obtenirTousLesUtilisateurs_DevraitRetourner() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(utilisateur));

        List<UserEntity> resultat = userService.obtenirTousLesUtilisateurs();

        assertThat(resultat).hasSize(1);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("obtenirUtilisateurParId() - Devrait retourner un utilisateur")
    void obtenirUtilisateurParId_DevraitRetourner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        UserEntity resultat = userService.obtenirUtilisateurParId(1L);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("obtenirUtilisateurParId() - Devrait lancer exception si non trouvé")
    void obtenirUtilisateurParId_DevraitLancerException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.obtenirUtilisateurParId(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }

    @Test
    @DisplayName("obtenirUtilisateurParEmail() - Devrait retourner un utilisateur")
    void obtenirUtilisateurParEmail_DevraitRetourner() {
        when(userRepository.findByEmail("amadou@example.com"))
                .thenReturn(Optional.of(utilisateur));

        UserEntity resultat = userService.obtenirUtilisateurParEmail("amadou@example.com");

        assertThat(resultat).isNotNull();
        assertThat(resultat.getEmail()).isEqualTo("amadou@example.com");
    }

    @Test
    @DisplayName("creerUtilisateur() - Devrait créer un utilisateur")
    void creerUtilisateur_DevraitCreer() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(utilisateur);

        UserEntity resultat = userService.creerUtilisateur(utilisateur);

        assertThat(resultat).isNotNull();
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("creerUtilisateur() - Devrait lancer exception si email existe")
    void creerUtilisateur_DevraitLancerExceptionEmailExiste() {
        when(userRepository.existsByEmail("amadou@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.creerUtilisateur(utilisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email existe déjà");
    }

    @Test
    @DisplayName("creerUtilisateur() - Devrait lancer exception si nom vide")
    void creerUtilisateur_DevraitLancerExceptionNomVide() {
        UserEntity utilisateurInvalide = UserEntity.builder()
                .nom("")
                .prenom("Test")
                .email("test@example.com")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.creerUtilisateur(utilisateurInvalide))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nom");
    }

    @Test
    @DisplayName("creerUtilisateur() - Devrait lancer exception si email invalide")
    void creerUtilisateur_DevraitLancerExceptionEmailInvalide() {
        UserEntity utilisateurInvalide = UserEntity.builder()
                .nom("Test")
                .prenom("User")
                .email("email-invalide")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.creerUtilisateur(utilisateurInvalide))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("supprimerUtilisateur() - Devrait supprimer un utilisateur")
    void supprimerUtilisateur_DevraitSupprimer() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.supprimerUtilisateur(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("supprimerUtilisateur() - Devrait lancer exception si non trouvé")
    void supprimerUtilisateur_DevraitLancerException() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> userService.supprimerUtilisateur(999L))
                .isInstanceOf(RuntimeException.class);
    }
}
