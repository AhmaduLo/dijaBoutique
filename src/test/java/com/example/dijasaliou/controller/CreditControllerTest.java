package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.CreditClientDto;
import com.example.dijasaliou.dto.PaiementCreditDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.CreditClientService;
import com.example.dijasaliou.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CreditController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@WithMockUser(username = "amadou@example.com", authorities = {"ADMIN"})
@DisplayName("Tests du CreditController")
class CreditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreditClientService creditClientService;

    @MockitoBean
    private UserService userService;

    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    @MockitoBean
    private com.example.dijasaliou.config.HibernateFilterInterceptor hibernateFilterInterceptor;

    @MockitoBean
    private com.example.dijasaliou.filter.SubscriptionExpirationFilter subscriptionExpirationFilter;

    private CreditClientDto creditDto;
    private PaiementCreditDto paiementDto;
    private UserEntity utilisateurTest;
    private PagedResponse<CreditClientDto> pageResponse;

    @BeforeEach
    void setUp() {
        utilisateurTest = UserEntity.builder()
                .id(1L).nom("Diop").prenom("Amadou")
                .email("amadou@example.com")
                .build();

        creditDto = CreditClientDto.builder()
                .id(1L)
                .clientNom("Mme Ndiaye")
                .montantInitial(new BigDecimal("200000.00"))
                .montantRestant(new BigDecimal("100000.00"))
                .montantPaye(new BigDecimal("100000.00"))
                .pourcentageRembourse(50)
                .statut("PARTIEL")
                .employeNom("Amadou Diop")
                .build();

        paiementDto = PaiementCreditDto.builder()
                .id(1L)
                .creditId(1L)
                .montantPaye(new BigDecimal("100000.00"))
                .modePaiement("ESPECES")
                .employeNom("Amadou Diop")
                .build();

        pageResponse = PagedResponse.<CreditClientDto>builder()
                .content(List.of(creditDto))
                .currentPage(0)
                .pageSize(20)
                .totalElements(1L)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    // =========================================================
    // GET /credits — liste paginée
    // =========================================================

    @Test
    @DisplayName("GET /credits — retourne 200 avec la liste paginée des crédits")
    void obtenirCredits_retourne200AvecPage() throws Exception {
        when(creditClientService.obtenirCredits(0, 20, null, null, null, null))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/credits")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].clientNom", is("Mme Ndiaye")))
                .andExpect(jsonPath("$.content[0].statut", is("PARTIEL")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));

        verify(creditClientService).obtenirCredits(0, 20, null, null, null, null);
    }

    @Test
    @DisplayName("GET /credits?statut=EN_ATTENTE — filtre par statut")
    void obtenirCredits_filtreParStatut() throws Exception {
        PagedResponse<CreditClientDto> pageVide = PagedResponse.<CreditClientDto>builder()
                .content(Collections.emptyList())
                .currentPage(0).pageSize(20).totalElements(0L).totalPages(0)
                .first(true).last(true).build();

        when(creditClientService.obtenirCredits(0, 20, null, "EN_ATTENTE", null, null))
                .thenReturn(pageVide);

        mockMvc.perform(get("/credits")
                        .param("statut", "EN_ATTENTE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(creditClientService).obtenirCredits(0, 20, null, "EN_ATTENTE", null, null);
    }

    @Test
    @DisplayName("GET /credits?search=Ndiaye — filtre par recherche")
    void obtenirCredits_filtreParRecherche() throws Exception {
        when(creditClientService.obtenirCredits(0, 20, "Ndiaye", null, null, null))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/credits")
                        .param("search", "Ndiaye")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].clientNom", is("Mme Ndiaye")));
    }

    @Test
    @DisplayName("GET /credits — liste vide si aucun crédit")
    void obtenirCredits_retourneListeVide() throws Exception {
        PagedResponse<CreditClientDto> pageVide = PagedResponse.<CreditClientDto>builder()
                .content(Collections.emptyList())
                .currentPage(0).pageSize(20).totalElements(0L).totalPages(0)
                .first(true).last(true).build();

        when(creditClientService.obtenirCredits(anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(pageVide);

        mockMvc.perform(get("/credits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // =========================================================
    // GET /credits/client/{id} — historique d'un client
    // =========================================================

    @Test
    @DisplayName("GET /credits/client/{id} — retourne 200 avec l'historique du client")
    void obtenirHistoriqueClient_retourne200() throws Exception {
        when(creditClientService.obtenirHistoriqueClient(10L))
                .thenReturn(List.of(creditDto));

        mockMvc.perform(get("/credits/client/10")
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null, List.of(new SimpleGrantedAuthority("ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].clientNom", is("Mme Ndiaye")));

        verify(creditClientService).obtenirHistoriqueClient(10L);
    }

    @Test
    @DisplayName("GET /credits/client/{id} — retourne 200 liste vide si aucun crédit")
    void obtenirHistoriqueClient_retourneListeVide() throws Exception {
        when(creditClientService.obtenirHistoriqueClient(99L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/credits/client/99")
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null, List.of(new SimpleGrantedAuthority("ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================
    // POST /credits/{id}/payer — enregistrer un paiement
    // =========================================================

    @Test
    @DisplayName("POST /credits/{id}/payer — retourne 200 avec le crédit mis à jour")
    void enregistrerPaiement_retourne200() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("montant", 100000.00, "modePaiement", "ESPECES"));

        when(userService.obtenirUtilisateurParEmail("amadou@example.com"))
                .thenReturn(utilisateurTest);
        when(creditClientService.enregistrerPaiement(
                eq(1L), any(), any(), any(), eq(utilisateurTest)))
                .thenReturn(creditDto);

        mockMvc.perform(post("/credits/1/payer")
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null, List.of(new SimpleGrantedAuthority("ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.statut", is("PARTIEL")))
                .andExpect(jsonPath("$.employeNom", is("Amadou Diop")));

        verify(userService).obtenirUtilisateurParEmail("amadou@example.com");
        verify(creditClientService).enregistrerPaiement(
                eq(1L), any(), any(), any(), eq(utilisateurTest));
    }

    @Test
    @DisplayName("POST /credits/{id}/payer — paiement total : statut SOLDE")
    void enregistrerPaiement_paiementTotal_statutSolde() throws Exception {
        CreditClientDto creditSolde = CreditClientDto.builder()
                .id(1L)
                .clientNom("Mme Ndiaye")
                .montantInitial(new BigDecimal("200000.00"))
                .montantRestant(BigDecimal.ZERO)
                .montantPaye(new BigDecimal("200000.00"))
                .pourcentageRembourse(100)
                .statut("SOLDE")
                .build();

        String requestBody = objectMapper.writeValueAsString(
                Map.of("montant", 200000.00, "modePaiement", "WAVE"));

        when(userService.obtenirUtilisateurParEmail("amadou@example.com"))
                .thenReturn(utilisateurTest);
        when(creditClientService.enregistrerPaiement(
                eq(1L), any(), any(), any(), eq(utilisateurTest)))
                .thenReturn(creditSolde);

        mockMvc.perform(post("/credits/1/payer")
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null, List.of(new SimpleGrantedAuthority("ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("SOLDE")))
                .andExpect(jsonPath("$.pourcentageRembourse", is(100)));
    }

    @Test
    @DisplayName("POST /credits/{id}/payer — service lève exception → 400")
    void enregistrerPaiement_serviceLeveException_retourne400() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("montant", 999999.00, "modePaiement", "ESPECES"));

        when(userService.obtenirUtilisateurParEmail("amadou@example.com"))
                .thenReturn(utilisateurTest);
        when(creditClientService.enregistrerPaiement(
                eq(1L), any(), any(), any(), eq(utilisateurTest)))
                .thenThrow(new IllegalArgumentException("Le montant dépasse le restant dû"));

        mockMvc.perform(post("/credits/1/payer")
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null, List.of(new SimpleGrantedAuthority("ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("restant dû")));
    }

    // =========================================================
    // GET /credits/{id}/paiements — liste des paiements d'un crédit
    // =========================================================

    @Test
    @DisplayName("GET /credits/{id}/paiements — retourne 200 avec la liste des paiements")
    void obtenirPaiements_retourne200() throws Exception {
        when(creditClientService.obtenirPaiements(1L))
                .thenReturn(List.of(paiementDto));

        mockMvc.perform(get("/credits/1/paiements")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].modePaiement", is("ESPECES")));

        verify(creditClientService).obtenirPaiements(1L);
    }

    @Test
    @DisplayName("GET /credits/{id}/paiements — crédit inexistant → 400")
    void obtenirPaiements_creditInexistant_retourne400() throws Exception {
        when(creditClientService.obtenirPaiements(999L))
                .thenThrow(new RuntimeException("Crédit introuvable : 999"));

        mockMvc.perform(get("/credits/999/paiements"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Crédit introuvable")));
    }

    @Test
    @DisplayName("GET /credits/{id}/paiements — liste vide si aucun paiement encore")
    void obtenirPaiements_aucunPaiement_retourneListeVide() throws Exception {
        when(creditClientService.obtenirPaiements(2L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/credits/2/paiements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================
    // GET /credits/stats — statistiques
    // =========================================================

    @Test
    @DisplayName("GET /credits/stats — retourne 200 avec les statistiques")
    void obtenirStats_retourne200AvecStats() throws Exception {
        Map<String, Object> stats = Map.of(
                "totalEnAttente", new BigDecimal("500000.00"),
                "montantTotalDu", new BigDecimal("1000000.00"),
                "nombreCreditsActifs", 5L,
                "nombreClientsCrediteurs", 3L,
                "creditsEnRetard", 1L,
                "tauxRecouvrement", 50.0
        );

        when(creditClientService.obtenirStats()).thenReturn(stats);

        mockMvc.perform(get("/credits/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCreditsActifs", is(5)))
                .andExpect(jsonPath("$.nombreClientsCrediteurs", is(3)))
                .andExpect(jsonPath("$.tauxRecouvrement", is(50.0)));

        verify(creditClientService).obtenirStats();
    }

    @Test
    @DisplayName("GET /credits/stats — taux recouvrement 0 si aucun crédit")
    void obtenirStats_tauxZeroSiAucunCredit() throws Exception {
        Map<String, Object> stats = Map.of(
                "nombreCreditsActifs", 0L,
                "nombreClientsCrediteurs", 0L,
                "creditsEnRetard", 0L,
                "tauxRecouvrement", 0.0
        );

        when(creditClientService.obtenirStats()).thenReturn(stats);

        mockMvc.perform(get("/credits/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tauxRecouvrement", is(0.0)))
                .andExpect(jsonPath("$.nombreCreditsActifs", is(0)));
    }
}
