package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.DepenseEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.DepenseService;
import com.example.dijasaliou.service.UserService;
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
 * Tests unitaires pour DepenseController
 *
 * Bonnes pratiques appliquées :
 * - @WebMvcTest : Test uniquement la couche controller (léger et rapide)
 * - @MockitoBean : Mock les dépendances (services)
 * - MockMvc : Simule les appels HTTP
 * - @DisplayName : Descriptions claires des tests
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - Vérification des status HTTP et du contenu JSON
 */
@WebMvcTest(controllers = DepenseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du DepenseController")
class DepenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DepenseService depenseService;

    @MockitoBean
    private UserService userService;

    // Mocker les beans de sécurité pour éviter les problèmes de dépendances
    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    private UserEntity utilisateurTest;
    private DepenseEntity depenseTest;
    private DepenseEntity depenseTest2;

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

        // Création d'une première dépense de test
        depenseTest = DepenseEntity.builder()
                .id(1L)
                .libelle("Loyer du magasin")
                .montant(new BigDecimal("500000.00"))
                .dateDepense(LocalDate.of(2025, 10, 15))
                .categorie(DepenseEntity.CategorieDepense.LOYER)
                .notes("Loyer mensuel")
                .estRecurrente(true)
                .utilisateur(utilisateurTest)
                .build();

        // Création d'une deuxième dépense de test
        depenseTest2 = DepenseEntity.builder()
                .id(2L)
                .libelle("Facture électricité")
                .montant(new BigDecimal("75000.00"))
                .dateDepense(LocalDate.of(2025, 10, 20))
                .categorie(DepenseEntity.CategorieDepense.ELECTRICITE)
                .notes("Facture mensuelle")
                .estRecurrente(true)
                .utilisateur(utilisateurTest)
                .build();
    }

    // ==================== Tests pour GET /depenses ====================

    @Test
    @DisplayName("GET /depenses - Devrait retourner toutes les dépenses avec succès")
    void obtenirTous_DevraitRetournerToutesLesDepenses() throws Exception {
        // Arrange
        List<DepenseEntity> depenses = Arrays.asList(depenseTest, depenseTest2);
        when(depenseService.obtenirToutesLesDepenses()).thenReturn(depenses);

        // Act & Assert
        mockMvc.perform(get("/depenses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].libelle", is("Loyer du magasin")))
                .andExpect(jsonPath("$[0].montant", is(500000.00)))
                .andExpect(jsonPath("$[0].categorie", is("LOYER")))
                .andExpect(jsonPath("$[0].estRecurrente", is(true)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].libelle", is("Facture électricité")));

        verify(depenseService, times(1)).obtenirToutesLesDepenses();
    }

    @Test
    @DisplayName("GET /depenses - Devrait retourner une liste vide quand aucune dépense")
    void obtenirTous_DevraitRetournerListeVide() throws Exception {
        // Arrange
        when(depenseService.obtenirToutesLesDepenses()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/depenses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(depenseService, times(1)).obtenirToutesLesDepenses();
    }

    // ==================== Tests pour GET /depenses/{id} ====================

    @Test
    @DisplayName("GET /depenses/{id} - Devrait retourner une dépense par son ID")
    void obtenirParId_DevraitRetournerDepense() throws Exception {
        // Arrange
        Long depenseId = 1L;
        when(depenseService.obtenirDepenseParId(depenseId)).thenReturn(depenseTest);

        // Act & Assert
        mockMvc.perform(get("/depenses/{id}", depenseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.libelle", is("Loyer du magasin")))
                .andExpect(jsonPath("$.montant", is(500000.00)))
                .andExpect(jsonPath("$.categorie", is("LOYER")))
                .andExpect(jsonPath("$.notes", is("Loyer mensuel")));

        verify(depenseService, times(1)).obtenirDepenseParId(depenseId);
    }

    @Test
    @DisplayName("GET /depenses/{id} - Devrait lancer une exception si dépense inexistante")
    void obtenirParId_DevraitLancerExceptionSiDepenseInexistante() {
        // Arrange
        Long depenseId = 999L;
        when(depenseService.obtenirDepenseParId(depenseId))
                .thenThrow(new RuntimeException("Dépense non trouvée"));

        // Act & Assert
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/depenses/{id}", depenseId)
                    .contentType(MediaType.APPLICATION_JSON));
        });

        verify(depenseService, times(1)).obtenirDepenseParId(depenseId);
    }

    // ==================== Tests pour GET /depenses/utilisateur/{utilisateurId} ====================

    @Test
    @DisplayName("GET /depenses/utilisateur/{id} - Devrait retourner les dépenses d'un utilisateur")
    void obtenirDepensesParUtilisateur_DevraitRetournerDepenses() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        List<DepenseEntity> depenses = Arrays.asList(depenseTest, depenseTest2);
        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(depenseService.obtenirDepensesParUtilisateur(utilisateurTest)).thenReturn(depenses);

        // Act & Assert
        mockMvc.perform(get("/depenses/utilisateur/{utilisateurId}", utilisateurId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].libelle", is("Loyer du magasin")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].libelle", is("Facture électricité")));

        verify(userService, times(1)).obtenirUtilisateurParId(utilisateurId);
        verify(depenseService, times(1)).obtenirDepensesParUtilisateur(utilisateurTest);
    }

    @Test
    @DisplayName("GET /depenses/utilisateur/{id} - Devrait retourner liste vide si utilisateur sans dépenses")
    void obtenirDepensesParUtilisateur_DevraitRetournerListeVide() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(depenseService.obtenirDepensesParUtilisateur(utilisateurTest)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/depenses/utilisateur/{utilisateurId}", utilisateurId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(userService, times(1)).obtenirUtilisateurParId(utilisateurId);
        verify(depenseService, times(1)).obtenirDepensesParUtilisateur(utilisateurTest);
    }

    // ==================== Tests pour POST /depenses ====================

    @Test
    @DisplayName("POST /depenses - Devrait créer une nouvelle dépense avec succès")
    void creer_DevraitCreerNouvelleDepense() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        DepenseEntity nouvelleDepense = DepenseEntity.builder()
                .libelle("Assurance locale")
                .montant(new BigDecimal("120000.00"))
                .dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.ASSURANCE)
                .notes("Assurance annuelle")
                .estRecurrente(false)
                .build();

        DepenseEntity depenseCreee = DepenseEntity.builder()
                .id(3L)
                .libelle("Assurance locale")
                .montant(new BigDecimal("120000.00"))
                .dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.ASSURANCE)
                .notes("Assurance annuelle")
                .estRecurrente(false)
                .utilisateur(utilisateurTest)
                .build();

        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(depenseService.creerDepense(any(DepenseEntity.class), eq(utilisateurTest)))
                .thenReturn(depenseCreee);

        // Act & Assert
        mockMvc.perform(post("/depenses")
                        .param("utilisateurId", utilisateurId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nouvelleDepense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.libelle", is("Assurance locale")))
                .andExpect(jsonPath("$.montant", is(120000.00)))
                .andExpect(jsonPath("$.categorie", is("ASSURANCE")));

        verify(userService, times(1)).obtenirUtilisateurParId(utilisateurId);
        verify(depenseService, times(1)).creerDepense(any(DepenseEntity.class), eq(utilisateurTest));
    }

    // ==================== Tests pour PUT /depenses/{id} ====================

    @Test
    @DisplayName("PUT /depenses/{id} - Devrait modifier une dépense existante")
    void modifier_DevraitModifierDepense() throws Exception {
        // Arrange
        Long depenseId = 1L;
        Long utilisateurId = 1L;

        DepenseEntity depenseModifiee = DepenseEntity.builder()
                .libelle("Loyer du magasin - Mise à jour")
                .montant(new BigDecimal("550000.00"))
                .dateDepense(LocalDate.of(2025, 11, 15))
                .categorie(DepenseEntity.CategorieDepense.LOYER)
                .notes("Loyer mensuel augmenté")
                .estRecurrente(true)
                .build();

        DepenseEntity depenseMiseAJour = DepenseEntity.builder()
                .id(depenseId)
                .libelle("Loyer du magasin - Mise à jour")
                .montant(new BigDecimal("550000.00"))
                .dateDepense(LocalDate.of(2025, 11, 15))
                .categorie(DepenseEntity.CategorieDepense.LOYER)
                .notes("Loyer mensuel augmenté")
                .estRecurrente(true)
                .utilisateur(utilisateurTest)
                .build();

        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(depenseService.modifierDepense(eq(depenseId), any(DepenseEntity.class)))
                .thenReturn(depenseMiseAJour);

        // Act & Assert
        mockMvc.perform(put("/depenses/{id}", depenseId)
                        .param("utilisateurId", utilisateurId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depenseModifiee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.libelle", is("Loyer du magasin - Mise à jour")))
                .andExpect(jsonPath("$.montant", is(550000.00)));

        verify(userService, times(1)).obtenirUtilisateurParId(utilisateurId);
        verify(depenseService, times(1)).modifierDepense(eq(depenseId), any(DepenseEntity.class));
    }

    @Test
    @DisplayName("PUT /depenses/{id} - Devrait lancer une exception si dépense inexistante")
    void modifier_DevraitLancerExceptionSiDepenseInexistante() {
        // Arrange
        Long depenseId = 999L;
        Long utilisateurId = 1L;

        DepenseEntity depenseModifiee = DepenseEntity.builder()
                .libelle("Dépense inexistante")
                .montant(new BigDecimal("100000.00"))
                .dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.AUTRE)
                .build();

        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(depenseService.modifierDepense(eq(depenseId), any(DepenseEntity.class)))
                .thenThrow(new RuntimeException("Dépense non trouvée"));

        // Act & Assert
        assertThrows(Exception.class, () -> {
            mockMvc.perform(put("/depenses/{id}", depenseId)
                    .param("utilisateurId", utilisateurId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(depenseModifiee)));
        });

        verify(depenseService, times(1)).modifierDepense(eq(depenseId), any(DepenseEntity.class));
    }

    // ==================== Tests pour DELETE /depenses/{id} ====================

    @Test
    @DisplayName("DELETE /depenses/{id} - Devrait supprimer une dépense avec succès")
    void supprimer_DevraitSupprimerDepense() throws Exception {
        // Arrange
        Long depenseId = 1L;
        doNothing().when(depenseService).supprimerDepense(depenseId);

        // Act & Assert
        mockMvc.perform(delete("/depenses/{id}", depenseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(depenseService, times(1)).supprimerDepense(depenseId);
    }

    @Test
    @DisplayName("DELETE /depenses/{id} - Devrait lancer une exception si dépense inexistante")
    void supprimer_DevraitLancerExceptionSiDepenseInexistante() {
        // Arrange
        Long depenseId = 999L;
        doThrow(new RuntimeException("Dépense non trouvée"))
                .when(depenseService).supprimerDepense(depenseId);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            mockMvc.perform(delete("/depenses/{id}", depenseId)
                    .contentType(MediaType.APPLICATION_JSON));
        });

        verify(depenseService, times(1)).supprimerDepense(depenseId);
    }

    // ==================== Tests pour GET /depenses/categorie/{categorie} ====================

    @Test
    @DisplayName("GET /depenses/categorie/{categorie} - Devrait retourner les dépenses par catégorie")
    void obtenirDepensesParCategorie_DevraitRetournerDepenses() throws Exception {
        // Arrange
        DepenseEntity.CategorieDepense categorie = DepenseEntity.CategorieDepense.LOYER;
        List<DepenseEntity> depenses = Arrays.asList(depenseTest);
        when(depenseService.obtenirDepensesParCategorie(categorie)).thenReturn(depenses);

        // Act & Assert
        mockMvc.perform(get("/depenses/categorie/{categorie}", categorie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].libelle", is("Loyer du magasin")))
                .andExpect(jsonPath("$[0].categorie", is("LOYER")));

        verify(depenseService, times(1)).obtenirDepensesParCategorie(categorie);
    }

    @Test
    @DisplayName("GET /depenses/categorie/{categorie} - Devrait retourner liste vide si aucune dépense")
    void obtenirDepensesParCategorie_DevraitRetournerListeVide() throws Exception {
        // Arrange
        DepenseEntity.CategorieDepense categorie = DepenseEntity.CategorieDepense.MARKETING;
        when(depenseService.obtenirDepensesParCategorie(categorie)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/depenses/categorie/{categorie}", categorie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(depenseService, times(1)).obtenirDepensesParCategorie(categorie);
    }

    // ==================== Tests pour GET /depenses/total ====================

    @Test
    @DisplayName("GET /depenses/total - Devrait retourner le total des dépenses pour une période")
    void calculerTotalDepenses_DevraitRetournerTotal() throws Exception {
        // Arrange
        LocalDate dateDebut = LocalDate.of(2025, 10, 1);
        LocalDate dateFin = LocalDate.of(2025, 10, 31);
        BigDecimal montantTotal = new BigDecimal("575000.00");

        when(depenseService.calculerTotalDepenses(dateDebut, dateFin)).thenReturn(montantTotal);

        // Act & Assert
        mockMvc.perform(get("/depenses/total")
                        .param("debut", dateDebut.toString())
                        .param("fin", dateFin.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(575000.00)));

        verify(depenseService, times(1)).calculerTotalDepenses(dateDebut, dateFin);
    }

    @Test
    @DisplayName("GET /depenses/total - Devrait retourner zéro si aucune dépense")
    void calculerTotalDepenses_DevraitRetournerZero() throws Exception {
        // Arrange
        LocalDate dateDebut = LocalDate.of(2025, 11, 1);
        LocalDate dateFin = LocalDate.of(2025, 11, 30);
        BigDecimal montantTotal = BigDecimal.ZERO;

        when(depenseService.calculerTotalDepenses(dateDebut, dateFin)).thenReturn(montantTotal);

        // Act & Assert
        mockMvc.perform(get("/depenses/total")
                        .param("debut", dateDebut.toString())
                        .param("fin", dateFin.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(0)));

        verify(depenseService, times(1)).calculerTotalDepenses(dateDebut, dateFin);
    }

    // ==================== Tests pour GET /depenses/statistiques ====================

    @Test
    @DisplayName("GET /depenses/statistiques - Devrait retourner les statistiques d'une période")
    void obtenirStatistiques_DevraitRetournerStatistiques() throws Exception {
        // Arrange
        LocalDate dateDebut = LocalDate.of(2025, 10, 1);
        LocalDate dateFin = LocalDate.of(2025, 10, 31);
        List<DepenseEntity> depenses = Arrays.asList(depenseTest, depenseTest2);
        BigDecimal montantTotal = new BigDecimal("575000.00");

        when(depenseService.obtenirDepensesParPeriode(dateDebut, dateFin)).thenReturn(depenses);
        when(depenseService.calculerTotalDepenses(dateDebut, dateFin)).thenReturn(montantTotal);

        // Act & Assert
        mockMvc.perform(get("/depenses/statistiques")
                        .param("debut", dateDebut.toString())
                        .param("fin", dateFin.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateDebut", is("2025-10-01")))
                .andExpect(jsonPath("$.dateFin", is("2025-10-31")))
                .andExpect(jsonPath("$.nombreDepenses", is(2)))
                .andExpect(jsonPath("$.montantTotal", is(575000.00)))
                .andExpect(jsonPath("$.depenses", hasSize(2)));

        verify(depenseService, times(1)).obtenirDepensesParPeriode(dateDebut, dateFin);
        verify(depenseService, times(1)).calculerTotalDepenses(dateDebut, dateFin);
    }

    @Test
    @DisplayName("GET /depenses/statistiques - Devrait retourner statistiques vides si aucune dépense")
    void obtenirStatistiques_DevraitRetournerStatistiquesVides() throws Exception {
        // Arrange
        LocalDate dateDebut = LocalDate.of(2025, 11, 1);
        LocalDate dateFin = LocalDate.of(2025, 11, 30);
        BigDecimal montantTotal = BigDecimal.ZERO;

        when(depenseService.obtenirDepensesParPeriode(dateDebut, dateFin)).thenReturn(Arrays.asList());
        when(depenseService.calculerTotalDepenses(dateDebut, dateFin)).thenReturn(montantTotal);

        // Act & Assert
        mockMvc.perform(get("/depenses/statistiques")
                        .param("debut", dateDebut.toString())
                        .param("fin", dateFin.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDepenses", is(0)))
                .andExpect(jsonPath("$.montantTotal", is(0)))
                .andExpect(jsonPath("$.depenses", hasSize(0)));

        verify(depenseService, times(1)).obtenirDepensesParPeriode(dateDebut, dateFin);
        verify(depenseService, times(1)).calculerTotalDepenses(dateDebut, dateFin);
    }

    @Test
    @DisplayName("GET /depenses/statistiques - Devrait retourner 400 si dates invalides")
    void obtenirStatistiques_DevraitRetourner400SiDatesInvalides() throws Exception {
        // Act & Assert - Dates mal formatées
        mockMvc.perform(get("/depenses/statistiques")
                        .param("debut", "date-invalide")
                        .param("fin", "2025-10-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Le service ne devrait pas être appelé
        verify(depenseService, never()).obtenirDepensesParPeriode(any(), any());
    }

    // ==================== Tests additionnels ====================

    @Test
    @DisplayName("GET /depenses - Devrait gérer correctement les dépenses avec notes null")
    void obtenirTous_DevraitGererNotesNull() throws Exception {
        // Arrange
        DepenseEntity depenseSansNotes = DepenseEntity.builder()
                .id(3L)
                .libelle("Transport")
                .montant(new BigDecimal("25000.00"))
                .dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.TRANSPORT)
                .notes(null) // Pas de notes
                .estRecurrente(false)
                .utilisateur(utilisateurTest)
                .build();

        when(depenseService.obtenirToutesLesDepenses())
                .thenReturn(Arrays.asList(depenseSansNotes));

        // Act & Assert
        mockMvc.perform(get("/depenses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].notes").doesNotExist());

        verify(depenseService, times(1)).obtenirToutesLesDepenses();
    }

    @Test
    @DisplayName("POST /depenses - Devrait créer une dépense non récurrente par défaut")
    void creer_DevraitCreerDepenseNonRecurrente() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        DepenseEntity nouvelleDepense = DepenseEntity.builder()
                .libelle("Achat ponctuel")
                .montant(new BigDecimal("50000.00"))
                .dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.FOURNITURES)
                .estRecurrente(false)
                .build();

        DepenseEntity depenseCreee = DepenseEntity.builder()
                .id(4L)
                .libelle("Achat ponctuel")
                .montant(new BigDecimal("50000.00"))
                .dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.FOURNITURES)
                .estRecurrente(false)
                .utilisateur(utilisateurTest)
                .build();

        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(depenseService.creerDepense(any(DepenseEntity.class), eq(utilisateurTest)))
                .thenReturn(depenseCreee);

        // Act & Assert
        mockMvc.perform(post("/depenses")
                        .param("utilisateurId", utilisateurId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nouvelleDepense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estRecurrente", is(false)));

        verify(depenseService, times(1)).creerDepense(any(DepenseEntity.class), eq(utilisateurTest));
    }
}
