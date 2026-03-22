package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.BonLivraisonDto;
import com.example.dijasaliou.dto.CreateBonLivraisonRequest;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.service.BonLivraisonService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BonLivraisonController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du BonLivraisonController")
class BonLivraisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BonLivraisonService bonLivraisonService;

    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    @MockitoBean
    private com.example.dijasaliou.config.HibernateFilterInterceptor hibernateFilterInterceptor;

    @MockitoBean
    private com.example.dijasaliou.filter.SubscriptionExpirationFilter subscriptionExpirationFilter;

    private BonLivraisonDto bl1;
    private BonLivraisonDto bl2;
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken("admin@boutique.com", null,
                    List.of(new SimpleGrantedAuthority("ADMIN")));

    @BeforeEach
    void setUp() {
        bl1 = BonLivraisonDto.builder()
                .id(1L)
                .numeroBL("BL-2025-001")
                .statut("EN_ATTENTE")
                .clientNom("Aminata Diallo")
                .adresseLivraison("Dakar, Plateau")
                .telephoneClient("771234567")
                .datePrevueLivraison(LocalDate.of(2025, 11, 15))
                .dateCreation(LocalDateTime.of(2025, 11, 10, 10, 0))
                .nomEntreprise("Boutique DijaSaliou")
                .lignes(List.of(
                        BonLivraisonDto.LigneBLDto.builder()
                                .id(1L).nomProduit("Collier en or").quantite(2.0).unite("pièce").build()
                ))
                .build();

        bl2 = BonLivraisonDto.builder()
                .id(2L)
                .numeroBL("BL-2025-002")
                .statut("LIVRE")
                .clientNom("Moussa Traoré")
                .adresseLivraison("Thiès centre")
                .dateCreation(LocalDateTime.of(2025, 11, 12, 14, 30))
                .nomEntreprise("Boutique DijaSaliou")
                .lignes(List.of())
                .build();
    }

    // ==================== GET /bons-de-livraison ====================

    @Test
    @DisplayName("GET /bons-de-livraison - Devrait retourner la liste paginée")
    void getTous_DevraitRetournerListePaginee() throws Exception {
        PagedResponse<BonLivraisonDto> page = PagedResponse.<BonLivraisonDto>builder()
                .content(List.of(bl1, bl2))
                .currentPage(0).pageSize(20).totalElements(2L).totalPages(1)
                .first(true).last(true).build();
        when(bonLivraisonService.getTousPagines(0, 20, null, "TOUS", null, null)).thenReturn(page);

        mockMvc.perform(get("/bons-de-livraison")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].numeroBL", is("BL-2025-001")))
                .andExpect(jsonPath("$.content[0].statut", is("EN_ATTENTE")))
                .andExpect(jsonPath("$.content[1].statut", is("LIVRE")));

        verify(bonLivraisonService, times(1)).getTousPagines(0, 20, null, "TOUS", null, null);
    }

    @Test
    @DisplayName("GET /bons-de-livraison - Devrait retourner liste vide")
    void getTous_DevraitRetournerListeVide() throws Exception {
        PagedResponse<BonLivraisonDto> pageVide = PagedResponse.<BonLivraisonDto>builder()
                .content(List.of()).currentPage(0).pageSize(20)
                .totalElements(0L).totalPages(0).first(true).last(true).build();
        when(bonLivraisonService.getTousPagines(0, 20, null, "TOUS", null, null)).thenReturn(pageVide);

        mockMvc.perform(get("/bons-de-livraison")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ==================== GET /bons-de-livraison/{id} ====================

    @Test
    @DisplayName("GET /bons-de-livraison/{id} - Devrait retourner le BL par son ID")
    void getParId_DevraitRetournerBL() throws Exception {
        when(bonLivraisonService.getParId(1L)).thenReturn(bl1);

        mockMvc.perform(get("/bons-de-livraison/1")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.numeroBL", is("BL-2025-001")))
                .andExpect(jsonPath("$.clientNom", is("Aminata Diallo")))
                .andExpect(jsonPath("$.lignes", hasSize(1)))
                .andExpect(jsonPath("$.lignes[0].nomProduit", is("Collier en or")));

        verify(bonLivraisonService, times(1)).getParId(1L);
    }

    @Test
    @DisplayName("GET /bons-de-livraison/{id} - Devrait retourner 400 si BL inexistant")
    void getParId_DevraitRetourner400SiInexistant() throws Exception {
        when(bonLivraisonService.getParId(999L))
                .thenThrow(new RuntimeException("BL non trouvé"));

        mockMvc.perform(get("/bons-de-livraison/999")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ==================== POST /bons-de-livraison ====================

    @Test
    @DisplayName("POST /bons-de-livraison - Devrait créer un nouveau BL")
    void creer_DevraitCreerBL() throws Exception {
        when(bonLivraisonService.creer(any(CreateBonLivraisonRequest.class))).thenReturn(bl1);

        CreateBonLivraisonRequest request = new CreateBonLivraisonRequest();
        request.setClientNom("Aminata Diallo");
        request.setAdresseLivraison("Dakar, Plateau");
        request.setTelephoneClient("771234567");
        request.setDatePrevueLivraison(LocalDate.of(2025, 11, 15));

        CreateBonLivraisonRequest.LigneBLRequest ligne = new CreateBonLivraisonRequest.LigneBLRequest();
        ligne.setNomProduit("Collier en or");
        ligne.setQuantite(2.0);
        ligne.setUnite("pièce");
        request.setLignes(List.of(ligne));

        mockMvc.perform(post("/bons-de-livraison")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.numeroBL", is("BL-2025-001")));

        verify(bonLivraisonService, times(1)).creer(any(CreateBonLivraisonRequest.class));
    }

    // ==================== PUT /bons-de-livraison/{id}/livrer ====================

    @Test
    @DisplayName("PUT /bons-de-livraison/{id}/livrer - Devrait marquer le BL comme livré")
    void marquerLivre_DevraitMettreAJourStatut() throws Exception {
        BonLivraisonDto blLivre = BonLivraisonDto.builder()
                .id(1L).numeroBL("BL-2025-001").statut("LIVRE")
                .clientNom("Aminata Diallo").lignes(List.of()).build();
        when(bonLivraisonService.marquerLivre(1L)).thenReturn(blLivre);

        mockMvc.perform(put("/bons-de-livraison/1/livrer")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("LIVRE")));

        verify(bonLivraisonService, times(1)).marquerLivre(1L);
    }

    @Test
    @DisplayName("PUT /bons-de-livraison/{id}/livrer - Devrait retourner 400 si BL inexistant")
    void marquerLivre_DevraitRetourner400SiInexistant() throws Exception {
        when(bonLivraisonService.marquerLivre(999L))
                .thenThrow(new RuntimeException("BL non trouvé"));

        mockMvc.perform(put("/bons-de-livraison/999/livrer")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ==================== PUT /bons-de-livraison/{id}/annuler ====================

    @Test
    @DisplayName("PUT /bons-de-livraison/{id}/annuler - Devrait annuler le BL")
    void annuler_DevraitMettreAJourStatut() throws Exception {
        BonLivraisonDto blAnnule = BonLivraisonDto.builder()
                .id(1L).numeroBL("BL-2025-001").statut("ANNULE")
                .clientNom("Aminata Diallo").lignes(List.of()).build();
        when(bonLivraisonService.annuler(1L)).thenReturn(blAnnule);

        mockMvc.perform(put("/bons-de-livraison/1/annuler")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("ANNULE")));

        verify(bonLivraisonService, times(1)).annuler(1L);
    }

    @Test
    @DisplayName("PUT /bons-de-livraison/{id}/annuler - Devrait retourner 400 si déjà annulé")
    void annuler_DevraitRetourner400SiDejaAnnule() throws Exception {
        when(bonLivraisonService.annuler(1L))
                .thenThrow(new IllegalStateException("BL déjà annulé"));

        mockMvc.perform(put("/bons-de-livraison/1/annuler")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ==================== DELETE /bons-de-livraison/{id} ====================

    @Test
    @DisplayName("DELETE /bons-de-livraison/{id} - Devrait supprimer le BL")
    void supprimer_DevraitSupprimerBL() throws Exception {
        doNothing().when(bonLivraisonService).supprimer(1L);

        mockMvc.perform(delete("/bons-de-livraison/1")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("supprimé")));

        verify(bonLivraisonService, times(1)).supprimer(1L);
    }

    @Test
    @DisplayName("DELETE /bons-de-livraison/{id} - Devrait retourner 400 si BL inexistant")
    void supprimer_DevraitRetourner400SiInexistant() throws Exception {
        doThrow(new RuntimeException("BL non trouvé")).when(bonLivraisonService).supprimer(999L);

        mockMvc.perform(delete("/bons-de-livraison/999")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
