package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.dto.UserDto;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.AdminService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour AdminController
 *
 * Bonnes pratiques appliquées :
 * - @WebMvcTest : Test uniquement la couche controller
 * - @MockitoBean : Mock les dépendances (services)
 * - MockMvc : Simule les appels HTTP
 * - @DisplayName : Descriptions claires des tests
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - Vérification des status HTTP et du contenu JSON
 */
@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du AdminController")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    // Mocker les beans de sécurité pour éviter les problèmes de dépendances
    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    private Authentication authentication;
    private String emailAdmin;
    private UserDto utilisateurDto1;
    private UserDto utilisateurDto2;
    private UserDto adminDto;

    /**
     * Initialisation des données de test avant chaque test
     */
    @BeforeEach
    void setUp() {
        emailAdmin = "admin@example.com";
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(emailAdmin);

        // Utilisateur DTO 1
        utilisateurDto1 = UserDto.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Amadou")
                .email("amadou@example.com")
                .role(UserEntity.Role.USER)
                .build();

        // Utilisateur DTO 2
        utilisateurDto2 = UserDto.builder()
                .id(2L)
                .nom("Ndiaye")
                .prenom("Fatou")
                .email("fatou@example.com")
                .role(UserEntity.Role.USER)
                .build();

        // Admin DTO
        adminDto = UserDto.builder()
                .id(3L)
                .nom("Sow")
                .prenom("Moussa")
                .email(emailAdmin)
                .role(UserEntity.Role.ADMIN)
                .build();
    }

    // ==================== Tests pour GET /admin/utilisateurs ====================

    @Test
    @DisplayName("GET /admin/utilisateurs - Devrait retourner tous les utilisateurs")
    void obtenirTousLesUtilisateurs_DevraitRetournerTous() throws Exception {
        // Arrange
        List<UserDto> utilisateurs = Arrays.asList(utilisateurDto1, utilisateurDto2, adminDto);
        when(adminService.obtenirTousLesUtilisateurs(emailAdmin)).thenReturn(utilisateurs);

        // Act & Assert
        mockMvc.perform(get("/admin/utilisateurs")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].nom", is("Diop")))
                .andExpect(jsonPath("$[0].email", is("amadou@example.com")))
                .andExpect(jsonPath("$[0].role", is("USER")))
                .andExpect(jsonPath("$[2].role", is("ADMIN")));

        verify(adminService, times(1)).obtenirTousLesUtilisateurs(emailAdmin);
    }

    @Test
    @DisplayName("GET /admin/utilisateurs - Devrait retourner liste vide si aucun utilisateur")
    void obtenirTousLesUtilisateurs_DevraitRetournerListeVide() throws Exception {
        // Arrange
        when(adminService.obtenirTousLesUtilisateurs(emailAdmin)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/admin/utilisateurs")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(adminService, times(1)).obtenirTousLesUtilisateurs(emailAdmin);
    }

    // ==================== Tests pour GET /admin/utilisateurs/{id} ====================

    @Test
    @DisplayName("GET /admin/utilisateurs/{id} - Devrait retourner un utilisateur par ID")
    void obtenirUtilisateurParId_DevraitRetournerUtilisateur() throws Exception {
        // Arrange
        Long userId = 1L;
        when(adminService.obtenirUtilisateurParId(userId, emailAdmin)).thenReturn(utilisateurDto1);

        // Act & Assert
        mockMvc.perform(get("/admin/utilisateurs/{id}", userId)
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nom", is("Diop")))
                .andExpect(jsonPath("$.email", is("amadou@example.com")));

        verify(adminService, times(1)).obtenirUtilisateurParId(userId, emailAdmin);
    }

    // ==================== Tests pour POST /admin/utilisateurs ====================

    @Test
    @DisplayName("POST /admin/utilisateurs - Devrait créer un nouvel utilisateur")
    void creerUtilisateur_DevraitCreerNouvelUtilisateur() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setNom("Ba");
        request.setPrenom("Aissatou");
        request.setEmail("aissatou@example.com");
        request.setMotDePasse("password123");
        request.setRole(UserEntity.Role.USER);

        UserDto nouveauUtilisateur = UserDto.builder()
                .id(4L)
                .nom("Ba")
                .prenom("Aissatou")
                .email("aissatou@example.com")
                .role(UserEntity.Role.USER)
                .build();

        when(adminService.creerUtilisateur(any(RegisterRequest.class), eq(emailAdmin)))
                .thenReturn(nouveauUtilisateur);

        // Act & Assert
        mockMvc.perform(post("/admin/utilisateurs")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(4)))
                .andExpect(jsonPath("$.nom", is("Ba")))
                .andExpect(jsonPath("$.prenom", is("Aissatou")))
                .andExpect(jsonPath("$.email", is("aissatou@example.com")))
                .andExpect(jsonPath("$.role", is("USER")));

        verify(adminService, times(1)).creerUtilisateur(any(RegisterRequest.class), eq(emailAdmin));
    }

    @Test
    @DisplayName("POST /admin/utilisateurs - Devrait créer un admin")
    void creerUtilisateur_DevraitCreerAdmin() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setNom("Fall");
        request.setPrenom("Omar");
        request.setEmail("omar@example.com");
        request.setMotDePasse("admin123");
        request.setRole(UserEntity.Role.ADMIN);

        UserDto nouvelAdmin = UserDto.builder()
                .id(5L)
                .nom("Fall")
                .prenom("Omar")
                .email("omar@example.com")
                .role(UserEntity.Role.ADMIN)
                .build();

        when(adminService.creerUtilisateur(any(RegisterRequest.class), eq(emailAdmin)))
                .thenReturn(nouvelAdmin);

        // Act & Assert
        mockMvc.perform(post("/admin/utilisateurs")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("ADMIN")));

        verify(adminService, times(1)).creerUtilisateur(any(RegisterRequest.class), eq(emailAdmin));
    }

    // ==================== Tests pour PUT /admin/utilisateurs/{id} ====================

    @Test
    @DisplayName("PUT /admin/utilisateurs/{id} - Devrait modifier un utilisateur")
    void modifierUtilisateur_DevraitModifierUtilisateur() throws Exception {
        // Arrange
        Long userId = 1L;
        Map<String, Object> updates = new HashMap<>();
        updates.put("nom", "Diop Modifié");
        updates.put("prenom", "Amadou Modifié");

        UserDto utilisateurModifie = UserDto.builder()
                .id(1L)
                .nom("Diop Modifié")
                .prenom("Amadou Modifié")
                .email("amadou@example.com")
                .role(UserEntity.Role.USER)
                .build();

        when(adminService.modifierUtilisateur(eq(userId), anyMap(), eq(emailAdmin)))
                .thenReturn(utilisateurModifie);

        // Act & Assert
        mockMvc.perform(put("/admin/utilisateurs/{id}", userId)
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nom", is("Diop Modifié")))
                .andExpect(jsonPath("$.prenom", is("Amadou Modifié")));

        verify(adminService, times(1)).modifierUtilisateur(eq(userId), anyMap(), eq(emailAdmin));
    }

    // ==================== Tests pour DELETE /admin/utilisateurs/{id} ====================

    @Test
    @DisplayName("DELETE /admin/utilisateurs/{id} - Devrait supprimer un utilisateur")
    void supprimerUtilisateur_DevraitSupprimerUtilisateur() throws Exception {
        // Arrange
        Long userId = 1L;
        doNothing().when(adminService).supprimerUtilisateur(userId, emailAdmin);

        // Act & Assert
        mockMvc.perform(delete("/admin/utilisateurs/{id}", userId)
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Utilisateur supprimé avec succès")));

        verify(adminService, times(1)).supprimerUtilisateur(userId, emailAdmin);
    }

    @Test
    @DisplayName("DELETE /admin/utilisateurs/{id} - Devrait empêcher la suppression de son propre compte")
    void supprimerUtilisateur_DevraitEmpecherSuppressionPropreCompte() throws Exception {
        // Arrange
        Long adminId = 3L;
        doThrow(new RuntimeException("Impossible de supprimer votre propre compte"))
                .when(adminService).supprimerUtilisateur(adminId, emailAdmin);

        // Act & Assert
        try {
            mockMvc.perform(delete("/admin/utilisateurs/{id}", adminId)
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            // Exception attendue
        }

        verify(adminService, times(1)).supprimerUtilisateur(adminId, emailAdmin);
    }

    // ==================== Tests pour GET /admin/statistiques ====================

    @Test
    @DisplayName("GET /admin/statistiques - Devrait retourner les statistiques")
    void obtenirStatistiques_DevraitRetournerStatistiques() throws Exception {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("nombreTotalUtilisateurs", 10);
        stats.put("nombreUsers", 8);
        stats.put("nombreAdmins", 2);
        stats.put("dernierInscrit", "amadou@example.com");

        when(adminService.obtenirStatistiques(emailAdmin)).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/admin/statistiques")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotalUtilisateurs", is(10)))
                .andExpect(jsonPath("$.nombreUsers", is(8)))
                .andExpect(jsonPath("$.nombreAdmins", is(2)))
                .andExpect(jsonPath("$.dernierInscrit", is("amadou@example.com")));

        verify(adminService, times(1)).obtenirStatistiques(emailAdmin);
    }

    // ==================== Tests pour PUT /admin/utilisateurs/{id}/role ====================

    @Test
    @DisplayName("PUT /admin/utilisateurs/{id}/role - Devrait promouvoir un USER en ADMIN")
    void modifierRole_DevraitPromouvoirUserEnAdmin() throws Exception {
        // Arrange
        Long userId = 1L;
        Map<String, String> roleData = new HashMap<>();
        roleData.put("role", "ADMIN");

        UserDto utilisateurPromu = UserDto.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Amadou")
                .email("amadou@example.com")
                .role(UserEntity.Role.ADMIN)
                .build();

        when(adminService.modifierRole(userId, "ADMIN", emailAdmin)).thenReturn(utilisateurPromu);

        // Act & Assert
        mockMvc.perform(put("/admin/utilisateurs/{id}/role", userId)
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.role", is("ADMIN")));

        verify(adminService, times(1)).modifierRole(userId, "ADMIN", emailAdmin);
    }

    @Test
    @DisplayName("PUT /admin/utilisateurs/{id}/role - Devrait rétrograder un ADMIN en USER")
    void modifierRole_DevraitRetrograderAdminEnUser() throws Exception {
        // Arrange
        Long adminId = 3L;
        Map<String, String> roleData = new HashMap<>();
        roleData.put("role", "USER");

        UserDto utilisateurRetrograde = UserDto.builder()
                .id(3L)
                .nom("Sow")
                .prenom("Moussa")
                .email("admin2@example.com")
                .role(UserEntity.Role.USER)
                .build();

        when(adminService.modifierRole(adminId, "USER", emailAdmin)).thenReturn(utilisateurRetrograde);

        // Act & Assert
        mockMvc.perform(put("/admin/utilisateurs/{id}/role", adminId)
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.role", is("USER")));

        verify(adminService, times(1)).modifierRole(adminId, "USER", emailAdmin);
    }

    // ==================== Tests additionnels ====================

    @Test
    @DisplayName("POST /admin/utilisateurs - Ne devrait pas exposer le mot de passe dans la réponse")
    void creerUtilisateur_NePasExposerMotDePasse() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setNom("Test");
        request.setPrenom("User");
        request.setEmail("test@example.com");
        request.setMotDePasse("secretPassword123");
        request.setRole(UserEntity.Role.USER);

        UserDto nouveauUtilisateur = UserDto.builder()
                .id(6L)
                .nom("Test")
                .prenom("User")
                .email("test@example.com")
                .role(UserEntity.Role.USER)
                .build();

        when(adminService.creerUtilisateur(any(RegisterRequest.class), eq(emailAdmin)))
                .thenReturn(nouveauUtilisateur);

        // Act & Assert
        mockMvc.perform(post("/admin/utilisateurs")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motDePasse").doesNotExist()); // Le DTO ne devrait pas inclure le mot de passe

        verify(adminService, times(1)).creerUtilisateur(any(RegisterRequest.class), eq(emailAdmin));
    }

    @Test
    @DisplayName("PUT /admin/utilisateurs/{id} - Devrait permettre de modifier plusieurs champs")
    void modifierUtilisateur_DevraitModifierPlusieursChamps() throws Exception {
        // Arrange
        Long userId = 1L;
        Map<String, Object> updates = new HashMap<>();
        updates.put("nom", "Nouveau Nom");
        updates.put("prenom", "Nouveau Prenom");
        updates.put("email", "nouveau@example.com");

        UserDto utilisateurModifie = UserDto.builder()
                .id(1L)
                .nom("Nouveau Nom")
                .prenom("Nouveau Prenom")
                .email("nouveau@example.com")
                .role(UserEntity.Role.USER)
                .build();

        when(adminService.modifierUtilisateur(eq(userId), anyMap(), eq(emailAdmin)))
                .thenReturn(utilisateurModifie);

        // Act & Assert
        mockMvc.perform(put("/admin/utilisateurs/{id}", userId)
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom", is("Nouveau Nom")))
                .andExpect(jsonPath("$.prenom", is("Nouveau Prenom")))
                .andExpect(jsonPath("$.email", is("nouveau@example.com")));

        verify(adminService, times(1)).modifierUtilisateur(eq(userId), anyMap(), eq(emailAdmin));
    }
}
