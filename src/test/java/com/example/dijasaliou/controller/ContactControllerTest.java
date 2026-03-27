package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.ContactRequest;
import com.example.dijasaliou.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ContactController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du ContactController")
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    @MockitoBean
    private com.example.dijasaliou.config.HibernateFilterInterceptor hibernateFilterInterceptor;

    @MockitoBean
    private com.example.dijasaliou.filter.SubscriptionExpirationFilter subscriptionExpirationFilter;

    private ContactRequest contactRequest;
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken("admin@boutique.com", null,
                    List.of(new SimpleGrantedAuthority("ADMIN")));

    @BeforeEach
    void setUp() {
        contactRequest = new ContactRequest(
                "Dija Saliou",
                "admin@boutique.com",
                "Boutique DijaSaliou",
                null,
                "Demande d'assistance technique",
                "Bonjour, j'ai besoin d'aide pour configurer mon compte correctement."
        );
    }

    @Test
    @DisplayName("POST /contact - Devrait envoyer le message avec succès")
    void sendContactMessage_DevraitReussir() throws Exception {
        doNothing().when(emailService).sendContactEmail(any());

        mockMvc.perform(post("/contact")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contactRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", containsString("envoyé avec succès")));

        verify(emailService, times(1)).sendContactEmail(any());
    }

    @Test
    @DisplayName("POST /contact - Devrait retourner 500 si erreur d'envoi email")
    void sendContactMessage_DevraitRetourner500SiErreurEmail() throws Exception {
        doThrow(new RuntimeException("Erreur SMTP")).when(emailService).sendContactEmail(any());

        mockMvc.perform(post("/contact")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contactRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is("error")));

        verify(emailService, times(1)).sendContactEmail(any());
    }

    @Test
    @DisplayName("POST /contact - Devrait retourner 500 si données invalides (@Valid)")
    void sendContactMessage_DevraitRetourner500SiValidationEchoue() throws Exception {
        // Message trop court (<10 chars), email invalide, nom vide
        ContactRequest invalide = new ContactRequest("", "email-invalide", "", null, "Sujet", "court");

        mockMvc.perform(post("/contact")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalide)))
                .andExpect(status().isInternalServerError());

        // La validation échoue avant l'appel au service
        verify(emailService, never()).sendContactEmail(any());
    }

    @Test
    @DisplayName("POST /contact - Devrait fonctionner sans authentification (auth optionnel)")
    void sendContactMessage_DevraitFonctionnerSansAuth() throws Exception {
        doNothing().when(emailService).sendContactEmail(any());

        // Pas de .principal() → authentication sera null dans le controller
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contactRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")));

        verify(emailService, times(1)).sendContactEmail(any());
    }
}
