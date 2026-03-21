package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.AuthResponse;
import com.example.dijasaliou.dto.ForgotPasswordRequest;
import com.example.dijasaliou.dto.LoginRequest;
import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.dto.ResetPasswordRequest;
import com.example.dijasaliou.entity.PasswordResetToken;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.jwt.JwtService;
import com.example.dijasaliou.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmailService emailService;
    @Mock private AchatRepository achatRepository;
    @Mock private VenteRepository venteRepository;
    @Mock private DepenseRepository depenseRepository;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserEntity utilisateur;
    private TenantEntity tenantTest;

    @BeforeEach
    void setUp() {
        tenantTest = TenantEntity.builder()
                .tenantUuid("uuid-tenant-test")
                .nomEntreprise("Boutique Test")
                .actif(true)
                .plan(TenantEntity.Plan.GRATUIT)
                .dateDebutEssai(LocalDateTime.now())
                .essaiUtilise(false)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setNom("Diop");
        registerRequest.setPrenom("Amadou");
        registerRequest.setEmail("amadou@example.com");
        registerRequest.setMotDePasse("password123");
        registerRequest.setNomEntreprise("Boutique Test");
        registerRequest.setAcceptationCGU(true);
        registerRequest.setAcceptationPolitiqueConfidentialite(true);

        loginRequest = new LoginRequest("amadou@example.com", "password123");

        utilisateur = UserEntity.builder()
                .id(1L).nom("Diop").prenom("Amadou")
                .email("amadou@example.com")
                .motDePasse("encodedPassword")
                .role(UserEntity.Role.ADMIN)
                .tenant(tenantTest)
                .build();
    }

    // =========================================================
    // register
    // =========================================================

    @Test
    @DisplayName("register() — crée un tenant + utilisateur ADMIN et retourne un token")
    void register_creeUtilisateurEtRetourneToken() {
        when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
        when(tenantRepository.existsByNomEntreprise(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(tenantRepository.save(any())).thenReturn(tenantTest);
        when(userRepository.save(any())).thenReturn(utilisateur);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("token123");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getUser()).isNotNull();
        verify(userRepository).save(any());
        verify(jwtService).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("register() — lève exception si email déjà utilisé")
    void register_leveExceptionEmailExiste() {
        when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("existe déjà");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() — lève exception si nom entreprise déjà pris")
    void register_leveExceptionEntrepriseExiste() {
        when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
        when(tenantRepository.existsByNomEntreprise("Boutique Test")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("entreprise");
    }

    @Test
    @DisplayName("register() — lève exception si CGU non acceptées")
    void register_leveExceptionCGUNonAcceptees() {
        registerRequest.setAcceptationCGU(false);
        when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
        when(tenantRepository.existsByNomEntreprise(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Conditions G");
    }

    @Test
    @DisplayName("register() — encode le mot de passe avant de sauvegarder")
    void register_encodeMotDePasse() {
        when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
        when(tenantRepository.existsByNomEntreprise(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(tenantRepository.save(any())).thenReturn(tenantTest);
        when(userRepository.save(any())).thenReturn(utilisateur);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("token123");

        authService.register(registerRequest);

        verify(passwordEncoder).encode("password123");
    }

    // =========================================================
    // login
    // =========================================================

    @Test
    @DisplayName("login() — connecte un utilisateur valide et retourne un token")
    void login_connecteUtilisateurValide() {
        when(userRepository.findByEmailAndDeletedFalse("amadou@example.com"))
                .thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken("amadou@example.com", "uuid-tenant-test")).thenReturn("token123");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getUser()).isEqualTo(utilisateur);
    }

    @Test
    @DisplayName("login() — lève exception si email non trouvé")
    void login_leveExceptionEmailNonTrouve() {
        when(userRepository.findByEmailAndDeletedFalse("amadou@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");
        verify(jwtService, never()).generateToken(anyString(), any());
    }

    @Test
    @DisplayName("login() — lève exception si mot de passe incorrect")
    void login_leveExceptionMotDePasseIncorrect() {
        when(userRepository.findByEmailAndDeletedFalse("amadou@example.com"))
                .thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");
    }

    @Test
    @DisplayName("login() — lève exception si compte entreprise désactivé")
    void login_leveExceptionTenantInactif() {
        tenantTest.setActif(false);
        when(userRepository.findByEmailAndDeletedFalse("amadou@example.com"))
                .thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("désactivé");
    }

    // =========================================================
    // forgotPassword
    // =========================================================

    @Test
    @DisplayName("forgotPassword() — envoie un email si l'utilisateur existe")
    void forgotPassword_envoieEmail() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("amadou@example.com");

        when(userRepository.findByEmailAndDeletedFalse("amadou@example.com"))
                .thenReturn(Optional.of(utilisateur));
        doNothing().when(passwordResetTokenRepository).deleteByUser(utilisateur);
        when(passwordResetTokenRepository.save(any())).thenReturn(new PasswordResetToken());

        authService.forgotPassword(req);

        verify(emailService).sendPasswordResetEmail(eq("amadou@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("forgotPassword() — ne fait rien si l'email est inconnu (sécurité anti-énumération)")
    void forgotPassword_silencieuxSiEmailInconnu() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("inconnu@example.com");
        when(userRepository.findByEmailAndDeletedFalse("inconnu@example.com"))
                .thenReturn(Optional.empty());

        authService.forgotPassword(req);

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    // =========================================================
    // resetPassword
    // =========================================================

    @Test
    @DisplayName("resetPassword() — lève exception si mots de passe différents")
    void resetPassword_leveExceptionMotsDePasseDifferents() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("token");
        req.setNouveauMotDePasse("password1");
        req.setConfirmationMotDePasse("password2");

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ne correspondent pas");
    }

    @Test
    @DisplayName("resetPassword() — lève exception si token invalide")
    void resetPassword_leveExceptionTokenInvalide() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("token-invalide");
        req.setNouveauMotDePasse("password");
        req.setConfirmationMotDePasse("password");

        when(passwordResetTokenRepository.findByToken("token-invalide"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalide ou expiré");
    }

    // =========================================================
    // deleteAdminAccount
    // =========================================================

    @Test
    @DisplayName("deleteAdminAccount() — supprime le tenant et toutes les données associées")
    void deleteAdminAccount_succes() {
        tenantTest.setUtilisateurs(java.util.List.of(utilisateur));
        when(userRepository.findByEmailAndDeletedFalse("amadou@example.com"))
                .thenReturn(Optional.of(utilisateur));

        authService.deleteAdminAccount("amadou@example.com");

        verify(tenantRepository).delete(tenantTest);
        verify(achatRepository).deleteByUtilisateur(utilisateur);
        verify(venteRepository).deleteByUtilisateur(utilisateur);
        verify(depenseRepository).deleteByUtilisateur(utilisateur);
    }

    @Test
    @DisplayName("deleteAdminAccount() — lève exception si l'utilisateur n'est pas ADMIN")
    void deleteAdminAccount_leveExceptionSiNonAdmin() {
        UserEntity nonAdmin = UserEntity.builder()
                .id(2L).nom("User").prenom("Normal")
                .email("user@example.com")
                .role(UserEntity.Role.USER)
                .tenant(tenantTest)
                .build();
        when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
                .thenReturn(Optional.of(nonAdmin));

        assertThatThrownBy(() -> authService.deleteAdminAccount("user@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("administrateur");
    }
}
