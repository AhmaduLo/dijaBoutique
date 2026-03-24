package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.VenteDto;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.service.UserService;
import com.example.dijasaliou.service.VenteService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour VenteController
 *
 * Bonnes pratiques appliquées :
 * - @WebMvcTest : Test uniquement la couche controller (léger et rapide)
 * - @MockitoBean : Mock les dépendances (services)
 * - MockMvc : Simule les appels HTTP
 * - @DisplayName : Descriptions claires des tests
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - Vérification des status HTTP et du contenu JSON
 */
@WebMvcTest(controllers = VenteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du VenteController")
class VenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VenteService venteService;

    @MockitoBean
    private UserService userService;

    // Mocker les beans de sécurité pour éviter les problèmes de dépendances
    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    @MockitoBean
    private com.example.dijasaliou.config.HibernateFilterInterceptor hibernateFilterInterceptor;

    @MockitoBean
    private com.example.dijasaliou.filter.SubscriptionExpirationFilter subscriptionExpirationFilter;

    private UserEntity utilisateurTest;
    private VenteEntity venteTest;
    private VenteEntity venteTest2;

    /**
     * Initialisation des données de test avant chaque test
     */
    @BeforeEach
    void setUp() {
        // Création d'un utilisateur de test
        utilisateurTest = UserEntity.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Amadou")
                .email("amadou@example.com")
                .build();

        // Création d'une première vente de test
        venteTest = VenteEntity.builder()
                .quantite(5)
                .nomProduit("Collier en or")
                .prixUnitaire(new BigDecimal("150000.00"))
                .prixTotal(new BigDecimal("750000.00"))
                .dateVente(LocalDate.of(2025, 10, 15))
                .client("Mme Ndiaye")
                .utilisateur(utilisateurTest)
                .build();
        venteTest.setId("test-id-1");

        // Création d'une deuxième vente de test
        venteTest2 = VenteEntity.builder()
                .quantite(3)
                .nomProduit("Bracelet en argent")
                .prixUnitaire(new BigDecimal("80000.00"))
                .prixTotal(new BigDecimal("240000.00"))
                .dateVente(LocalDate.of(2025, 10, 20))
                .client("M. Sow")
                .utilisateur(utilisateurTest)
                .build();
        venteTest2.setId("test-id-2");
    }

    // ==================== Tests pour GET /ventes ====================

    @Test
    @DisplayName("GET /ventes - Devrait retourner toutes les ventes avec succès")
    void obtenirTous_DevraitRetournerToutesLesVentes() throws Exception {
        // Arrange
        VenteDto dto1 = VenteDto.fromEntity(venteTest);
        VenteDto dto2 = VenteDto.fromEntity(venteTest2);
        PagedResponse<VenteDto> page = PagedResponse.<VenteDto>builder()
                .content(Arrays.asList(dto1, dto2)).currentPage(0).pageSize(20)
                .totalElements(2L).totalPages(1).first(true).last(true).build();
        when(venteService.obtenirVentesPaginees(0, 20, null, null, null)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/ventes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is("test-id-1")))
                .andExpect(jsonPath("$.content[0].nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$.content[0].quantite", is(5)))
                .andExpect(jsonPath("$.content[0].client", is("Mme Ndiaye")))
                .andExpect(jsonPath("$.content[1].id", is("test-id-2")))
                .andExpect(jsonPath("$.content[1].nomProduit", is("Bracelet en argent")));

        verify(venteService, times(1)).obtenirVentesPaginees(0, 20, null, null, null);
    }

    @Test
    @DisplayName("GET /ventes - Devrait retourner une liste vide quand aucune vente")
    void obtenirTous_DevraitRetournerListeVide() throws Exception {
        // Arrange
        PagedResponse<VenteDto> pageVide = PagedResponse.<VenteDto>builder()
                .content(Arrays.asList()).currentPage(0).pageSize(20)
                .totalElements(0L).totalPages(0).first(true).last(true).build();
        when(venteService.obtenirVentesPaginees(0, 20, null, null, null)).thenReturn(pageVide);

        // Act & Assert
        mockMvc.perform(get("/ventes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(venteService, times(1)).obtenirVentesPaginees(0, 20, null, null, null);
    }

    // ==================== Tests pour GET /ventes/{id} ====================

    @Test
    @DisplayName("GET /ventes/{id} - Devrait retourner une vente par son ID")
    void obtenirParId_DevraitRetournerVente() throws Exception {
        // Arrange
        String venteId = "test-id-1";
        when(venteService.obtenirVenteParId(venteId)).thenReturn(venteTest);

        // Act & Assert
        mockMvc.perform(get("/ventes/{id}", venteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("test-id-1")))
                .andExpect(jsonPath("$.nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$.quantite", is(5)))
                .andExpect(jsonPath("$.prixUnitaire", is(150000.00)))
                .andExpect(jsonPath("$.prixTotal", is(750000.00)))
                .andExpect(jsonPath("$.client", is("Mme Ndiaye")));

        verify(venteService, times(1)).obtenirVenteParId(venteId);
    }

    @Test
    @DisplayName("GET /ventes/{id} - Devrait lancer une exception si vente inexistante")
    void obtenirParId_DevraitLancerExceptionSiVenteInexistante() throws Exception {
        // Arrange
        String venteId = "999";
        when(venteService.obtenirVenteParId(venteId))
                .thenThrow(new RuntimeException("Vente non trouvée"));

        // Act & Assert
        mockMvc.perform(get("/ventes/{id}", venteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(venteService, times(1)).obtenirVenteParId(venteId);
    }

    // ==================== Tests pour POST /ventes ====================

    @Test
    @DisplayName("POST /ventes - Devrait créer une nouvelle vente avec succès")
    void creer_DevraitCreerNouvelleVente() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        VenteEntity nouvelleVente = VenteEntity.builder()
                .quantite(10)
                .nomProduit("Bague en diamant")
                .prixUnitaire(new BigDecimal("200000.00"))
                .dateVente(LocalDate.now())
                .client("Mme Ba")
                .build();

        VenteEntity venteCreee = VenteEntity.builder()
                .quantite(10)
                .nomProduit("Bague en diamant")
                .prixUnitaire(new BigDecimal("200000.00"))
                .prixTotal(new BigDecimal("2000000.00"))
                .dateVente(LocalDate.now())
                .client("Mme Ba")
                .utilisateur(utilisateurTest)
                .build();
        venteCreee.setId("test-id-3");

        when(userService.obtenirUtilisateurParEmail("amadou@example.com")).thenReturn(utilisateurTest);
        when(venteService.creerVente(any(VenteEntity.class), eq(utilisateurTest)))
                .thenReturn(venteCreee);

        // Act & Assert
        mockMvc.perform(post("/ventes")
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null, List.of(new SimpleGrantedAuthority("USER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nouvelleVente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("test-id-3")))
                .andExpect(jsonPath("$.nomProduit", is("Bague en diamant")))
                .andExpect(jsonPath("$.quantite", is(10)))
                .andExpect(jsonPath("$.prixTotal", is(2000000.00)))
                .andExpect(jsonPath("$.client", is("Mme Ba")));

        verify(userService, times(1)).obtenirUtilisateurParEmail("amadou@example.com");
        verify(venteService, times(1)).creerVente(any(VenteEntity.class), eq(utilisateurTest));
    }

    // ==================== Tests pour PUT /ventes/{id} ====================

    @Test
    @DisplayName("PUT /ventes/{id} - Devrait modifier une vente existante")
    void modifier_DevraitModifierVente() throws Exception {
        // Arrange
        String venteId = "test-id-1";
        Long utilisateurId = 1L;

        VenteEntity venteModifiee = VenteEntity.builder()
                .quantite(8)
                .nomProduit("Collier en or - Mise à jour")
                .prixUnitaire(new BigDecimal("160000.00"))
                .dateVente(LocalDate.of(2025, 10, 25))
                .client("Mme Ndiaye (modifié)")
                .build();

        VenteEntity venteMiseAJour = VenteEntity.builder()
                .quantite(8)
                .nomProduit("Collier en or - Mise à jour")
                .prixUnitaire(new BigDecimal("160000.00"))
                .prixTotal(new BigDecimal("1280000.00"))
                .dateVente(LocalDate.of(2025, 10, 25))
                .client("Mme Ndiaye (modifié)")
                .utilisateur(utilisateurTest)
                .build();
        venteMiseAJour.setId(venteId);

        when(userService.obtenirUtilisateurParEmail("amadou@example.com")).thenReturn(utilisateurTest);
        when(venteService.modifierVente(eq(venteId), any(VenteEntity.class)))
                .thenReturn(venteMiseAJour);

        // Act & Assert
        mockMvc.perform(put("/ventes/{id}", venteId)
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null, List.of(new SimpleGrantedAuthority("USER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(venteModifiee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("test-id-1")))
                .andExpect(jsonPath("$.nomProduit", is("Collier en or - Mise à jour")))
                .andExpect(jsonPath("$.quantite", is(8)))
                .andExpect(jsonPath("$.prixTotal", is(1280000.00)));

        verify(userService, times(1)).obtenirUtilisateurParEmail("amadou@example.com");
        verify(venteService, times(1)).modifierVente(eq(venteId), any(VenteEntity.class));
    }

    @Test
    @DisplayName("PUT /ventes/{id} - Devrait lancer une exception si vente inexistante")
    void modifier_DevraitLancerExceptionSiVenteInexistante() throws Exception {
        // Arrange
        String venteId = "999";

        VenteEntity venteModifiee = VenteEntity.builder()
                .quantite(5)
                .nomProduit("Vente inexistante")
                .prixUnitaire(new BigDecimal("100000.00"))
                .dateVente(LocalDate.now())
                .build();

        when(userService.obtenirUtilisateurParEmail("amadou@example.com")).thenReturn(utilisateurTest);
        when(venteService.modifierVente(eq(venteId), any(VenteEntity.class)))
                .thenThrow(new RuntimeException("Vente non trouvée"));

        // Act & Assert
        mockMvc.perform(put("/ventes/{id}", venteId)
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null, List.of(new SimpleGrantedAuthority("USER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(venteModifiee)))
                .andExpect(status().isBadRequest());

        verify(venteService, times(1)).modifierVente(eq(venteId), any(VenteEntity.class));
    }

    // ==================== Tests pour DELETE /ventes/{id} ====================

    @Test
    @DisplayName("DELETE /ventes/{id} - Devrait supprimer une vente avec succès")
    void supprimer_DevraitSupprimerVente() throws Exception {
        // Arrange
        String venteId = "test-id-1";
        doNothing().when(venteService).supprimerVente(venteId);

        // Act & Assert
        mockMvc.perform(delete("/ventes/{id}", venteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(venteService, times(1)).supprimerVente(venteId);
    }

    @Test
    @DisplayName("DELETE /ventes/{id} - Devrait lancer une exception si vente inexistante")
    void supprimer_DevraitLancerExceptionSiVenteInexistante() throws Exception {
        // Arrange
        String venteId = "999";
        doThrow(new RuntimeException("Vente non trouvée"))
                .when(venteService).supprimerVente(venteId);

        // Act & Assert
        mockMvc.perform(delete("/ventes/{id}", venteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(venteService, times(1)).supprimerVente(venteId);
    }

    // ==================== Tests pour GET /ventes/utilisateur/{utilisateurId} ====================

    @Test
    @DisplayName("GET /ventes/utilisateur/{id} - Devrait retourner les ventes d'un utilisateur")
    void obtenirVentesParUtilisateur_DevraitRetournerVentes() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        PagedResponse<VenteDto> page = PagedResponse.<VenteDto>builder()
                .content(Arrays.asList(VenteDto.fromEntity(venteTest), VenteDto.fromEntity(venteTest2)))
                .currentPage(0).pageSize(20).totalElements(2L).totalPages(1).first(true).last(true).build();
        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(venteService.obtenirVentesParUtilisateurPaginees(utilisateurTest, 0, 20, null, null, null)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/ventes/utilisateur/{utilisateurId}", utilisateurId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is("test-id-1")))
                .andExpect(jsonPath("$.content[0].nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$.content[1].id", is("test-id-2")))
                .andExpect(jsonPath("$.content[1].nomProduit", is("Bracelet en argent")));

        verify(userService, times(1)).obtenirUtilisateurParId(utilisateurId);
        verify(venteService, times(1)).obtenirVentesParUtilisateurPaginees(utilisateurTest, 0, 20, null, null, null);
    }

    @Test
    @DisplayName("GET /ventes/utilisateur/{id} - Devrait retourner liste vide si utilisateur sans ventes")
    void obtenirVentesParUtilisateur_DevraitRetournerListeVide() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        PagedResponse<VenteDto> pageVide = PagedResponse.<VenteDto>builder()
                .content(Arrays.asList()).currentPage(0).pageSize(20)
                .totalElements(0L).totalPages(0).first(true).last(true).build();
        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(venteService.obtenirVentesParUtilisateurPaginees(utilisateurTest, 0, 20, null, null, null)).thenReturn(pageVide);

        // Act & Assert
        mockMvc.perform(get("/ventes/utilisateur/{utilisateurId}", utilisateurId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(venteService, times(1)).obtenirVentesParUtilisateurPaginees(utilisateurTest, 0, 20, null, null, null);
    }

    // ==================== Tests pour GET /ventes/chiffre-affaires ====================

    @Test
    @DisplayName("GET /ventes/chiffre-affaires - Devrait retourner le CA pour une période")
    void calculerChiffreAffaires_DevraitRetournerCA() throws Exception {
        // Arrange
        LocalDate dateDebut = LocalDate.of(2025, 10, 1);
        LocalDate dateFin = LocalDate.of(2025, 10, 31);
        BigDecimal chiffreAffaires = new BigDecimal("990000.00");

        when(venteService.calculerChiffreAffaires(dateDebut, dateFin)).thenReturn(chiffreAffaires);

        // Act & Assert
        mockMvc.perform(get("/ventes/chiffre-affaires")
                        .param("debut", dateDebut.toString())
                        .param("fin", dateFin.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(990000.00)));

        verify(venteService, times(1)).calculerChiffreAffaires(dateDebut, dateFin);
    }

    @Test
    @DisplayName("GET /ventes/chiffre-affaires - Devrait retourner zéro si aucune vente")
    void calculerChiffreAffaires_DevraitRetournerZero() throws Exception {
        // Arrange
        LocalDate dateDebut = LocalDate.of(2025, 11, 1);
        LocalDate dateFin = LocalDate.of(2025, 11, 30);
        BigDecimal chiffreAffaires = BigDecimal.ZERO;

        when(venteService.calculerChiffreAffaires(dateDebut, dateFin)).thenReturn(chiffreAffaires);

        // Act & Assert
        mockMvc.perform(get("/ventes/chiffre-affaires")
                        .param("debut", dateDebut.toString())
                        .param("fin", dateFin.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(0)));

        verify(venteService, times(1)).calculerChiffreAffaires(dateDebut, dateFin);
    }

    // ==================== Tests pour GET /ventes/statistiques ====================

    @Test
    @DisplayName("GET /ventes/statistiques - Devrait retourner les statistiques d'une période")
    void obtenirStatistiques_DevraitRetournerStatistiques() throws Exception {
        // Arrange
        LocalDate dateDebut = LocalDate.of(2025, 10, 1);
        LocalDate dateFin = LocalDate.of(2025, 10, 31);
        List<VenteEntity> ventes = Arrays.asList(venteTest, venteTest2);
        BigDecimal chiffreAffaires = new BigDecimal("990000.00");

        when(venteService.obtenirVentesParPeriode(dateDebut, dateFin)).thenReturn(ventes);
        when(venteService.calculerChiffreAffaires(dateDebut, dateFin)).thenReturn(chiffreAffaires);

        // Act & Assert
        mockMvc.perform(get("/ventes/statistiques")
                        .param("debut", dateDebut.toString())
                        .param("fin", dateFin.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateDebut", is("2025-10-01")))
                .andExpect(jsonPath("$.dateFin", is("2025-10-31")))
                .andExpect(jsonPath("$.nombreVentes", is(2)))
                .andExpect(jsonPath("$.chiffreAffaires", is(990000.00)))
                .andExpect(jsonPath("$.ventes", hasSize(2)));

        verify(venteService, times(1)).obtenirVentesParPeriode(dateDebut, dateFin);
        verify(venteService, times(1)).calculerChiffreAffaires(dateDebut, dateFin);
    }

    @Test
    @DisplayName("GET /ventes/statistiques - Devrait retourner statistiques vides si aucune vente")
    void obtenirStatistiques_DevraitRetournerStatistiquesVides() throws Exception {
        // Arrange
        LocalDate dateDebut = LocalDate.of(2025, 11, 1);
        LocalDate dateFin = LocalDate.of(2025, 11, 30);
        BigDecimal chiffreAffaires = BigDecimal.ZERO;

        when(venteService.obtenirVentesParPeriode(dateDebut, dateFin)).thenReturn(Arrays.asList());
        when(venteService.calculerChiffreAffaires(dateDebut, dateFin)).thenReturn(chiffreAffaires);

        // Act & Assert
        mockMvc.perform(get("/ventes/statistiques")
                        .param("debut", dateDebut.toString())
                        .param("fin", dateFin.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreVentes", is(0)))
                .andExpect(jsonPath("$.chiffreAffaires", is(0)))
                .andExpect(jsonPath("$.ventes", hasSize(0)));

        verify(venteService, times(1)).obtenirVentesParPeriode(dateDebut, dateFin);
        verify(venteService, times(1)).calculerChiffreAffaires(dateDebut, dateFin);
    }

    @Test
    @DisplayName("GET /ventes/statistiques - Devrait retourner 400 si dates invalides")
    void obtenirStatistiques_DevraitRetourner400SiDatesInvalides() throws Exception {
        // Act & Assert - Dates mal formatées
        mockMvc.perform(get("/ventes/statistiques")
                        .param("debut", "date-invalide")
                        .param("fin", "2025-10-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Le service ne devrait pas être appelé
        verify(venteService, never()).obtenirVentesParPeriode(any(), any());
    }

    // ==================== Tests additionnels ====================

    @Test
    @DisplayName("GET /ventes - Devrait gérer correctement les ventes avec client null")
    void obtenirTous_DevraitGererClientNull() throws Exception {
        // Arrange
        VenteEntity venteSansClient = VenteEntity.builder()
                .quantite(2)
                .nomProduit("Montre")
                .prixUnitaire(new BigDecimal("120000.00"))
                .prixTotal(new BigDecimal("240000.00"))
                .dateVente(LocalDate.now())
                .client(null) // Pas de client
                .utilisateur(utilisateurTest)
                .build();
        venteSansClient.setId("test-id-3");

        PagedResponse<VenteDto> page = PagedResponse.<VenteDto>builder()
                .content(Arrays.asList(VenteDto.fromEntity(venteSansClient)))
                .currentPage(0).pageSize(20).totalElements(1L).totalPages(1).first(true).last(true).build();
        when(venteService.obtenirVentesPaginees(0, 20, null, null, null)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/ventes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].client").doesNotExist());

        verify(venteService, times(1)).obtenirVentesPaginees(0, 20, null, null, null);
    }

    @Test
    @DisplayName("POST /ventes - Devrait calculer automatiquement le prix total")
    void creer_DevraitCalculerPrixTotal() throws Exception {
        // Arrange
        VenteEntity nouvelleVente = VenteEntity.builder()
                .quantite(7)
                .nomProduit("Chaîne en or")
                .prixUnitaire(new BigDecimal("90000.00"))
                .dateVente(LocalDate.now())
                .build();

        // Le service devrait calculer : 7 × 90000.00 = 630000.00
        VenteEntity venteCreee = VenteEntity.builder()
                .quantite(7)
                .nomProduit("Chaîne en or")
                .prixUnitaire(new BigDecimal("90000.00"))
                .prixTotal(new BigDecimal("630000.00")) // Calculé automatiquement
                .dateVente(LocalDate.now())
                .utilisateur(utilisateurTest)
                .build();
        venteCreee.setId("test-id-4");

        when(userService.obtenirUtilisateurParEmail("amadou@example.com")).thenReturn(utilisateurTest);
        when(venteService.creerVente(any(VenteEntity.class), eq(utilisateurTest)))
                .thenReturn(venteCreee);

        // Act & Assert
        mockMvc.perform(post("/ventes")
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null, List.of(new SimpleGrantedAuthority("USER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nouvelleVente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixTotal", is(630000.00)));

        verify(venteService, times(1)).creerVente(any(VenteEntity.class), eq(utilisateurTest));
    }

    @Test
    @DisplayName("GET /ventes/chiffre-affaires - Devrait retourner 400 si dates invalides")
    void calculerChiffreAffaires_DevraitRetourner400SiDatesInvalides() throws Exception {
        // Act & Assert - Dates mal formatées
        mockMvc.perform(get("/ventes/chiffre-affaires")
                        .param("debut", "date-invalide")
                        .param("fin", "2025-10-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Le service ne devrait pas être appelé
        verify(venteService, never()).calculerChiffreAffaires(any(), any());
    }
}
