package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.CreateDeviseDto;
import com.example.dijasaliou.dto.UpdateDeviseDto;
import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.service.DeviseService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour DeviseController
 *
 * Bonnes pratiques appliquées :
 * - @WebMvcTest : Test uniquement la couche controller
 * - @MockitoBean : Mock les dépendances (services)
 * - MockMvc : Simule les appels HTTP
 * - @DisplayName : Descriptions claires des tests
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - Vérification des status HTTP et du contenu JSON
 */
@WebMvcTest(controllers = DeviseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du DeviseController")
class DeviseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeviseService deviseService;

    // Mocker les beans de sécurité pour éviter les problèmes de dépendances
    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    private DeviseEntity deviseXOF;
    private DeviseEntity deviseEUR;
    private DeviseEntity deviseUSD;
    private CreateDeviseDto createDeviseDto;
    private UpdateDeviseDto updateDeviseDto;

    /**
     * Initialisation des données de test avant chaque test
     */
    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        // Devise XOF (Franc CFA) - devise de référence par défaut
        deviseXOF = DeviseEntity.builder()
                .id(1L)
                .code("XOF")
                .nom("Franc CFA")
                .symbole("CFA")
                .pays("Sénégal")
                .tauxChange(1.0)
                .isDefault(true)
                .dateCreation(now)
                .build();

        // Devise EUR (Euro)
        deviseEUR = DeviseEntity.builder()
                .id(2L)
                .code("EUR")
                .nom("Euro")
                .symbole("€")
                .pays("Zone Euro")
                .tauxChange(655.957)
                .isDefault(false)
                .dateCreation(now)
                .build();

        // Devise USD (Dollar)
        deviseUSD = DeviseEntity.builder()
                .id(3L)
                .code("USD")
                .nom("Dollar américain")
                .symbole("$")
                .pays("États-Unis")
                .tauxChange(600.0)
                .isDefault(false)
                .dateCreation(now)
                .build();

        // DTO de création
        createDeviseDto = CreateDeviseDto.builder()
                .code("GBP")
                .nom("Livre sterling")
                .symbole("£")
                .pays("Royaume-Uni")
                .tauxChange(750.0)
                .isDefault(false)
                .build();

        // DTO de mise à jour
        updateDeviseDto = UpdateDeviseDto.builder()
                .nom("Dollar américain (modifié)")
                .tauxChange(620.0)
                .build();
    }

    // ==================== Tests pour GET /devises ====================

    @Test
    @DisplayName("GET /devises - Devrait retourner toutes les devises")
    void obtenirToutesLesDevises_DevraitRetournerToutesLesDevises() throws Exception {
        // Arrange
        List<DeviseEntity> devises = Arrays.asList(deviseXOF, deviseEUR, deviseUSD);
        when(deviseService.obtenirToutesLesDevises()).thenReturn(devises);

        // Act & Assert
        mockMvc.perform(get("/devises")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].code", is("XOF")))
                .andExpect(jsonPath("$[0].nom", is("Franc CFA")))
                .andExpect(jsonPath("$[0].symbole", is("CFA")))
                .andExpect(jsonPath("$[0].tauxChange", is(1.0)))
                .andExpect(jsonPath("$[0].isDefault", is(true)))
                .andExpect(jsonPath("$[1].code", is("EUR")))
                .andExpect(jsonPath("$[2].code", is("USD")));

        verify(deviseService, times(1)).obtenirToutesLesDevises();
    }

    @Test
    @DisplayName("GET /devises - Devrait retourner une liste vide quand aucune devise")
    void obtenirToutesLesDevises_DevraitRetournerListeVide() throws Exception {
        // Arrange
        when(deviseService.obtenirToutesLesDevises()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/devises")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(deviseService, times(1)).obtenirToutesLesDevises();
    }

    // ==================== Tests pour GET /devises/{id} ====================

    @Test
    @DisplayName("GET /devises/{id} - Devrait retourner une devise par son ID")
    void obtenirDeviseParId_DevraitRetournerDevise() throws Exception {
        // Arrange
        Long deviseId = 2L;
        when(deviseService.obtenirDeviseParId(deviseId)).thenReturn(deviseEUR);

        // Act & Assert
        mockMvc.perform(get("/devises/{id}", deviseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.code", is("EUR")))
                .andExpect(jsonPath("$.nom", is("Euro")))
                .andExpect(jsonPath("$.symbole", is("€")))
                .andExpect(jsonPath("$.pays", is("Zone Euro")))
                .andExpect(jsonPath("$.tauxChange", is(655.957)))
                .andExpect(jsonPath("$.isDefault", is(false)));

        verify(deviseService, times(1)).obtenirDeviseParId(deviseId);
    }

    @Test
    @DisplayName("GET /devises/{id} - Devrait retourner la devise par défaut")
    void obtenirDeviseParId_DevraitRetournerDeviseParDefaut() throws Exception {
        // Arrange
        Long deviseId = 1L;
        when(deviseService.obtenirDeviseParId(deviseId)).thenReturn(deviseXOF);

        // Act & Assert
        mockMvc.perform(get("/devises/{id}", deviseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.code", is("XOF")))
                .andExpect(jsonPath("$.isDefault", is(true)));

        verify(deviseService, times(1)).obtenirDeviseParId(deviseId);
    }

    // ==================== Tests pour GET /devises/default ====================

    @Test
    @DisplayName("GET /devises/default - Devrait retourner la devise par défaut")
    void obtenirDeviseParDefaut_DevraitRetournerDeviseParDefaut() throws Exception {
        // Arrange
        when(deviseService.obtenirDeviseParDefaut()).thenReturn(deviseXOF);

        // Act & Assert
        mockMvc.perform(get("/devises/default")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.code", is("XOF")))
                .andExpect(jsonPath("$.nom", is("Franc CFA")))
                .andExpect(jsonPath("$.isDefault", is(true)))
                .andExpect(jsonPath("$.tauxChange", is(1.0)));

        verify(deviseService, times(1)).obtenirDeviseParDefaut();
    }

    // ==================== Tests pour POST /devises ====================

    @Test
    @DisplayName("POST /devises - Devrait créer une nouvelle devise")
    void creerDevise_DevraitCreerNouvelleDevise() throws Exception {
        // Arrange
        DeviseEntity nouvelleDevise = DeviseEntity.builder()
                .id(4L)
                .code("GBP")
                .nom("Livre sterling")
                .symbole("£")
                .pays("Royaume-Uni")
                .tauxChange(750.0)
                .isDefault(false)
                .dateCreation(LocalDateTime.now())
                .build();

        when(deviseService.creerDevise(any(CreateDeviseDto.class))).thenReturn(nouvelleDevise);

        // Act & Assert
        mockMvc.perform(post("/devises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDeviseDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(4)))
                .andExpect(jsonPath("$.code", is("GBP")))
                .andExpect(jsonPath("$.nom", is("Livre sterling")))
                .andExpect(jsonPath("$.symbole", is("£")))
                .andExpect(jsonPath("$.pays", is("Royaume-Uni")))
                .andExpect(jsonPath("$.tauxChange", is(750.0)))
                .andExpect(jsonPath("$.isDefault", is(false)));

        verify(deviseService, times(1)).creerDevise(any(CreateDeviseDto.class));
    }

    @Test
    @DisplayName("POST /devises - Devrait créer une devise avec isDefault true")
    void creerDevise_DevraitCreerDeviseAvecIsDefaultTrue() throws Exception {
        // Arrange
        CreateDeviseDto dtoAvecDefault = CreateDeviseDto.builder()
                .code("MAD")
                .nom("Dirham marocain")
                .symbole("MAD")
                .pays("Maroc")
                .tauxChange(58.0)
                .isDefault(true)
                .build();

        DeviseEntity nouvelleDevise = DeviseEntity.builder()
                .id(5L)
                .code("MAD")
                .nom("Dirham marocain")
                .symbole("MAD")
                .pays("Maroc")
                .tauxChange(58.0)
                .isDefault(true)
                .dateCreation(LocalDateTime.now())
                .build();

        when(deviseService.creerDevise(any(CreateDeviseDto.class))).thenReturn(nouvelleDevise);

        // Act & Assert
        mockMvc.perform(post("/devises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoAvecDefault)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault", is(true)));

        verify(deviseService, times(1)).creerDevise(any(CreateDeviseDto.class));
    }

    // ==================== Tests pour PUT /devises/{id} ====================

    @Test
    @DisplayName("PUT /devises/{id} - Devrait modifier une devise")
    void modifierDevise_DevraitModifierDevise() throws Exception {
        // Arrange
        Long deviseId = 3L;
        DeviseEntity deviseModifiee = DeviseEntity.builder()
                .id(deviseId)
                .code("USD")
                .nom("Dollar américain (modifié)")
                .symbole("$")
                .pays("États-Unis")
                .tauxChange(620.0)
                .isDefault(false)
                .dateCreation(LocalDateTime.now())
                .build();

        when(deviseService.modifierDevise(eq(deviseId), any(UpdateDeviseDto.class)))
                .thenReturn(deviseModifiee);

        // Act & Assert
        mockMvc.perform(put("/devises/{id}", deviseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDeviseDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.nom", is("Dollar américain (modifié)")))
                .andExpect(jsonPath("$.tauxChange", is(620.0)));

        verify(deviseService, times(1)).modifierDevise(eq(deviseId), any(UpdateDeviseDto.class));
    }

    @Test
    @DisplayName("PUT /devises/{id} - Devrait modifier partiellement une devise")
    void modifierDevise_DevraitModifierPartiellement() throws Exception {
        // Arrange
        Long deviseId = 2L;
        UpdateDeviseDto updatePartiel = UpdateDeviseDto.builder()
                .tauxChange(660.0)
                .build();

        DeviseEntity deviseModifiee = DeviseEntity.builder()
                .id(deviseId)
                .code("EUR")
                .nom("Euro")
                .symbole("€")
                .pays("Zone Euro")
                .tauxChange(660.0)
                .isDefault(false)
                .dateCreation(LocalDateTime.now())
                .build();

        when(deviseService.modifierDevise(eq(deviseId), any(UpdateDeviseDto.class)))
                .thenReturn(deviseModifiee);

        // Act & Assert
        mockMvc.perform(put("/devises/{id}", deviseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePartiel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tauxChange", is(660.0)));

        verify(deviseService, times(1)).modifierDevise(eq(deviseId), any(UpdateDeviseDto.class));
    }

    // ==================== Tests pour DELETE /devises/{id} ====================

    @Test
    @DisplayName("DELETE /devises/{id} - Devrait supprimer une devise")
    void supprimerDevise_DevraitSupprimerDevise() throws Exception {
        // Arrange
        Long deviseId = 3L;
        doNothing().when(deviseService).supprimerDevise(deviseId);

        // Act & Assert
        mockMvc.perform(delete("/devises/{id}", deviseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(deviseService, times(1)).supprimerDevise(deviseId);
    }

    // ==================== Tests pour PUT /devises/{id}/set-default ====================

    @Test
    @DisplayName("PUT /devises/{id}/set-default - Devrait définir une devise comme devise par défaut")
    void definirDeviseParDefaut_DevraitDefinirDeviseParDefaut() throws Exception {
        // Arrange
        Long deviseId = 2L;
        DeviseEntity deviseEURDefault = DeviseEntity.builder()
                .id(deviseId)
                .code("EUR")
                .nom("Euro")
                .symbole("€")
                .pays("Zone Euro")
                .tauxChange(655.957)
                .isDefault(true)
                .dateCreation(LocalDateTime.now())
                .build();

        when(deviseService.definirDeviseParDefaut(deviseId)).thenReturn(deviseEURDefault);

        // Act & Assert
        mockMvc.perform(put("/devises/{id}/set-default", deviseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.code", is("EUR")))
                .andExpect(jsonPath("$.isDefault", is(true)));

        verify(deviseService, times(1)).definirDeviseParDefaut(deviseId);
    }

    @Test
    @DisplayName("PUT /devises/{id}/set-default - Devrait changer la devise par défaut")
    void definirDeviseParDefaut_DevraitChangerDeviseParDefaut() throws Exception {
        // Arrange
        Long deviseId = 3L;
        DeviseEntity deviseUSDDefault = DeviseEntity.builder()
                .id(deviseId)
                .code("USD")
                .nom("Dollar américain")
                .symbole("$")
                .pays("États-Unis")
                .tauxChange(600.0)
                .isDefault(true)
                .dateCreation(LocalDateTime.now())
                .build();

        when(deviseService.definirDeviseParDefaut(deviseId)).thenReturn(deviseUSDDefault);

        // Act & Assert
        mockMvc.perform(put("/devises/{id}/set-default", deviseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault", is(true)));

        verify(deviseService, times(1)).definirDeviseParDefaut(deviseId);
    }

    // ==================== Tests pour POST /devises/convertir ====================

    @Test
    @DisplayName("POST /devises/convertir - Devrait convertir un montant entre devises")
    void convertir_DevraitConvertirMontant() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("montant", 100.0);
        request.put("deviseSource", "EUR");
        request.put("deviseCible", "XOF");

        when(deviseService.convertir(100.0, "EUR", "XOF")).thenReturn(65595.7);

        // Act & Assert
        mockMvc.perform(post("/devises/convertir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant", is(100.0)))
                .andExpect(jsonPath("$.deviseSource", is("EUR")))
                .andExpect(jsonPath("$.deviseCible", is("XOF")))
                .andExpect(jsonPath("$.montantConverti", is(65595.7)));

        verify(deviseService, times(1)).convertir(100.0, "EUR", "XOF");
    }

    @Test
    @DisplayName("POST /devises/convertir - Devrait convertir USD vers EUR")
    void convertir_DevraitConvertirUSDVersEUR() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("montant", 100.0);
        request.put("deviseSource", "USD");
        request.put("deviseCible", "EUR");

        when(deviseService.convertir(100.0, "USD", "EUR")).thenReturn(91.5);

        // Act & Assert
        mockMvc.perform(post("/devises/convertir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant", is(100.0)))
                .andExpect(jsonPath("$.deviseSource", is("USD")))
                .andExpect(jsonPath("$.deviseCible", is("EUR")))
                .andExpect(jsonPath("$.montantConverti", is(91.5)));

        verify(deviseService, times(1)).convertir(100.0, "USD", "EUR");
    }

    @Test
    @DisplayName("POST /devises/convertir - Devrait convertir avec un montant décimal")
    void convertir_DevraitConvertirAvecMontantDecimal() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("montant", 150.50);
        request.put("deviseSource", "EUR");
        request.put("deviseCible", "USD");

        when(deviseService.convertir(150.50, "EUR", "USD")).thenReturn(164.5);

        // Act & Assert
        mockMvc.perform(post("/devises/convertir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant", is(150.50)))
                .andExpect(jsonPath("$.montantConverti", is(164.5)));

        verify(deviseService, times(1)).convertir(150.50, "EUR", "USD");
    }

    @Test
    @DisplayName("POST /devises/convertir - Devrait convertir la même devise (montant inchangé)")
    void convertir_DevraitConvertirMemeDevise() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("montant", 1000.0);
        request.put("deviseSource", "XOF");
        request.put("deviseCible", "XOF");

        when(deviseService.convertir(1000.0, "XOF", "XOF")).thenReturn(1000.0);

        // Act & Assert
        mockMvc.perform(post("/devises/convertir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant", is(1000.0)))
                .andExpect(jsonPath("$.deviseSource", is("XOF")))
                .andExpect(jsonPath("$.deviseCible", is("XOF")))
                .andExpect(jsonPath("$.montantConverti", is(1000.0)));

        verify(deviseService, times(1)).convertir(1000.0, "XOF", "XOF");
    }

    // ==================== Tests additionnels ====================

    @Test
    @DisplayName("GET /devises - Devrait retourner les devises avec les taux de change corrects")
    void obtenirToutesLesDevises_DevraitRetournerTauxChangeCorrects() throws Exception {
        // Arrange
        List<DeviseEntity> devises = Arrays.asList(deviseXOF, deviseEUR, deviseUSD);
        when(deviseService.obtenirToutesLesDevises()).thenReturn(devises);

        // Act & Assert
        mockMvc.perform(get("/devises")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tauxChange", is(1.0)))
                .andExpect(jsonPath("$[1].tauxChange", is(655.957)))
                .andExpect(jsonPath("$[2].tauxChange", is(600.0)));

        verify(deviseService, times(1)).obtenirToutesLesDevises();
    }

    @Test
    @DisplayName("GET /devises - Devrait avoir une seule devise par défaut")
    void obtenirToutesLesDevises_DevraitAvoirUneSeuleDeviseParDefaut() throws Exception {
        // Arrange
        List<DeviseEntity> devises = Arrays.asList(deviseXOF, deviseEUR, deviseUSD);
        when(deviseService.obtenirToutesLesDevises()).thenReturn(devises);

        // Act & Assert
        mockMvc.perform(get("/devises")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isDefault", is(true)))
                .andExpect(jsonPath("$[1].isDefault", is(false)))
                .andExpect(jsonPath("$[2].isDefault", is(false)));

        verify(deviseService, times(1)).obtenirToutesLesDevises();
    }

    @Test
    @DisplayName("POST /devises - Devrait accepter le Content-Type JSON")
    void creerDevise_DevraitAccepterContentTypeJson() throws Exception {
        // Arrange
        DeviseEntity nouvelleDevise = DeviseEntity.builder()
                .id(4L)
                .code("GBP")
                .nom("Livre sterling")
                .symbole("£")
                .pays("Royaume-Uni")
                .tauxChange(750.0)
                .isDefault(false)
                .dateCreation(LocalDateTime.now())
                .build();

        when(deviseService.creerDevise(any(CreateDeviseDto.class))).thenReturn(nouvelleDevise);

        // Act & Assert
        mockMvc.perform(post("/devises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDeviseDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(deviseService, times(1)).creerDevise(any(CreateDeviseDto.class));
    }

    @Test
    @DisplayName("GET /devises/{id} - Devrait retourner toutes les propriétés de la devise")
    void obtenirDeviseParId_DevraitRetournerToutesLesProprietes() throws Exception {
        // Arrange
        Long deviseId = 2L;
        when(deviseService.obtenirDeviseParId(deviseId)).thenReturn(deviseEUR);

        // Act & Assert
        mockMvc.perform(get("/devises/{id}", deviseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.code", notNullValue()))
                .andExpect(jsonPath("$.nom", notNullValue()))
                .andExpect(jsonPath("$.symbole", notNullValue()))
                .andExpect(jsonPath("$.pays", notNullValue()))
                .andExpect(jsonPath("$.tauxChange", notNullValue()))
                .andExpect(jsonPath("$.isDefault", notNullValue()))
                .andExpect(jsonPath("$.dateCreation", notNullValue()));

        verify(deviseService, times(1)).obtenirDeviseParId(deviseId);
    }

    @Test
    @DisplayName("POST /devises/convertir - Devrait inclure toutes les informations de conversion")
    void convertir_DevraitInclureToutesLesInformations() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("montant", 50.0);
        request.put("deviseSource", "USD");
        request.put("deviseCible", "EUR");

        when(deviseService.convertir(50.0, "USD", "EUR")).thenReturn(45.75);

        // Act & Assert
        mockMvc.perform(post("/devises/convertir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant", notNullValue()))
                .andExpect(jsonPath("$.deviseSource", notNullValue()))
                .andExpect(jsonPath("$.deviseCible", notNullValue()))
                .andExpect(jsonPath("$.montantConverti", notNullValue()))
                .andExpect(jsonPath("$.montant", is(50.0)))
                .andExpect(jsonPath("$.deviseSource", is("USD")))
                .andExpect(jsonPath("$.deviseCible", is("EUR")))
                .andExpect(jsonPath("$.montantConverti", is(45.75)));

        verify(deviseService, times(1)).convertir(50.0, "USD", "EUR");
    }
}
