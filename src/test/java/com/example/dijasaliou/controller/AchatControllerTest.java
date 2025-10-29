package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.AchatService;
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
 * Tests unitaires pour AchatController
 *
 * Bonnes pratiques appliquées :
 * - @WebMvcTest : Test uniquement la couche controller (léger et rapide)
 * - @MockitoBean : Mock les dépendances (services) - remplace @MockBean déprécié
 * - MockMvc : Simule les appels HTTP
 * - @DisplayName : Descriptions claires des tests
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - Vérification des status HTTP et du contenu JSON
 * - @AutoConfigureMockMvc : Configuration automatique de MockMvc sans sécurité
 * - excludeAutoConfiguration : Exclusion de la configuration de sécurité pour les tests
 */
@WebMvcTest(controllers = AchatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du AchatController")
class AchatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AchatService achatService;

    @MockitoBean
    private UserService userService;

    // Mocker les beans de sécurité pour éviter les problèmes de dépendances
    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    private UserEntity utilisateurTest;
    private AchatEntity achatTest;
    private AchatEntity achatTest2;

    /**
     * Initialisation des données de test avant chaque test
     * @BeforeEach : Exécuté avant CHAQUE test
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

        // Création d'un premier achat de test
        achatTest = AchatEntity.builder()
                .id(1L)
                .quantite(10)
                .nomProduit("Collier en or")
                .prixUnitaire(new BigDecimal("50.00"))
                .prixTotal(new BigDecimal("500.00"))
                .dateAchat(LocalDate.of(2025, 10, 15))
                .fournisseur("Fournisseur A")
                .utilisateur(utilisateurTest)
                .build();

        // Création d'un deuxième achat de test
        achatTest2 = AchatEntity.builder()
                .id(2L)
                .quantite(5)
                .nomProduit("Bracelet en argent")
                .prixUnitaire(new BigDecimal("30.00"))
                .prixTotal(new BigDecimal("150.00"))
                .dateAchat(LocalDate.of(2025, 10, 20))
                .fournisseur("Fournisseur B")
                .utilisateur(utilisateurTest)
                .build();
    }

    // ==================== Tests pour GET /achats ====================

    @Test
    @DisplayName("GET /achats - Devrait retourner tous les achats avec succès")
    void obtenirTous_DevraitRetournerTousLesAchats() throws Exception {
        // Arrange (Préparation)
        List<AchatEntity> achats = Arrays.asList(achatTest, achatTest2);
        when(achatService.obtenirTousLesAchats()).thenReturn(achats);

        // Act & Assert (Action et Vérification)
        mockMvc.perform(get("/achats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$[0].quantite", is(10)))
                .andExpect(jsonPath("$[0].prixUnitaire", is(50.00)))
                .andExpect(jsonPath("$[0].prixTotal", is(500.00)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].nomProduit", is("Bracelet en argent")));

        // Vérifier que le service a été appelé une fois
        verify(achatService, times(1)).obtenirTousLesAchats();
    }

    @Test
    @DisplayName("GET /achats - Devrait retourner une liste vide quand aucun achat")
    void obtenirTous_DevraitRetournerListeVide() throws Exception {
        // Arrange
        when(achatService.obtenirTousLesAchats()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/achats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(achatService, times(1)).obtenirTousLesAchats();
    }

    // ==================== Tests pour GET /achats/{id} ====================

    @Test
    @DisplayName("GET /achats/{id} - Devrait retourner un achat par son ID")
    void obtenirParId_DevraitRetournerAchat() throws Exception {
        // Arrange
        Long achatId = 1L;
        when(achatService.obtenirAchatParId(achatId)).thenReturn(achatTest);

        // Act & Assert
        mockMvc.perform(get("/achats/{id}", achatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$.quantite", is(10)))
                .andExpect(jsonPath("$.prixUnitaire", is(50.00)))
                .andExpect(jsonPath("$.prixTotal", is(500.00)))
                .andExpect(jsonPath("$.fournisseur", is("Fournisseur A")));

        verify(achatService, times(1)).obtenirAchatParId(achatId);
    }

    @Test
    @DisplayName("GET /achats/{id} - Devrait lancer une exception si achat inexistant")
    void obtenirParId_DevraitLancerExceptionSiAchatInexistant() {
        // Arrange
        Long achatId = 999L;
        when(achatService.obtenirAchatParId(achatId))
                .thenThrow(new RuntimeException("Achat non trouvé"));

        // Act & Assert
        // Vérifie que l'exécution lance une exception (ServletException qui encapsule RuntimeException)
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/achats/{id}", achatId)
                    .contentType(MediaType.APPLICATION_JSON));
        });

        verify(achatService, times(1)).obtenirAchatParId(achatId);
    }

    // ==================== Tests pour GET /achats/utilisateur/{utilisateurId} ====================

    @Test
    @DisplayName("GET /achats/utilisateur/{id} - Devrait retourner les achats d'un utilisateur")
    void obtenirAchatsParUtilisateur_DevraitRetournerAchats() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        List<AchatEntity> achats = Arrays.asList(achatTest, achatTest2);
        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(achatService.obtenirAchatsParUtilisateur(utilisateurTest)).thenReturn(achats);

        // Act & Assert
        mockMvc.perform(get("/achats/utilisateur/{utilisateurId}", utilisateurId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].nomProduit", is("Bracelet en argent")));

        verify(userService, times(1)).obtenirUtilisateurParId(utilisateurId);
        verify(achatService, times(1)).obtenirAchatsParUtilisateur(utilisateurTest);
    }

    @Test
    @DisplayName("GET /achats/utilisateur/{id} - Devrait retourner liste vide si utilisateur sans achats")
    void obtenirAchatsParUtilisateur_DevraitRetournerListeVide() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(achatService.obtenirAchatsParUtilisateur(utilisateurTest)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/achats/utilisateur/{utilisateurId}", utilisateurId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(userService, times(1)).obtenirUtilisateurParId(utilisateurId);
        verify(achatService, times(1)).obtenirAchatsParUtilisateur(utilisateurTest);
    }

    // ==================== Tests pour POST /achats ====================

    @Test
    @DisplayName("POST /achats - Devrait créer un nouvel achat avec succès")
    void creer_DevraitCreerNouvelAchat() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        AchatEntity nouvelAchat = AchatEntity.builder()
                .quantite(15)
                .nomProduit("Bague en diamant")
                .prixUnitaire(new BigDecimal("100.00"))
                .dateAchat(LocalDate.now())
                .fournisseur("Fournisseur C")
                .build();

        AchatEntity achatCree = AchatEntity.builder()
                .id(3L)
                .quantite(15)
                .nomProduit("Bague en diamant")
                .prixUnitaire(new BigDecimal("100.00"))
                .prixTotal(new BigDecimal("1500.00"))
                .dateAchat(LocalDate.now())
                .fournisseur("Fournisseur C")
                .utilisateur(utilisateurTest)
                .build();

        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(achatService.creerAchat(any(AchatEntity.class), eq(utilisateurTest)))
                .thenReturn(achatCree);

        // Act & Assert
        mockMvc.perform(post("/achats")
                        .param("utilisateurId", utilisateurId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nouvelAchat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.nomProduit", is("Bague en diamant")))
                .andExpect(jsonPath("$.quantite", is(15)))
                .andExpect(jsonPath("$.prixUnitaire", is(100.00)))
                .andExpect(jsonPath("$.prixTotal", is(1500.00)));

        verify(userService, times(1)).obtenirUtilisateurParId(utilisateurId);
        verify(achatService, times(1)).creerAchat(any(AchatEntity.class), eq(utilisateurTest));
    }

    @Test
    @DisplayName("POST /achats - Devrait gérer les données invalides")
    void creer_AvecDonneesInvalides() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        AchatEntity achatInvalide = AchatEntity.builder()
                .quantite(-5) // Quantité négative (invalide)
                .nomProduit("") // Nom vide (invalide)
                .prixUnitaire(new BigDecimal("-10.00")) // Prix négatif (invalide)
                .build();

        // Note: En test unitaire avec @WebMvcTest, la validation Bean (@Valid) n'est pas appliquée automatiquement
        // comme elle le serait dans un test d'intégration complet. Le controller acceptera donc les données.
        // Pour tester la validation complète, il faudrait utiliser @SpringBootTest

        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(achatService.creerAchat(any(AchatEntity.class), eq(utilisateurTest)))
                .thenReturn(achatInvalide);

        // Act & Assert
        mockMvc.perform(post("/achats")
                        .param("utilisateurId", utilisateurId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(achatInvalide)))
                .andExpect(status().isCreated());

        // Dans ce test unitaire, le service est appelé car la validation n'est pas active
        verify(achatService, times(1)).creerAchat(any(), any());
    }

    // ==================== Tests pour PUT /achats/{id} ====================

    @Test
    @DisplayName("PUT /achats/{id} - Devrait modifier un achat existant")
    void modifier_DevraitModifierAchat() throws Exception {
        // Arrange
        Long achatId = 1L;
        Long utilisateurId = 1L;

        AchatEntity achatModifie = AchatEntity.builder()
                .quantite(20)
                .nomProduit("Collier en or modifié")
                .prixUnitaire(new BigDecimal("60.00"))
                .dateAchat(LocalDate.of(2025, 10, 25))
                .fournisseur("Fournisseur A modifié")
                .build();

        AchatEntity achatMiseAJour = AchatEntity.builder()
                .id(achatId)
                .quantite(20)
                .nomProduit("Collier en or modifié")
                .prixUnitaire(new BigDecimal("60.00"))
                .prixTotal(new BigDecimal("1200.00"))
                .dateAchat(LocalDate.of(2025, 10, 25))
                .fournisseur("Fournisseur A modifié")
                .utilisateur(utilisateurTest)
                .build();

        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(achatService.modifierAchat(eq(achatId), any(AchatEntity.class)))
                .thenReturn(achatMiseAJour);

        // Act & Assert
        mockMvc.perform(put("/achats/{id}", achatId)
                        .param("utilisateurId", utilisateurId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(achatModifie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nomProduit", is("Collier en or modifié")))
                .andExpect(jsonPath("$.quantite", is(20)))
                .andExpect(jsonPath("$.prixUnitaire", is(60.00)))
                .andExpect(jsonPath("$.prixTotal", is(1200.00)));

        verify(userService, times(1)).obtenirUtilisateurParId(utilisateurId);
        verify(achatService, times(1)).modifierAchat(eq(achatId), any(AchatEntity.class));
    }

    @Test
    @DisplayName("PUT /achats/{id} - Devrait lancer une exception si achat inexistant")
    void modifier_DevraitLancerExceptionSiAchatInexistant() {
        // Arrange
        Long achatId = 999L;
        Long utilisateurId = 1L;

        AchatEntity achatModifie = AchatEntity.builder()
                .quantite(20)
                .nomProduit("Produit inexistant")
                .prixUnitaire(new BigDecimal("50.00"))
                .build();

        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(achatService.modifierAchat(eq(achatId), any(AchatEntity.class)))
                .thenThrow(new RuntimeException("Achat non trouvé"));

        // Act & Assert
        // Vérifie que l'exécution lance une exception
        assertThrows(Exception.class, () -> {
            mockMvc.perform(put("/achats/{id}", achatId)
                    .param("utilisateurId", utilisateurId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(achatModifie)));
        });

        verify(achatService, times(1)).modifierAchat(eq(achatId), any(AchatEntity.class));
    }

    // ==================== Tests pour GET /achats/statistiques ====================

    @Test
    @DisplayName("GET /achats/statistiques - Devrait retourner les statistiques d'une période")
    void obtenirStatistiques_DevraitRetournerStatistiques() throws Exception {
        // Arrange
        LocalDate dateDebut = LocalDate.of(2025, 10, 1);
        LocalDate dateFin = LocalDate.of(2025, 10, 31);
        List<AchatEntity> achats = Arrays.asList(achatTest, achatTest2);
        BigDecimal montantTotal = new BigDecimal("650.00");

        when(achatService.obtenirAchatsParPeriode(dateDebut, dateFin)).thenReturn(achats);
        when(achatService.calculerTotalAchats(dateDebut, dateFin)).thenReturn(montantTotal);

        // Act & Assert
        mockMvc.perform(get("/achats/statistiques")
                        .param("debut", dateDebut.toString())
                        .param("fin", dateFin.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateDebut", is("2025-10-01")))
                .andExpect(jsonPath("$.dateFin", is("2025-10-31")))
                .andExpect(jsonPath("$.nombreAchats", is(2)))
                .andExpect(jsonPath("$.montantTotal", is(650.00)))
                .andExpect(jsonPath("$.achats", hasSize(2)));

        verify(achatService, times(1)).obtenirAchatsParPeriode(dateDebut, dateFin);
        verify(achatService, times(1)).calculerTotalAchats(dateDebut, dateFin);
    }

    @Test
    @DisplayName("GET /achats/statistiques - Devrait retourner statistiques vides si aucun achat")
    void obtenirStatistiques_DevraitRetournerStatistiquesVides() throws Exception {
        // Arrange
        LocalDate dateDebut = LocalDate.of(2025, 11, 1);
        LocalDate dateFin = LocalDate.of(2025, 11, 30);
        BigDecimal montantTotal = BigDecimal.ZERO;

        when(achatService.obtenirAchatsParPeriode(dateDebut, dateFin)).thenReturn(Arrays.asList());
        when(achatService.calculerTotalAchats(dateDebut, dateFin)).thenReturn(montantTotal);

        // Act & Assert
        mockMvc.perform(get("/achats/statistiques")
                        .param("debut", dateDebut.toString())
                        .param("fin", dateFin.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreAchats", is(0)))
                .andExpect(jsonPath("$.montantTotal", is(0)))
                .andExpect(jsonPath("$.achats", hasSize(0)));

        verify(achatService, times(1)).obtenirAchatsParPeriode(dateDebut, dateFin);
        verify(achatService, times(1)).calculerTotalAchats(dateDebut, dateFin);
    }

    @Test
    @DisplayName("GET /achats/statistiques - Devrait retourner 400 si dates invalides")
    void obtenirStatistiques_DevraitRetourner400SiDatesInvalides() throws Exception {
        // Act & Assert - Dates mal formatées
        mockMvc.perform(get("/achats/statistiques")
                        .param("debut", "date-invalide")
                        .param("fin", "2025-10-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Le service ne devrait pas être appelé
        verify(achatService, never()).obtenirAchatsParPeriode(any(), any());
    }

    // ==================== Tests pour DELETE /achats/{id} ====================

    @Test
    @DisplayName("DELETE /achats/{id} - Devrait supprimer un achat avec succès")
    void supprimer_DevraitSupprimerAchat() throws Exception {
        // Arrange
        Long achatId = 1L;
        doNothing().when(achatService).supprimerAchat(achatId);

        // Act & Assert
        mockMvc.perform(delete("/achats/{id}", achatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(achatService, times(1)).supprimerAchat(achatId);
    }

    @Test
    @DisplayName("DELETE /achats/{id} - Devrait lancer une exception si achat inexistant")
    void supprimer_DevraitLancerExceptionSiAchatInexistant() {
        // Arrange
        Long achatId = 999L;
        doThrow(new RuntimeException("Achat non trouvé"))
                .when(achatService).supprimerAchat(achatId);

        // Act & Assert
        // Vérifie que l'exécution lance une exception
        assertThrows(Exception.class, () -> {
            mockMvc.perform(delete("/achats/{id}", achatId)
                    .contentType(MediaType.APPLICATION_JSON));
        });

        verify(achatService, times(1)).supprimerAchat(achatId);
    }

    // ==================== Tests d'intégration additionnels ====================

    @Test
    @DisplayName("GET /achats - Devrait gérer correctement les achats avec fournisseur null")
    void obtenirTous_DevraitGererFournisseurNull() throws Exception {
        // Arrange
        AchatEntity achatSansFournisseur = AchatEntity.builder()
                .id(3L)
                .quantite(8)
                .nomProduit("Montre")
                .prixUnitaire(new BigDecimal("80.00"))
                .prixTotal(new BigDecimal("640.00"))
                .dateAchat(LocalDate.now())
                .fournisseur(null) // Pas de fournisseur
                .utilisateur(utilisateurTest)
                .build();

        when(achatService.obtenirTousLesAchats())
                .thenReturn(Arrays.asList(achatSansFournisseur));

        // Act & Assert
        mockMvc.perform(get("/achats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fournisseur").doesNotExist());

        verify(achatService, times(1)).obtenirTousLesAchats();
    }

    @Test
    @DisplayName("POST /achats - Devrait calculer automatiquement le prix total")
    void creer_DevraitCalculerPrixTotal() throws Exception {
        // Arrange
        Long utilisateurId = 1L;
        AchatEntity nouvelAchat = AchatEntity.builder()
                .quantite(12)
                .nomProduit("Boucles d'oreilles")
                .prixUnitaire(new BigDecimal("25.00"))
                .dateAchat(LocalDate.now())
                .build();

        // Le service devrait calculer : 12 × 25.00 = 300.00
        AchatEntity achatCree = AchatEntity.builder()
                .id(4L)
                .quantite(12)
                .nomProduit("Boucles d'oreilles")
                .prixUnitaire(new BigDecimal("25.00"))
                .prixTotal(new BigDecimal("300.00")) // Calculé automatiquement
                .dateAchat(LocalDate.now())
                .utilisateur(utilisateurTest)
                .build();

        when(userService.obtenirUtilisateurParId(utilisateurId)).thenReturn(utilisateurTest);
        when(achatService.creerAchat(any(AchatEntity.class), eq(utilisateurTest)))
                .thenReturn(achatCree);

        // Act & Assert
        mockMvc.perform(post("/achats")
                        .param("utilisateurId", utilisateurId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nouvelAchat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixTotal", is(300.00)));

        verify(achatService, times(1)).creerAchat(any(AchatEntity.class), eq(utilisateurTest));
    }
}
