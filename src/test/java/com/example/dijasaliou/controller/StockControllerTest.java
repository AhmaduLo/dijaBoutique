package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.service.StockService;
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
 * Tests unitaires pour StockController
 *
 * Bonnes pratiques appliquées :
 * - @WebMvcTest : Test uniquement la couche controller (léger et rapide)
 * - @MockitoBean : Mock les dépendances (services)
 * - MockMvc : Simule les appels HTTP
 * - @DisplayName : Descriptions claires des tests
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - Vérification des status HTTP et du contenu JSON
 */
@WebMvcTest(controllers = StockController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du StockController")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockService stockService;

    // Mocker les beans de sécurité pour éviter les problèmes de dépendances
    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    private StockDto stockTest1;
    private StockDto stockTest2;
    private StockDto stockTest3;

    /**
     * Initialisation des données de test avant chaque test
     */
    @BeforeEach
    void setUp() {
        // Produit 1 : Stock normal (EN_STOCK)
        stockTest1 = StockDto.builder()
                .nomProduit("Collier en or")
                .quantiteAchetee(50)
                .quantiteVendue(30)
                .stockDisponible(20)
                .prixMoyenAchat(new BigDecimal("100000.00"))
                .prixMoyenVente(new BigDecimal("150000.00"))
                .valeurStock(new BigDecimal("2000000.00"))
                .margeUnitaire(new BigDecimal("50000.00"))
                .statut(StockDto.StatutStock.EN_STOCK)
                .build();

        // Produit 2 : Stock bas (STOCK_BAS)
        stockTest2 = StockDto.builder()
                .nomProduit("Bracelet en argent")
                .quantiteAchetee(15)
                .quantiteVendue(10)
                .stockDisponible(5)
                .prixMoyenAchat(new BigDecimal("50000.00"))
                .prixMoyenVente(new BigDecimal("80000.00"))
                .valeurStock(new BigDecimal("250000.00"))
                .margeUnitaire(new BigDecimal("30000.00"))
                .statut(StockDto.StatutStock.STOCK_BAS)
                .build();

        // Produit 3 : Rupture de stock (RUPTURE)
        stockTest3 = StockDto.builder()
                .nomProduit("Bague en diamant")
                .quantiteAchetee(10)
                .quantiteVendue(10)
                .stockDisponible(0)
                .prixMoyenAchat(new BigDecimal("200000.00"))
                .prixMoyenVente(new BigDecimal("300000.00"))
                .valeurStock(BigDecimal.ZERO)
                .margeUnitaire(new BigDecimal("100000.00"))
                .statut(StockDto.StatutStock.RUPTURE)
                .build();
    }

    // ==================== Tests pour GET /stock ====================

    @Test
    @DisplayName("GET /stock - Devrait retourner tous les stocks")
    void obtenirTousLesStocks_DevraitRetournerTousLesStocks() throws Exception {
        // Arrange
        List<StockDto> stocks = Arrays.asList(stockTest1, stockTest2, stockTest3);
        when(stockService.obtenirTousLesStocks()).thenReturn(stocks);

        // Act & Assert
        mockMvc.perform(get("/stock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$[0].stockDisponible", is(20)))
                .andExpect(jsonPath("$[0].statut", is("EN_STOCK")))
                .andExpect(jsonPath("$[1].nomProduit", is("Bracelet en argent")))
                .andExpect(jsonPath("$[1].stockDisponible", is(5)))
                .andExpect(jsonPath("$[1].statut", is("STOCK_BAS")))
                .andExpect(jsonPath("$[2].nomProduit", is("Bague en diamant")))
                .andExpect(jsonPath("$[2].stockDisponible", is(0)))
                .andExpect(jsonPath("$[2].statut", is("RUPTURE")));

        verify(stockService, times(1)).obtenirTousLesStocks();
    }

    @Test
    @DisplayName("GET /stock - Devrait retourner une liste vide quand aucun stock")
    void obtenirTousLesStocks_DevraitRetournerListeVide() throws Exception {
        // Arrange
        when(stockService.obtenirTousLesStocks()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/stock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(stockService, times(1)).obtenirTousLesStocks();
    }

    // ==================== Tests pour GET /stock/produit/{nomProduit} ====================

    @Test
    @DisplayName("GET /stock/produit/{nomProduit} - Devrait retourner le stock d'un produit")
    void obtenirStockParProduit_DevraitRetournerStock() throws Exception {
        // Arrange
        String nomProduit = "Collier en or";
        when(stockService.obtenirStockParNomProduit(nomProduit)).thenReturn(stockTest1);

        // Act & Assert
        mockMvc.perform(get("/stock/produit/{nomProduit}", nomProduit)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$.quantiteAchetee", is(50)))
                .andExpect(jsonPath("$.quantiteVendue", is(30)))
                .andExpect(jsonPath("$.stockDisponible", is(20)))
                .andExpect(jsonPath("$.prixMoyenAchat", is(100000.00)))
                .andExpect(jsonPath("$.prixMoyenVente", is(150000.00)))
                .andExpect(jsonPath("$.valeurStock", is(2000000.00)))
                .andExpect(jsonPath("$.margeUnitaire", is(50000.00)));

        verify(stockService, times(1)).obtenirStockParNomProduit(nomProduit);
    }

    @Test
    @DisplayName("GET /stock/produit/{nomProduit} - Devrait lancer une exception si produit inexistant")
    void obtenirStockParProduit_DevraitLancerExceptionSiProduitInexistant() {
        // Arrange
        String nomProduit = "Produit Inexistant";
        when(stockService.obtenirStockParNomProduit(nomProduit))
                .thenThrow(new RuntimeException("Produit non trouvé"));

        // Act & Assert
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/stock/produit/{nomProduit}", nomProduit)
                    .contentType(MediaType.APPLICATION_JSON));
        });

        verify(stockService, times(1)).obtenirStockParNomProduit(nomProduit);
    }

    // ==================== Tests pour GET /stock/rupture ====================

    @Test
    @DisplayName("GET /stock/rupture - Devrait retourner les produits en rupture")
    void obtenirProduitsEnRupture_DevraitRetournerProduits() throws Exception {
        // Arrange
        List<StockDto> ruptures = Arrays.asList(stockTest3);
        when(stockService.obtenirProduitsEnRupture()).thenReturn(ruptures);

        // Act & Assert
        mockMvc.perform(get("/stock/rupture")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nomProduit", is("Bague en diamant")))
                .andExpect(jsonPath("$[0].stockDisponible", is(0)))
                .andExpect(jsonPath("$[0].statut", is("RUPTURE")));

        verify(stockService, times(1)).obtenirProduitsEnRupture();
    }

    @Test
    @DisplayName("GET /stock/rupture - Devrait retourner liste vide si aucune rupture")
    void obtenirProduitsEnRupture_DevraitRetournerListeVide() throws Exception {
        // Arrange
        when(stockService.obtenirProduitsEnRupture()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/stock/rupture")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(stockService, times(1)).obtenirProduitsEnRupture();
    }

    // ==================== Tests pour GET /stock/stock-bas ====================

    @Test
    @DisplayName("GET /stock/stock-bas - Devrait retourner les produits à stock bas")
    void obtenirProduitsStockBas_DevraitRetournerProduits() throws Exception {
        // Arrange
        List<StockDto> stocksBas = Arrays.asList(stockTest2);
        when(stockService.obtenirProduitsStockBas()).thenReturn(stocksBas);

        // Act & Assert
        mockMvc.perform(get("/stock/stock-bas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nomProduit", is("Bracelet en argent")))
                .andExpect(jsonPath("$[0].stockDisponible", is(5)))
                .andExpect(jsonPath("$[0].statut", is("STOCK_BAS")));

        verify(stockService, times(1)).obtenirProduitsStockBas();
    }

    @Test
    @DisplayName("GET /stock/stock-bas - Devrait retourner liste vide si aucun stock bas")
    void obtenirProduitsStockBas_DevraitRetournerListeVide() throws Exception {
        // Arrange
        when(stockService.obtenirProduitsStockBas()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/stock/stock-bas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(stockService, times(1)).obtenirProduitsStockBas();
    }

    // ==================== Tests pour GET /stock/alertes ====================

    @Test
    @DisplayName("GET /stock/alertes - Devrait retourner toutes les alertes")
    void obtenirAlertes_DevraitRetournerAlertes() throws Exception {
        // Arrange
        List<StockDto> ruptures = Arrays.asList(stockTest3);
        List<StockDto> stocksBas = Arrays.asList(stockTest2);
        when(stockService.obtenirProduitsEnRupture()).thenReturn(ruptures);
        when(stockService.obtenirProduitsStockBas()).thenReturn(stocksBas);

        // Act & Assert
        mockMvc.perform(get("/stock/alertes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruptures", hasSize(1)))
                .andExpect(jsonPath("$.nombreRuptures", is(1)))
                .andExpect(jsonPath("$.stocksBas", hasSize(1)))
                .andExpect(jsonPath("$.nombreStocksBas", is(1)))
                .andExpect(jsonPath("$.nombreTotalAlertes", is(2)));

        verify(stockService, times(1)).obtenirProduitsEnRupture();
        verify(stockService, times(1)).obtenirProduitsStockBas();
    }

    @Test
    @DisplayName("GET /stock/alertes - Devrait retourner zéro alerte si aucun problème")
    void obtenirAlertes_DevraitRetournerZeroAlerte() throws Exception {
        // Arrange
        when(stockService.obtenirProduitsEnRupture()).thenReturn(Arrays.asList());
        when(stockService.obtenirProduitsStockBas()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/stock/alertes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreRuptures", is(0)))
                .andExpect(jsonPath("$.nombreStocksBas", is(0)))
                .andExpect(jsonPath("$.nombreTotalAlertes", is(0)));

        verify(stockService, times(1)).obtenirProduitsEnRupture();
        verify(stockService, times(1)).obtenirProduitsStockBas();
    }

    // ==================== Tests pour GET /stock/verifier ====================

    @Test
    @DisplayName("GET /stock/verifier - Devrait confirmer que le stock est suffisant")
    void verifierStock_DevraitConfirmerStockSuffisant() throws Exception {
        // Arrange
        String nomProduit = "Collier en or";
        Integer quantite = 10;
        when(stockService.verifierStockDisponible(nomProduit, quantite)).thenReturn(true);
        when(stockService.obtenirStockParNomProduit(nomProduit)).thenReturn(stockTest1);

        // Act & Assert
        mockMvc.perform(get("/stock/verifier")
                        .param("nomProduit", nomProduit)
                        .param("quantite", quantite.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomProduit", is(nomProduit)))
                .andExpect(jsonPath("$.quantiteDemandee", is(10)))
                .andExpect(jsonPath("$.stockDisponible", is(20)))
                .andExpect(jsonPath("$.disponible", is(true)))
                .andExpect(jsonPath("$.message", is("Stock suffisant pour cette vente")));

        verify(stockService, times(1)).verifierStockDisponible(nomProduit, quantite);
        verify(stockService, times(1)).obtenirStockParNomProduit(nomProduit);
    }

    @Test
    @DisplayName("GET /stock/verifier - Devrait indiquer que le stock est insuffisant")
    void verifierStock_DevraitIndiquerStockInsuffisant() throws Exception {
        // Arrange
        String nomProduit = "Bracelet en argent";
        Integer quantite = 10;
        when(stockService.verifierStockDisponible(nomProduit, quantite)).thenReturn(false);
        when(stockService.obtenirStockParNomProduit(nomProduit)).thenReturn(stockTest2);

        // Act & Assert
        mockMvc.perform(get("/stock/verifier")
                        .param("nomProduit", nomProduit)
                        .param("quantite", quantite.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomProduit", is(nomProduit)))
                .andExpect(jsonPath("$.quantiteDemandee", is(10)))
                .andExpect(jsonPath("$.stockDisponible", is(5)))
                .andExpect(jsonPath("$.disponible", is(false)))
                .andExpect(jsonPath("$.message", containsString("Stock insuffisant")));

        verify(stockService, times(1)).verifierStockDisponible(nomProduit, quantite);
        verify(stockService, times(1)).obtenirStockParNomProduit(nomProduit);
    }

    @Test
    @DisplayName("GET /stock/verifier - Devrait gérer un produit inexistant")
    void verifierStock_DevraitGererProduitInexistant() throws Exception {
        // Arrange
        String nomProduit = "Produit Inexistant";
        Integer quantite = 5;
        when(stockService.verifierStockDisponible(nomProduit, quantite)).thenReturn(false);
        when(stockService.obtenirStockParNomProduit(nomProduit))
                .thenThrow(new RuntimeException("Produit non trouvé"));

        // Act & Assert
        mockMvc.perform(get("/stock/verifier")
                        .param("nomProduit", nomProduit)
                        .param("quantite", quantite.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockDisponible", is(0)))
                .andExpect(jsonPath("$.disponible", is(false)));

        verify(stockService, times(1)).verifierStockDisponible(nomProduit, quantite);
    }

    // ==================== Tests pour GET /stock/valeur-totale ====================

    @Test
    @DisplayName("GET /stock/valeur-totale - Devrait retourner la valeur totale du stock")
    void obtenirValeurTotale_DevraitRetournerValeurTotale() throws Exception {
        // Arrange
        BigDecimal valeurTotale = new BigDecimal("2250000.00");
        List<StockDto> stocks = Arrays.asList(stockTest1, stockTest2, stockTest3);
        when(stockService.obtenirValeurTotaleStock()).thenReturn(valeurTotale);
        when(stockService.obtenirTousLesStocks()).thenReturn(stocks);

        // Act & Assert
        mockMvc.perform(get("/stock/valeur-totale")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valeurTotale", is(2250000.00)))
                .andExpect(jsonPath("$.nombreProduits", is(3)))
                .andExpect(jsonPath("$.details", hasSize(3)));

        verify(stockService, times(1)).obtenirValeurTotaleStock();
        verify(stockService, times(1)).obtenirTousLesStocks();
    }

    @Test
    @DisplayName("GET /stock/valeur-totale - Devrait retourner zéro si aucun stock")
    void obtenirValeurTotale_DevraitRetournerZero() throws Exception {
        // Arrange
        when(stockService.obtenirValeurTotaleStock()).thenReturn(BigDecimal.ZERO);
        when(stockService.obtenirTousLesStocks()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/stock/valeur-totale")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valeurTotale", is(0)))
                .andExpect(jsonPath("$.nombreProduits", is(0)));

        verify(stockService, times(1)).obtenirValeurTotaleStock();
        verify(stockService, times(1)).obtenirTousLesStocks();
    }

    // ==================== Tests pour GET /stock/resume ====================

    @Test
    @DisplayName("GET /stock/resume - Devrait retourner un résumé complet")
    void obtenirResume_DevraitRetournerResume() throws Exception {
        // Arrange
        List<StockDto> stocks = Arrays.asList(stockTest1, stockTest2, stockTest3);
        BigDecimal valeurTotale = new BigDecimal("2250000.00");
        when(stockService.obtenirTousLesStocks()).thenReturn(stocks);
        when(stockService.obtenirValeurTotaleStock()).thenReturn(valeurTotale);

        // Act & Assert
        mockMvc.perform(get("/stock/resume")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotalProduits", is(3)))
                .andExpect(jsonPath("$.produitsEnStock", is(2))) // stockTest1 et stockTest2
                .andExpect(jsonPath("$.produitsEnRupture", is(1))) // stockTest3
                .andExpect(jsonPath("$.produitsStockBas", is(1))) // stockTest2
                .andExpect(jsonPath("$.valeurTotaleStock", is(2250000.00)));

        verify(stockService, times(1)).obtenirTousLesStocks();
        verify(stockService, times(1)).obtenirValeurTotaleStock();
    }

    // ==================== Tests pour GET /stock/produits-disponibles ====================

    @Test
    @DisplayName("GET /stock/produits-disponibles - Devrait retourner les produits disponibles")
    void obtenirProduitsDisponibles_DevraitRetournerProduits() throws Exception {
        // Arrange
        List<StockDto> stocks = Arrays.asList(stockTest1, stockTest2, stockTest3);
        when(stockService.obtenirTousLesStocks()).thenReturn(stocks);

        // Act & Assert
        mockMvc.perform(get("/stock/produits-disponibles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Seulement stockTest1 et stockTest2 (stock > 0)
                .andExpect(jsonPath("$[0].nomProduit", notNullValue()))
                .andExpect(jsonPath("$[0].stockDisponible", notNullValue()))
                .andExpect(jsonPath("$[0].prixMoyenVente", notNullValue()))
                .andExpect(jsonPath("$[0].statut", notNullValue()));

        verify(stockService, times(1)).obtenirTousLesStocks();
    }

    @Test
    @DisplayName("GET /stock/produits-disponibles - Devrait retourner liste vide si aucun produit disponible")
    void obtenirProduitsDisponibles_DevraitRetournerListeVide() throws Exception {
        // Arrange
        List<StockDto> stocks = Arrays.asList(stockTest3); // Seulement en rupture
        when(stockService.obtenirTousLesStocks()).thenReturn(stocks);

        // Act & Assert
        mockMvc.perform(get("/stock/produits-disponibles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(stockService, times(1)).obtenirTousLesStocks();
    }

    // ==================== Tests pour GET /stock/noms-produits ====================

    @Test
    @DisplayName("GET /stock/noms-produits - Devrait retourner la liste des noms de produits disponibles")
    void obtenirNomsProduits_DevraitRetournerNoms() throws Exception {
        // Arrange
        List<StockDto> stocks = Arrays.asList(stockTest1, stockTest2, stockTest3);
        when(stockService.obtenirTousLesStocks()).thenReturn(stocks);

        // Act & Assert
        mockMvc.perform(get("/stock/noms-produits")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Seulement les produits avec stock > 0
                .andExpect(jsonPath("$[*]", hasItem("Collier en or")))
                .andExpect(jsonPath("$[*]", hasItem("Bracelet en argent")));

        verify(stockService, times(1)).obtenirTousLesStocks();
    }

    @Test
    @DisplayName("GET /stock/noms-produits - Devrait retourner liste vide si aucun produit disponible")
    void obtenirNomsProduits_DevraitRetournerListeVide() throws Exception {
        // Arrange
        when(stockService.obtenirTousLesStocks()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/stock/noms-produits")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(stockService, times(1)).obtenirTousLesStocks();
    }

    // ==================== Tests additionnels ====================

    @Test
    @DisplayName("GET /stock - Devrait calculer correctement les valeurs de stock")
    void obtenirTousLesStocks_DevraitCalculerValeursCorrectement() throws Exception {
        // Arrange
        when(stockService.obtenirTousLesStocks()).thenReturn(Arrays.asList(stockTest1));

        // Act & Assert
        mockMvc.perform(get("/stock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantiteAchetee", is(50)))
                .andExpect(jsonPath("$[0].quantiteVendue", is(30)))
                .andExpect(jsonPath("$[0].stockDisponible", is(20))) // 50 - 30 = 20
                .andExpect(jsonPath("$[0].valeurStock", is(2000000.00))) // 20 × 100000 = 2000000
                .andExpect(jsonPath("$[0].margeUnitaire", is(50000.00))); // 150000 - 100000 = 50000

        verify(stockService, times(1)).obtenirTousLesStocks();
    }

    @Test
    @DisplayName("GET /stock/alertes - Devrait combiner correctement ruptures et stocks bas")
    void obtenirAlertes_DevraitCombinerCorrectement() throws Exception {
        // Arrange
        StockDto stockBas1 = StockDto.builder().nomProduit("Produit A").stockDisponible(3).statut(StockDto.StatutStock.STOCK_BAS).build();
        StockDto stockBas2 = StockDto.builder().nomProduit("Produit B").stockDisponible(7).statut(StockDto.StatutStock.STOCK_BAS).build();
        StockDto rupture1 = StockDto.builder().nomProduit("Produit C").stockDisponible(0).statut(StockDto.StatutStock.RUPTURE).build();

        when(stockService.obtenirProduitsEnRupture()).thenReturn(Arrays.asList(rupture1));
        when(stockService.obtenirProduitsStockBas()).thenReturn(Arrays.asList(stockBas1, stockBas2));

        // Act & Assert
        mockMvc.perform(get("/stock/alertes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreRuptures", is(1)))
                .andExpect(jsonPath("$.nombreStocksBas", is(2)))
                .andExpect(jsonPath("$.nombreTotalAlertes", is(3))); // 1 + 2

        verify(stockService, times(1)).obtenirProduitsEnRupture();
        verify(stockService, times(1)).obtenirProduitsStockBas();
    }
}
