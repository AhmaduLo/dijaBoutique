package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour UserController
 *
 * Bonnes pratiques appliquées :
 * - @WebMvcTest : Test uniquement la couche controller
 * - @MockitoBean : Mock les dépendances (services)
 * - MockMvc : Simule les appels HTTP
 * - @DisplayName : Descriptions claires des tests
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - Vérification des status HTTP et du contenu JSON
 */
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    // Mocker les beans de sécurité pour éviter les problèmes de dépendances
    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    private UserEntity utilisateurTest1;
    private UserEntity utilisateurTest2;
    private UserEntity adminTest;

    /**
     * Initialisation des données de test avant chaque test
     */
    @BeforeEach
    void setUp() {
        // Utilisateur 1
        utilisateurTest1 = UserEntity.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Amadou")
                .email("amadou@example.com")
                .motDePasse("password123")
                .role(UserEntity.Role.USER)
                .build();

        // Utilisateur 2
        utilisateurTest2 = UserEntity.builder()
                .id(2L)
                .nom("Ndiaye")
                .prenom("Fatou")
                .email("fatou@example.com")
                .motDePasse("password456")
                .role(UserEntity.Role.USER)
                .build();

        // Admin
        adminTest = UserEntity.builder()
                .id(3L)
                .nom("Sow")
                .prenom("Moussa")
                .email("admin@example.com")
                .motDePasse("admin123")
                .role(UserEntity.Role.ADMIN)
                .build();
    }

    // ==================== Tests pour GET /utilisateurs ====================

    @Test
    @DisplayName("GET /utilisateurs - Devrait retourner tous les utilisateurs")
    void obtenirTous_DevraitRetournerTousLesUtilisateurs() throws Exception {
        // Arrange
        List<UserEntity> utilisateurs = Arrays.asList(utilisateurTest1, utilisateurTest2, adminTest);
        when(userService.obtenirTousLesUtilisateurs()).thenReturn(utilisateurs);

        // Act & Assert
        mockMvc.perform(get("/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].nom", is("Diop")))
                .andExpect(jsonPath("$[0].prenom", is("Amadou")))
                .andExpect(jsonPath("$[0].email", is("amadou@example.com")))
                .andExpect(jsonPath("$[0].role", is("USER")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[2].role", is("ADMIN")));

        verify(userService, times(1)).obtenirTousLesUtilisateurs();
    }

    @Test
    @DisplayName("GET /utilisateurs - Devrait retourner une liste vide quand aucun utilisateur")
    void obtenirTous_DevraitRetournerListeVide() throws Exception {
        // Arrange
        when(userService.obtenirTousLesUtilisateurs()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(userService, times(1)).obtenirTousLesUtilisateurs();
    }

    // ==================== Tests pour GET /utilisateurs/{id} ====================

    @Test
    @DisplayName("GET /utilisateurs/{id} - Devrait retourner un utilisateur par son ID")
    void obtenirParId_DevraitRetournerUtilisateur() throws Exception {
        // Arrange
        Long userId = 1L;
        when(userService.obtenirUtilisateurParId(userId)).thenReturn(utilisateurTest1);

        // Act & Assert
        mockMvc.perform(get("/utilisateurs/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nom", is("Diop")))
                .andExpect(jsonPath("$.prenom", is("Amadou")))
                .andExpect(jsonPath("$.email", is("amadou@example.com")))
                .andExpect(jsonPath("$.role", is("USER")));

        verify(userService, times(1)).obtenirUtilisateurParId(userId);
    }

    @Test
    @DisplayName("GET /utilisateurs/{id} - Devrait retourner un admin")
    void obtenirParId_DevraitRetournerAdmin() throws Exception {
        // Arrange
        Long adminId = 3L;
        when(userService.obtenirUtilisateurParId(adminId)).thenReturn(adminTest);

        // Act & Assert
        mockMvc.perform(get("/utilisateurs/{id}", adminId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.role", is("ADMIN")));

        verify(userService, times(1)).obtenirUtilisateurParId(adminId);
    }

    // ==================== Tests pour GET /utilisateurs/email/{email} ====================

    @Test
    @DisplayName("GET /utilisateurs/email/{email} - Devrait retourner un utilisateur par email")
    void obtenirParEmail_DevraitRetournerUtilisateur() throws Exception {
        // Arrange
        String email = "amadou@example.com";
        when(userService.obtenirUtilisateurParEmail(email)).thenReturn(utilisateurTest1);

        // Act & Assert
        mockMvc.perform(get("/utilisateurs/email/{email}", email)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.nom", is("Diop")));

        verify(userService, times(1)).obtenirUtilisateurParEmail(email);
    }

    // ==================== Tests pour GET /utilisateurs/moi ====================

    @Test
    @DisplayName("GET /utilisateurs/moi - Devrait retourner le profil de l'utilisateur connecté")
    void obtenirMonProfil_DevraitRetournerProfilConnecte() throws Exception {
        // Arrange
        String email = "amadou@example.com";
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        when(userService.obtenirUtilisateurParEmail(email)).thenReturn(utilisateurTest1);

        // Act & Assert
        mockMvc.perform(get("/utilisateurs/moi")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.nom", is("Diop")))
                .andExpect(jsonPath("$.prenom", is("Amadou")));

        verify(userService, times(1)).obtenirUtilisateurParEmail(email);
    }

    // ==================== Tests pour POST /utilisateurs (DÉSACTIVÉ) ====================

    @Test
    @DisplayName("POST /utilisateurs - Devrait retourner 403 FORBIDDEN (création réservée aux admins)")
    void creer_DevraitRetourner403Forbidden() throws Exception {
        // Arrange
        UserEntity nouvelUtilisateur = UserEntity.builder()
                .nom("Test")
                .prenom("User")
                .email("test@example.com")
                .motDePasse("password")
                .role(UserEntity.Role.USER)
                .build();

        // Act & Assert
        mockMvc.perform(post("/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nouvelUtilisateur)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("réservée aux administrateurs")))
                .andExpect(jsonPath("$.endpoint", containsString("/api/admin/utilisateurs")));

        // Le service ne devrait jamais être appelé
        verify(userService, never()).creerUtilisateur(any());
    }

    // ==================== Tests pour PUT /utilisateurs/{id} (DÉSACTIVÉ) ====================

    @Test
    @DisplayName("PUT /utilisateurs/{id} - Devrait retourner 403 FORBIDDEN (modification réservée aux admins)")
    void modifier_DevraitRetourner403Forbidden() throws Exception {
        // Arrange
        Long userId = 1L;
        UserEntity utilisateurModifie = UserEntity.builder()
                .nom("Diop Modifié")
                .prenom("Amadou")
                .email("amadou@example.com")
                .build();

        // Act & Assert
        mockMvc.perform(put("/utilisateurs/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(utilisateurModifie)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("réservée aux administrateurs")))
                .andExpect(jsonPath("$.endpoint", containsString("/api/admin/utilisateurs/{id}")));

        // Le service ne devrait jamais être appelé
        verify(userService, never()).modifierUtilisateur(any(), any());
    }

    // ==================== Tests pour DELETE /utilisateurs/{id} (DÉSACTIVÉ) ====================

    @Test
    @DisplayName("DELETE /utilisateurs/{id} - Devrait retourner 403 FORBIDDEN (suppression réservée aux admins)")
    void supprimer_DevraitRetourner403Forbidden() throws Exception {
        // Arrange
        Long userId = 1L;

        // Act & Assert
        mockMvc.perform(delete("/utilisateurs/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("réservée aux administrateurs")))
                .andExpect(jsonPath("$.endpoint", containsString("/api/admin/utilisateurs/{id}")));

        // Le service ne devrait jamais être appelé
        verify(userService, never()).supprimerUtilisateur(any());
    }

    // ==================== Tests additionnels ====================

    @Test
    @DisplayName("GET /utilisateurs - Ne devrait pas exposer les mots de passe")
    void obtenirTous_NePasExposerMotsDePasse() throws Exception {
        // Arrange
        when(userService.obtenirTousLesUtilisateurs()).thenReturn(Arrays.asList(utilisateurTest1));

        // Act & Assert
        mockMvc.perform(get("/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motDePasse").doesNotExist()); // Le DTO ne devrait pas inclure le mot de passe

        verify(userService, times(1)).obtenirTousLesUtilisateurs();
    }

    @Test
    @DisplayName("GET /utilisateurs/{id} - Devrait gérer les utilisateurs avec différents rôles")
    void obtenirParId_DevraitGererDifferentsRoles() throws Exception {
        // Arrange - Tester avec un USER
        when(userService.obtenirUtilisateurParId(1L)).thenReturn(utilisateurTest1);

        // Act & Assert - USER
        mockMvc.perform(get("/utilisateurs/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("USER")));

        // Arrange - Tester avec un ADMIN
        when(userService.obtenirUtilisateurParId(3L)).thenReturn(adminTest);

        // Act & Assert - ADMIN
        mockMvc.perform(get("/utilisateurs/{id}", 3L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("ADMIN")));

        verify(userService, times(1)).obtenirUtilisateurParId(1L);
        verify(userService, times(1)).obtenirUtilisateurParId(3L);
    }
}
