package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.AuthResponse;
import com.example.dijasaliou.dto.LoginRequest;
import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.jwt.JwtService;
import com.example.dijasaliou.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserEntity utilisateur;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setNom("Diop");
        registerRequest.setPrenom("Amadou");
        registerRequest.setEmail("amadou@example.com");
        registerRequest.setMotDePasse("password123");
        registerRequest.setRole(UserEntity.Role.USER);

        loginRequest = new LoginRequest("amadou@example.com", "password123");

        utilisateur = UserEntity.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Amadou")
                .email("amadou@example.com")
                .motDePasse("encodedPassword")
                .role(UserEntity.Role.USER)
                .build();
    }

    @Test
    @DisplayName("register() - Devrait créer un nouvel utilisateur")
    void register_DevraitCreerUtilisateur() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(utilisateur);
        when(jwtService.generateToken(anyString())).thenReturn("token123");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getUser()).isNotNull();
        verify(userRepository, times(1)).save(any(UserEntity.class));
        verify(jwtService, times(1)).generateToken(anyString());
    }

    @Test
    @DisplayName("register() - Devrait lancer exception si email existe")
    void register_DevraitLancerExceptionEmailExiste() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("email existe déjà");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() - Devrait encoder le mot de passe")
    void register_DevraitEncoderMotDePasse() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(utilisateur);
        when(jwtService.generateToken(anyString())).thenReturn("token123");

        authService.register(registerRequest);

        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("register() - Devrait utiliser rôle USER par défaut si non spécifié")
    void register_DevraitUtiliserRoleUserParDefaut() {
        registerRequest.setRole(null);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(utilisateur);
        when(jwtService.generateToken(anyString())).thenReturn("token123");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("login() - Devrait connecter un utilisateur valide")
    void login_DevraitConnecterUtilisateurValide() {
        when(userRepository.findByEmail("amadou@example.com"))
                .thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(true);
        when(jwtService.generateToken("amadou@example.com"))
                .thenReturn("token123");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getUser()).isEqualTo(utilisateur);
        verify(jwtService, times(1)).generateToken("amadou@example.com");
    }

    @Test
    @DisplayName("login() - Devrait lancer exception si email non trouvé")
    void login_DevraitLancerExceptionEmailNonTrouve() {
        when(userRepository.findByEmail("inexistant@example.com"))
                .thenReturn(Optional.empty());

        LoginRequest loginInexistant = new LoginRequest("inexistant@example.com", "password");

        assertThatThrownBy(() -> authService.login(loginInexistant))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");

        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("login() - Devrait lancer exception si mot de passe incorrect")
    void login_DevraitLancerExceptionMotDePasseIncorrect() {
        when(userRepository.findByEmail("amadou@example.com"))
                .thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("mauvaisMotDePasse", "encodedPassword"))
                .thenReturn(false);

        LoginRequest loginInvalide = new LoginRequest("amadou@example.com", "mauvaisMotDePasse");

        assertThatThrownBy(() -> authService.login(loginInvalide))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");

        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("login() - Devrait vérifier le mot de passe avec PasswordEncoder")
    void login_DevraitVerifierMotDePasseAvecEncoder() {
        when(userRepository.findByEmail("amadou@example.com"))
                .thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(true);
        when(jwtService.generateToken(anyString())).thenReturn("token123");

        authService.login(loginRequest);

        verify(passwordEncoder, times(1)).matches("password123", "encodedPassword");
    }
}
