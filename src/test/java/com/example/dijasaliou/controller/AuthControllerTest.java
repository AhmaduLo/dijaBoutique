package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.AuthResponse;
import com.example.dijasaliou.dto.LoginRequest;
import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour AuthController
 *
 * Bonnes pratiques appliquées :
 * - @WebMvcTest : Test uniquement la couche controller
 * - @MockitoBean : Mock les dépendances (services)
 * - MockMvc : Simule les appels HTTP
 * - @DisplayName : Descriptions claires des tests
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - Vérification des status HTTP et du contenu JSON
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // Mocker les beans de sécurité pour éviter les problèmes de dépendances
    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    private RegisterRequest registerRequestUser;
    private RegisterRequest registerRequestAdmin;
    private LoginRequest loginRequest;
    private AuthResponse authResponseUser;
    private AuthResponse authResponseAdmin;
    private UserEntity utilisateurTest;
    private UserEntity adminTest;

    /**
     * Initialisation des données de test avant chaque test
     */
    @BeforeEach
    void setUp() {
        // Utilisateur test
        utilisateurTest = UserEntity.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Amadou")
                .email("amadou@example.com")
                .motDePasse("encodedPassword123")
                .role(UserEntity.Role.USER)
                .build();

        // Admin test
        adminTest = UserEntity.builder()
                .id(2L)
                .nom("Saliou")
                .prenom("Dija")
                .email("dija@boutique.com")
                .motDePasse("encodedPassword456")
                .role(UserEntity.Role.ADMIN)
                .build();

        // Requête d'inscription USER
        registerRequestUser = new RegisterRequest();
        registerRequestUser.setNom("Diop");
        registerRequestUser.setPrenom("Amadou");
        registerRequestUser.setEmail("amadou@example.com");
        registerRequestUser.setMotDePasse("password123");
        registerRequestUser.setRole(UserEntity.Role.USER);

        // Requête d'inscription ADMIN
        registerRequestAdmin = new RegisterRequest();
        registerRequestAdmin.setNom("Saliou");
        registerRequestAdmin.setPrenom("Dija");
        registerRequestAdmin.setEmail("dija@boutique.com");
        registerRequestAdmin.setMotDePasse("password456");
        registerRequestAdmin.setRole(UserEntity.Role.ADMIN);

        // Requête de connexion
        loginRequest = new LoginRequest("amadou@example.com", "password123");

        // Réponse d'authentification USER
        authResponseUser = AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.user.token")
                .user(utilisateurTest)
                .build();

        // Réponse d'authentification ADMIN
        authResponseAdmin = AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin.token")
                .user(adminTest)
                .build();
    }

    // ==================== Tests pour POST /auth/register ====================

    @Test
    @DisplayName("POST /auth/register - Devrait inscrire un nouvel utilisateur USER")
    void register_DevraitInscrireNouvelUtilisateurUser() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponseUser);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequestUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.user.token")))
                .andExpect(jsonPath("$.user.id", is(1)))
                .andExpect(jsonPath("$.user.nom", is("Diop")))
                .andExpect(jsonPath("$.user.prenom", is("Amadou")))
                .andExpect(jsonPath("$.user.email", is("amadou@example.com")))
                .andExpect(jsonPath("$.user.role", is("USER")));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Devrait inscrire un nouvel utilisateur ADMIN")
    void register_DevraitInscrireNouvelUtilisateurAdmin() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponseAdmin);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequestAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin.token")))
                .andExpect(jsonPath("$.user.id", is(2)))
                .andExpect(jsonPath("$.user.nom", is("Saliou")))
                .andExpect(jsonPath("$.user.prenom", is("Dija")))
                .andExpect(jsonPath("$.user.email", is("dija@boutique.com")))
                .andExpect(jsonPath("$.user.role", is("ADMIN")));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Devrait retourner un token JWT valide")
    void register_DevraitRetournerTokenJwtValide() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponseUser);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequestUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.token", org.hamcrest.Matchers.startsWith("eyJ"))) // JWT commence toujours par eyJ
                .andExpect(jsonPath("$.user", notNullValue()));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Devrait rejeter une requête avec email invalide")
    void register_DevraitRejeterEmailInvalide() throws Exception {
        // Arrange
        RegisterRequest requestInvalide = new RegisterRequest();
        requestInvalide.setNom("Test");
        requestInvalide.setPrenom("User");
        requestInvalide.setEmail("email-invalide");
        requestInvalide.setMotDePasse("password");
        requestInvalide.setRole(UserEntity.Role.USER);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Email invalide"));

        // Act & Assert - L'exception devrait être lancée
        try {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvalide)));
        } catch (Exception e) {
            // Exception attendue
        }

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Devrait rejeter une requête avec email déjà existant")
    void register_DevraitRejeterEmailExistant() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Email déjà utilisé"));

        // Act & Assert - L'exception devrait être lancée
        try {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequestUser)));
        } catch (Exception e) {
            // Exception attendue
        }

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Devrait rejeter une requête avec mot de passe vide")
    void register_DevraitRejeterMotDePasseVide() throws Exception {
        // Arrange
        RegisterRequest requestSansMotDePasse = new RegisterRequest();
        requestSansMotDePasse.setNom("Test");
        requestSansMotDePasse.setPrenom("User");
        requestSansMotDePasse.setEmail("test@example.com");
        requestSansMotDePasse.setMotDePasse("");
        requestSansMotDePasse.setRole(UserEntity.Role.USER);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Mot de passe requis"));

        // Act & Assert - L'exception devrait être lancée
        try {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestSansMotDePasse)));
        } catch (Exception e) {
            // Exception attendue
        }

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Devrait rejeter une requête sans rôle")
    void register_DevraitRejeterSansRole() throws Exception {
        // Arrange
        RegisterRequest requestSansRole = new RegisterRequest();
        requestSansRole.setNom("Test");
        requestSansRole.setPrenom("User");
        requestSansRole.setEmail("test@example.com");
        requestSansRole.setMotDePasse("password");
        requestSansRole.setRole(null);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Rôle requis"));

        // Act & Assert - L'exception devrait être lancée
        try {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestSansRole)));
        } catch (Exception e) {
            // Exception attendue
        }

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    // ==================== Tests pour POST /auth/login ====================

    @Test
    @DisplayName("POST /auth/login - Devrait connecter un utilisateur avec des identifiants valides")
    void login_DevraitConnecterUtilisateurValide() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponseUser);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.user.token")))
                .andExpect(jsonPath("$.user.id", is(1)))
                .andExpect(jsonPath("$.user.email", is("amadou@example.com")))
                .andExpect(jsonPath("$.user.nom", is("Diop")))
                .andExpect(jsonPath("$.user.role", is("USER")));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Devrait connecter un admin avec des identifiants valides")
    void login_DevraitConnecterAdminValide() throws Exception {
        // Arrange
        LoginRequest loginRequestAdmin = new LoginRequest("dija@boutique.com", "password456");
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponseAdmin);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin.token")))
                .andExpect(jsonPath("$.user.id", is(2)))
                .andExpect(jsonPath("$.user.email", is("dija@boutique.com")))
                .andExpect(jsonPath("$.user.role", is("ADMIN")));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Devrait retourner un token JWT valide")
    void login_DevraitRetournerTokenJwtValide() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponseUser);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.token", org.hamcrest.Matchers.startsWith("eyJ")))
                .andExpect(jsonPath("$.user", notNullValue()));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Devrait rejeter des identifiants invalides")
    void login_DevraitRejeterIdentifiantsInvalides() throws Exception {
        // Arrange
        LoginRequest loginInvalide = new LoginRequest("amadou@example.com", "mauvais-password");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Identifiants invalides"));

        // Act & Assert - L'exception devrait être lancée
        try {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginInvalide)));
        } catch (Exception e) {
            // Exception attendue
        }

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Devrait rejeter un email inexistant")
    void login_DevraitRejeterEmailInexistant() throws Exception {
        // Arrange
        LoginRequest loginInexistant = new LoginRequest("inexistant@example.com", "password");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Utilisateur non trouvé"));

        // Act & Assert - L'exception devrait être lancée
        try {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginInexistant)));
        } catch (Exception e) {
            // Exception attendue
        }

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Devrait rejeter une requête avec email vide")
    void login_DevraitRejeterEmailVide() throws Exception {
        // Arrange
        LoginRequest loginSansEmail = new LoginRequest("", "password");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Email requis"));

        // Act & Assert - L'exception devrait être lancée
        try {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginSansEmail)));
        } catch (Exception e) {
            // Exception attendue
        }

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Devrait rejeter une requête avec mot de passe vide")
    void login_DevraitRejeterMotDePasseVide() throws Exception {
        // Arrange
        LoginRequest loginSansPassword = new LoginRequest("amadou@example.com", "");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Mot de passe requis"));

        // Act & Assert - L'exception devrait être lancée
        try {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginSansPassword)));
        } catch (Exception e) {
            // Exception attendue
        }

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    // ==================== Tests additionnels ====================

    @Test
    @DisplayName("POST /auth/register - Ne devrait pas exposer le mot de passe dans la réponse")
    void register_NePasExposerMotDePasse() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponseUser);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequestUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.motDePasse").doesNotExist()); // @JsonIgnore empêche l'exposition du mot de passe

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Ne devrait pas exposer le mot de passe dans la réponse")
    void login_NePasExposerMotDePasse() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponseUser);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.motDePasse").doesNotExist()); // @JsonIgnore empêche l'exposition du mot de passe

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Devrait accepter des caractères spéciaux dans le nom")
    void register_DevraitAccepterCaracteresSpeciauxDansNom() throws Exception {
        // Arrange
        RegisterRequest requestAvecCaracteresSpeciaux = new RegisterRequest();
        requestAvecCaracteresSpeciaux.setNom("N'Diaye");
        requestAvecCaracteresSpeciaux.setPrenom("Aïssatou");
        requestAvecCaracteresSpeciaux.setEmail("aissatou@example.com");
        requestAvecCaracteresSpeciaux.setMotDePasse("password");
        requestAvecCaracteresSpeciaux.setRole(UserEntity.Role.USER);

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponseUser);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestAvecCaracteresSpeciaux)))
                .andExpect(status().isOk());

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login et /auth/register - Devrait accepter le Content-Type JSON")
    void auth_DevraitAccepterContentTypeJson() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponseUser);
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponseUser);

        // Act & Assert - Login
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // Act & Assert - Register
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequestUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(authService, times(1)).login(any(LoginRequest.class));
        verify(authService, times(1)).register(any(RegisterRequest.class));
    }
}
