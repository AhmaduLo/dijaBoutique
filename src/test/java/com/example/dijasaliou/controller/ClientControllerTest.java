package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.ClientDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du ClientController")
@WithMockUser(username = "admin@example.com", authorities = {"ADMIN"})
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    @MockitoBean
    private com.example.dijasaliou.config.HibernateFilterInterceptor hibernateFilterInterceptor;

    @MockitoBean
    private com.example.dijasaliou.filter.SubscriptionExpirationFilter subscriptionExpirationFilter;

    private ClientDto clientDto1;
    private ClientDto clientDto2;

    @BeforeEach
    void setUp() {
        clientDto1 = ClientDto.builder()
                .id("test-id-1").nom("Aminata Diallo").telephone("771234567")
                .detteTotale(new BigDecimal("15000.00")).nombreCreditsActifs(2)
                .build();
        clientDto2 = ClientDto.builder()
                .id("test-id-2").nom("Moussa Traoré").telephone("781234567")
                .detteTotale(BigDecimal.ZERO).nombreCreditsActifs(0)
                .build();
    }

    // ==================== GET /clients ====================

    @Test
    @DisplayName("GET /clients - Devrait retourner la liste paginée")
    void obtenirTous_DevraitRetournerListePaginee() throws Exception {
        PagedResponse<ClientDto> page = PagedResponse.<ClientDto>builder()
                .content(List.of(clientDto1, clientDto2))
                .currentPage(0).pageSize(20).totalElements(2L).totalPages(1)
                .first(true).last(true).build();
        when(clientService.obtenirClientsPagines(0, 20, null)).thenReturn(page);

        mockMvc.perform(get("/clients").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is("test-id-1")))
                .andExpect(jsonPath("$.content[0].nom", is("Aminata Diallo")))
                .andExpect(jsonPath("$.content[0].nombreCreditsActifs", is(2)))
                .andExpect(jsonPath("$.content[1].id", is("test-id-2")))
                .andExpect(jsonPath("$.totalElements", is(2)));

        verify(clientService, times(1)).obtenirClientsPagines(0, 20, null);
    }

    @Test
    @DisplayName("GET /clients - Devrait retourner liste vide")
    void obtenirTous_DevraitRetournerListeVide() throws Exception {
        PagedResponse<ClientDto> pageVide = PagedResponse.<ClientDto>builder()
                .content(List.of()).currentPage(0).pageSize(20)
                .totalElements(0L).totalPages(0).first(true).last(true).build();
        when(clientService.obtenirClientsPagines(0, 20, null)).thenReturn(pageVide);

        mockMvc.perform(get("/clients").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));

        verify(clientService, times(1)).obtenirClientsPagines(0, 20, null);
    }

    @Test
    @DisplayName("GET /clients avec search - Devrait filtrer par nom")
    void obtenirTous_AvecSearch() throws Exception {
        PagedResponse<ClientDto> page = PagedResponse.<ClientDto>builder()
                .content(List.of(clientDto1))
                .currentPage(0).pageSize(20).totalElements(1L).totalPages(1)
                .first(true).last(true).build();
        when(clientService.obtenirClientsPagines(0, 20, "Aminata")).thenReturn(page);

        mockMvc.perform(get("/clients").param("search", "Aminata")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nom", is("Aminata Diallo")));

        verify(clientService, times(1)).obtenirClientsPagines(0, 20, "Aminata");
    }

    // ==================== GET /clients/search ====================

    @Test
    @DisplayName("GET /clients/search - Devrait retourner les clients correspondants")
    void rechercher_DevraitRetournerClients() throws Exception {
        when(clientService.rechercherClients("Aminata")).thenReturn(List.of(clientDto1));

        mockMvc.perform(get("/clients/search").param("q", "Aminata")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("test-id-1")))
                .andExpect(jsonPath("$[0].nom", is("Aminata Diallo")));

        verify(clientService, times(1)).rechercherClients("Aminata");
    }

    @Test
    @DisplayName("GET /clients/search - Devrait retourner liste vide si aucun résultat")
    void rechercher_DevraitRetournerListeVideSiAucunResultat() throws Exception {
        when(clientService.rechercherClients("Inexistant")).thenReturn(List.of());

        mockMvc.perform(get("/clients/search").param("q", "Inexistant")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(clientService, times(1)).rechercherClients("Inexistant");
    }

    // ==================== GET /clients/{id} ====================

    @Test
    @DisplayName("GET /clients/{id} - Devrait retourner le client par son ID")
    void obtenirParId_DevraitRetournerClient() throws Exception {
        when(clientService.obtenirClientParId("test-id-1")).thenReturn(clientDto1);

        mockMvc.perform(get("/clients/test-id-1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("test-id-1")))
                .andExpect(jsonPath("$.nom", is("Aminata Diallo")))
                .andExpect(jsonPath("$.telephone", is("771234567")))
                .andExpect(jsonPath("$.detteTotale", is(15000.00)));

        verify(clientService, times(1)).obtenirClientParId("test-id-1");
    }

    @Test
    @DisplayName("GET /clients/{id} - Devrait retourner 400 si client inexistant")
    void obtenirParId_DevraitRetourner400SiInexistant() throws Exception {
        when(clientService.obtenirClientParId("999"))
                .thenThrow(new RuntimeException("Client non trouvé"));

        mockMvc.perform(get("/clients/999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(clientService, times(1)).obtenirClientParId("999");
    }

    // ==================== POST /clients ====================

    @Test
    @DisplayName("POST /clients - Devrait créer un nouveau client")
    void creer_DevraitCreerClient() throws Exception {
        ClientDto nouveauClient = ClientDto.builder()
                .id("test-id-3").nom("Fatou Ba").telephone("791234567")
                .detteTotale(BigDecimal.ZERO).nombreCreditsActifs(0).build();
        when(clientService.creerClient("Fatou Ba", "791234567")).thenReturn(nouveauClient);

        Map<String, String> body = Map.of("nom", "Fatou Ba", "telephone", "791234567");

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("test-id-3")))
                .andExpect(jsonPath("$.nom", is("Fatou Ba")))
                .andExpect(jsonPath("$.telephone", is("791234567")));

        verify(clientService, times(1)).creerClient("Fatou Ba", "791234567");
    }

    @Test
    @DisplayName("POST /clients - Devrait créer un client sans téléphone")
    void creer_DevraitCreerClientSansTelephone() throws Exception {
        ClientDto nouveauClient = ClientDto.builder()
                .id("test-id-4").nom("Ibra Sow").telephone(null)
                .detteTotale(BigDecimal.ZERO).nombreCreditsActifs(0).build();
        when(clientService.creerClient("Ibra Sow", null)).thenReturn(nouveauClient);

        Map<String, String> body = Map.of("nom", "Ibra Sow");

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("test-id-4")))
                .andExpect(jsonPath("$.nom", is("Ibra Sow")));

        verify(clientService, times(1)).creerClient("Ibra Sow", null);
    }

    @Test
    @DisplayName("POST /clients - Devrait retourner 400 si service lève une exception")
    void creer_DevraitRetourner400SiErreur() throws Exception {
        when(clientService.creerClient("", null))
                .thenThrow(new RuntimeException("Nom du client requis"));

        Map<String, String> body = Map.of("nom", "");

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
