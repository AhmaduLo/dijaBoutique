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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour CustomUserDetailsService
 *
 * Bonnes pratiques appliquées :
 * - @ExtendWith(MockitoExtension.class) : Active Mockito pour les tests
 * - @Mock : Créer des mocks des dépendances
 * - @InjectMocks : Injecter les mocks dans le service testé
 * - AssertJ : Assertions fluides et lisibles
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - @DisplayName : Descriptions claires en français
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    // Mock du repository des utilisateurs
    @Mock
    private UserRepository userRepository;

    // Service à tester avec les mocks injectés
    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    // Données de test
    private UserEntity utilisateurUser;
    private UserEntity utilisateurAdmin;

    /**
     * Initialisation des données de test avant chaque test
     *
     * Scénario :
     * - Utilisateur standard (USER) : amadou@example.com
     * - Administrateur (ADMIN) : admin@example.com
     */
    @BeforeEach
    void setUp() {
        // Utilisateur avec rôle USER
        utilisateurUser = UserEntity.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Amadou")
                .email("amadou@example.com")
                .motDePasse("$2a$10$hashedPassword123")
                .role(UserEntity.Role.USER)
                .build();

        // Utilisateur avec rôle ADMIN
        utilisateurAdmin = UserEntity.builder()
                .id(2L)
                .nom("Sow")
                .prenom("Fatou")
                .email("admin@example.com")
                .motDePasse("$2a$10$hashedAdminPassword456")
                .role(UserEntity.Role.ADMIN)
                .build();
    }

    // ==================== Tests pour loadUserByUsername() ====================

    /**
     * Test : Charger un utilisateur USER par son email
     *
     * Scénario :
     * - Rechercher utilisateur avec email amadou@example.com
     * - L'utilisateur existe avec rôle USER
     * - Doit retourner UserDetails avec les bonnes informations
     */
    @Test
    @DisplayName("loadUserByUsername() - Devrait charger un utilisateur USER avec succès")
    void loadUserByUsername_DevraitChargerUtilisateurUser() {
        // Arrange : Préparer le mock du repository
        String email = "amadou@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(utilisateurUser));

        // Act : Charger l'utilisateur
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert : Vérifier les informations de l'utilisateur
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword123");

        // Vérifier les autorités (rôles)
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities)
                .isNotNull()
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("USER");

        verify(userRepository, times(1)).findByEmail(email);
    }

    /**
     * Test : Charger un utilisateur ADMIN par son email
     *
     * Scénario :
     * - Rechercher utilisateur avec email admin@example.com
     * - L'utilisateur existe avec rôle ADMIN
     * - Doit retourner UserDetails avec autorité ADMIN
     */
    @Test
    @DisplayName("loadUserByUsername() - Devrait charger un utilisateur ADMIN avec succès")
    void loadUserByUsername_DevraitChargerUtilisateurAdmin() {
        // Arrange : Préparer le mock du repository
        String email = "admin@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(utilisateurAdmin));

        // Act : Charger l'utilisateur
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert : Vérifier les informations de l'administrateur
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedAdminPassword456");

        // Vérifier que l'autorité ADMIN est assignée
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities)
                .isNotNull()
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ADMIN");

        verify(userRepository, times(1)).findByEmail(email);
    }

    /**
     * Test : Lancer exception si utilisateur non trouvé
     *
     * Scénario :
     * - Rechercher utilisateur avec email inexistant
     * - Le repository retourne Optional.empty()
     * - Doit lancer UsernameNotFoundException
     */
    @Test
    @DisplayName("loadUserByUsername() - Devrait lancer UsernameNotFoundException si utilisateur non trouvé")
    void loadUserByUsername_DevraitLancerExceptionSiUtilisateurNonTrouve() {
        // Arrange : Email inexistant
        String emailInexistant = "inexistant@example.com";
        when(userRepository.findByEmail(emailInexistant)).thenReturn(Optional.empty());

        // Act & Assert : Vérifier l'exception
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(emailInexistant))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Utilisateur non trouvé : " + emailInexistant);

        verify(userRepository, times(1)).findByEmail(emailInexistant);
    }

    /**
     * Test : Vérifier que le UserDetails retourné est correctement formaté pour Spring Security
     *
     * Scénario :
     * - Spring Security utilise UserDetails pour l'authentification
     * - On vérifie que tous les champs requis sont présents
     */
    @Test
    @DisplayName("loadUserByUsername() - Devrait retourner un UserDetails valide pour Spring Security")
    void loadUserByUsername_DevraitRetournerUserDetailsValide() {
        // Arrange
        String email = "amadou@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(utilisateurUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert : Vérifier que UserDetails est valide pour Spring Security
        assertThat(userDetails.getUsername()).isNotBlank();
        assertThat(userDetails.getPassword()).isNotBlank();
        assertThat(userDetails.getAuthorities()).isNotEmpty();

        // Vérifier que les méthodes booléennes retournent des valeurs par défaut de Spring Security
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();

        verify(userRepository, times(1)).findByEmail(email);
    }

    /**
     * Test : Vérifier que le format des autorités est correct (sans préfixe ROLE_)
     *
     * Scénario :
     * - Les autorités doivent être au format "USER" ou "ADMIN"
     * - SANS le préfixe "ROLE_" (car on utilise hasAuthority() dans SecurityConfig)
     */
    @Test
    @DisplayName("loadUserByUsername() - Les autorités ne doivent pas avoir le préfixe ROLE_")
    void loadUserByUsername_AutoriteSansPrexiseRole() {
        // Arrange
        String email = "amadou@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(utilisateurUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert : Vérifier que l'autorité est "USER" et non "ROLE_USER"
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("USER")
                .doesNotContain("ROLE_USER");

        verify(userRepository, times(1)).findByEmail(email);
    }

    /**
     * Test : Vérifier le comportement avec un email null
     *
     * Scénario :
     * - Email null ne devrait jamais arriver en production
     * - Mais le test vérifie le comportement défensif
     */
    @Test
    @DisplayName("loadUserByUsername() - Devrait gérer un email null")
    void loadUserByUsername_DevraitGererEmailNull() {
        // Arrange
        String emailNull = null;
        when(userRepository.findByEmail(emailNull)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(emailNull))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository, times(1)).findByEmail(emailNull);
    }

    /**
     * Test : Vérifier le comportement avec un email vide
     *
     * Scénario :
     * - Email vide ne devrait jamais arriver en production
     * - Mais le test vérifie le comportement défensif
     */
    @Test
    @DisplayName("loadUserByUsername() - Devrait gérer un email vide")
    void loadUserByUsername_DevraitGererEmailVide() {
        // Arrange
        String emailVide = "";
        when(userRepository.findByEmail(emailVide)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(emailVide))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository, times(1)).findByEmail(emailVide);
    }

    /**
     * Test : Vérifier que le mot de passe retourné est bien celui de l'entité
     *
     * Scénario :
     * - Le UserDetails doit contenir le mot de passe hashé de l'utilisateur
     * - Spring Security l'utilisera pour comparer avec le mot de passe fourni lors du login
     */
    @Test
    @DisplayName("loadUserByUsername() - Le mot de passe doit correspondre à celui de l'entité")
    void loadUserByUsername_MotDePasseCorrespond() {
        // Arrange
        String email = "amadou@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(utilisateurUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert : Vérifier que le mot de passe est bien celui de l'entité
        assertThat(userDetails.getPassword()).isEqualTo(utilisateurUser.getMotDePasse());

        verify(userRepository, times(1)).findByEmail(email);
    }

    /**
     * Test : Vérifier que le username retourné est bien l'email de l'utilisateur
     *
     * Scénario :
     * - Spring Security utilise le username pour identifier l'utilisateur
     * - Dans notre cas, le username est l'email
     */
    @Test
    @DisplayName("loadUserByUsername() - Le username doit correspondre à l'email")
    void loadUserByUsername_UsernameCorrespondEmail() {
        // Arrange
        String email = "amadou@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(utilisateurUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert : Vérifier que le username est bien l'email
        assertThat(userDetails.getUsername()).isEqualTo(utilisateurUser.getEmail());

        verify(userRepository, times(1)).findByEmail(email);
    }
}
