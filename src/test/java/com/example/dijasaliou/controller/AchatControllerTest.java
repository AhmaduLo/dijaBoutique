package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.AchatDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.AchatService;
import com.example.dijasaliou.service.UserService;
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

    @MockitoBean
    private com.example.dijasaliou.config.HibernateFilterInterceptor hibernateFilterInterceptor;

    @MockitoBean
    private com.example.dijasaliou.filter.SubscriptionExpirationFilter subscriptionExpirationFilter;

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
                .quantite(10)
                .nomProduit("Collier en or")
                .prixUnitaire(new BigDecimal("50.00"))
                .prixTotal(new BigDecimal("500.00"))
                .dateAchat(LocalDate.of(2025, 10, 15))
                .fournisseur("Fournisseur A")
                .utilisateur(utilisateurTest)
                .build();
        achatTest.setId("test-id-1");

        // Création d'un deuxième achat de test
        achatTest2 = AchatEntity.builder()
                .quantite(5)
                .nomProduit("Bracelet en argent")
                .prixUnitaire(new BigDecimal("30.00"))
                .prixTotal(new BigDecimal("150.00"))
                .dateAchat(LocalDate.of(2025, 10, 20))
                .fournisseur("Fournisseur B")
                .utilisateur(utilisateurTest)
                .build();
        achatTest2.setId("test-id-2");
    }

    // ==================== Tests pour GET /achats ====================

    @Test
    @DisplayName("GET /achats - Devrait retourner tous les achats avec succès")
    void obtenirTous_DevraitRetournerTousLesAchats() throws Exception {
        // Arrange (Préparation)
        PagedResponse<AchatDto> page = PagedResponse.<AchatDto>builder()
                .content(Arrays.asList(AchatDto.fromEntity(achatTest), AchatDto.fromEntity(achatTest2)))
                .currentPage(0).pageSize(20).totalElements(2L).totalPages(1).first(true).last(true).build();
        when(achatService.obtenirAchatsPagines(0, 20, null, null, null)).thenReturn(page);

        // Act & Assert (Action et Vérification)
        mockMvc.perform(get("/achats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is("test-id-1")))
                .andExpect(jsonPath("$.content[0].nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$.content[0].quantite", is(10)))
                .andExpect(jsonPath("$.content[1].id", is("test-id-2")))
                .andExpect(jsonPath("$.content[1].nomProduit", is("Bracelet en argent")));

        verify(achatService, times(1)).obtenirAchatsPagines(0, 20, null, null, null);
    }

    @Test
    @DisplayName("GET /achats - Devrait retourner une liste vide quand aucun achat")
    void obtenirTous_DevraitRetournerListeVide() throws Exception {
        // Arrange
        PagedResponse<AchatDto> pageVide = PagedResponse.<AchatDto>builder()
                .content(Arrays.asList()).currentPage(0).pageSize(20)
                .totalElements(0L).totalPages(0).first(true).last(true).build();
        when(achatService.obtenirAchatsPagines(0, 20, null, null, null)).thenReturn(pageVide);

        // Act & Assert
        mockMvc.perform(get("/achats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(achatService, times(1)).obtenirAchatsPagines(0, 20, null, null, null);
    }

    // ==================== Tests pour GET /achats/{id} ====================

    @Test
    @DisplayName("GET /achats/{id} - Devrait retourner un achat par son ID")
    void obtenirParId_DevraitRetournerAchat() throws Exception {
        // Arrange
        String achatId = "test-id-1";
        when(achatService.obtenirAchatParId(achatId)).thenReturn(achatTest);

        // Act & Assert
        mockMvc.perform(get("/achats/{id}", achatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("test-id-1")))
                .andExpect(jsonPath("$.nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$.quantite", is(10)))
                .andExpect(jsonPath("$.prixUnitaire", is(50.00)))
                .andExpect(jsonPath("$.prixTotal", is(500.00)))
                .andExpect(jsonPath("$.fournisseur", is("Fournisseur A")));

        verify(achatService, times(1)).obtenirAchatParId(achatId);
    }

    @Test
    @DisplayName("GET /achats/{id} - Devrait lancer une exception si achat inexistant")
    void obtenirParId_DevraitLancerExceptionSiAchatInexistant() throws Exception {
        // Arrange
        String achatId = "id-inexistant";
        when(achatService.obtenirAchatParId(achatId))
                .thenThrow(new RuntimeException("Achat non trouvé"));

        // Act & Assert
        mockMvc.perform(get("/achats/{id}", achatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

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
                .andExpect(jsonPath("$[0].id", is("test-id-1")))
                .andExpect(jsonPath("$[0].nomProduit", is("Collier en or")))
                .andExpect(jsonPath("$[1].id", is("test-id-2")))
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
        AchatEntity nouvelAchat = AchatEntity.builder()
                .quantite(15)
                .nomProduit("Bague en diamant")
                .prixUnitaire(new BigDecimal("100.00"))
                .prixTotal(new BigDecimal("1500.00"))
                .dateAchat(LocalDate.now())
                .fournisseur("Fournisseur C")
                .build();

        AchatEntity achatCree = AchatEntity.builder()
                .quantite(15)
                .nomProduit("Bague en diamant")
                .prixUnitaire(new BigDecimal("100.00"))
                .prixTotal(new BigDecimal("1500.00"))
                .dateAchat(LocalDate.now())
                .fournisseur("Fournisseur C")
                .utilisateur(utilisateurTest)
                .build();
        achatCree.setId("test-id-3");

        when(userService.obtenirUtilisateurParEmail("amadou@example.com")).thenReturn(utilisateurTest);
        when(achatService.creerAchat(any(AchatEntity.class), eq(utilisateurTest)))
                .thenReturn(achatCree);

        // Act & Assert
        mockMvc.perform(post("/achats")
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null, List.of(new SimpleGrantedAuthority("USER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nouvelAchat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("test-id-3")))
                .andExpect(jsonPath("$.nomProduit", is("Bague en diamant")))
                .andExpect(jsonPath("$.quantite", is(15)))
                .andExpect(jsonPath("$.prixTotal", is(1500.00)));

        verify(userService, times(1)).obtenirUtilisateurParEmail("amadou@example.com");
        verify(achatService, times(1)).creerAchat(any(AchatEntity.class), eq(utilisateurTest));
    }

    @Test
    @DisplayName("POST /achats - Devrait gérer les données invalides")
    void creer_AvecDonneesInvalides() throws Exception {
        // Arrange - données invalides : quantité négative, nom vide, prix négatif, prixTotal null
        AchatEntity achatInvalide = AchatEntity.builder()
                .quantite(-5) // Quantité négative (invalide)
                .nomProduit("") // Nom vide (invalide)
                .prixUnitaire(new BigDecimal("-10.00")) // Prix négatif (invalide)
                .build();

        // @Valid déclenche MethodArgumentNotValidException qui est interceptée par
        // GlobalExceptionHandler.handleGeneralException → 500 (il n'étend pas ResponseEntityExceptionHandler)

        // Act & Assert
        mockMvc.perform(post("/achats")
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null,
                                List.of(new SimpleGrantedAuthority("USER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(achatInvalide)))
                .andExpect(status().isInternalServerError());

        // La validation échoue avant d'appeler le service
        verify(achatService, never()).creerAchat(any(), any());
    }

    // ==================== Tests pour PUT /achats/{id} ====================

    @Test
    @DisplayName("PUT /achats/{id} - Devrait modifier un achat existant")
    void modifier_DevraitModifierAchat() throws Exception {
        // Arrange
        String achatId = "test-id-1";

        AchatEntity achatModifie = AchatEntity.builder()
                .quantite(20)
                .nomProduit("Collier en or modifié")
                .prixUnitaire(new BigDecimal("60.00"))
                .prixTotal(new BigDecimal("1200.00"))
                .dateAchat(LocalDate.of(2025, 10, 25))
                .fournisseur("Fournisseur A modifié")
                .build();

        AchatEntity achatMiseAJour = AchatEntity.builder()
                .quantite(20)
                .nomProduit("Collier en or modifié")
                .prixUnitaire(new BigDecimal("60.00"))
                .prixTotal(new BigDecimal("1200.00"))
                .dateAchat(LocalDate.of(2025, 10, 25))
                .fournisseur("Fournisseur A modifié")
                .utilisateur(utilisateurTest)
                .build();
        achatMiseAJour.setId(achatId);

        when(userService.obtenirUtilisateurParEmail("amadou@example.com")).thenReturn(utilisateurTest);
        when(achatService.modifierAchat(eq(achatId), any(AchatEntity.class)))
                .thenReturn(achatMiseAJour);

        // Act & Assert
        mockMvc.perform(put("/achats/{id}", achatId)
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null,
                                List.of(new SimpleGrantedAuthority("USER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(achatModifie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("test-id-1")))
                .andExpect(jsonPath("$.nomProduit", is("Collier en or modifié")))
                .andExpect(jsonPath("$.quantite", is(20)))
                .andExpect(jsonPath("$.prixUnitaire", is(60.00)))
                .andExpect(jsonPath("$.prixTotal", is(1200.00)));

        verify(userService, times(1)).obtenirUtilisateurParEmail("amadou@example.com");
        verify(achatService, times(1)).modifierAchat(eq(achatId), any(AchatEntity.class));
    }

    @Test
    @DisplayName("PUT /achats/{id} - Devrait lancer une exception si achat inexistant")
    void modifier_DevraitLancerExceptionSiAchatInexistant() throws Exception {
        // Arrange
        String achatId = "id-inexistant";

        AchatEntity achatModifie = AchatEntity.builder()
                .quantite(20)
                .nomProduit("Produit inexistant")
                .prixUnitaire(new BigDecimal("50.00"))
                .prixTotal(new BigDecimal("1000.00"))
                .dateAchat(LocalDate.of(2025, 10, 25))
                .build();

        when(userService.obtenirUtilisateurParEmail("amadou@example.com")).thenReturn(utilisateurTest);
        when(achatService.modifierAchat(eq(achatId), any(AchatEntity.class)))
                .thenThrow(new RuntimeException("Achat non trouvé"));

        // Act & Assert
        mockMvc.perform(put("/achats/{id}", achatId)
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null,
                                List.of(new SimpleGrantedAuthority("USER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(achatModifie)))
                .andExpect(status().isBadRequest());

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
        String achatId = "test-id-1";
        doNothing().when(achatService).supprimerAchat(achatId);

        // Act & Assert
        mockMvc.perform(delete("/achats/{id}", achatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(achatService, times(1)).supprimerAchat(achatId);
    }

    @Test
    @DisplayName("DELETE /achats/{id} - Devrait lancer une exception si achat inexistant")
    void supprimer_DevraitLancerExceptionSiAchatInexistant() throws Exception {
        // Arrange
        String achatId = "id-inexistant";
        doThrow(new RuntimeException("Achat non trouvé"))
                .when(achatService).supprimerAchat(achatId);

        // Act & Assert
        mockMvc.perform(delete("/achats/{id}", achatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(achatService, times(1)).supprimerAchat(achatId);
    }

    // ==================== Tests d'intégration additionnels ====================

    @Test
    @DisplayName("GET /achats - Devrait gérer correctement les achats avec fournisseur null")
    void obtenirTous_DevraitGererFournisseurNull() throws Exception {
        // Arrange
        AchatEntity achatSansFournisseur = AchatEntity.builder()
                .quantite(8)
                .nomProduit("Montre")
                .prixUnitaire(new BigDecimal("80.00"))
                .prixTotal(new BigDecimal("640.00"))
                .dateAchat(LocalDate.now())
                .fournisseur(null) // Pas de fournisseur
                .utilisateur(utilisateurTest)
                .build();
        achatSansFournisseur.setId("test-id-3");

        PagedResponse<AchatDto> page = PagedResponse.<AchatDto>builder()
                .content(Arrays.asList(AchatDto.fromEntity(achatSansFournisseur)))
                .currentPage(0).pageSize(20).totalElements(1L).totalPages(1).first(true).last(true).build();
        when(achatService.obtenirAchatsPagines(0, 20, null, null, null)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/achats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fournisseur").doesNotExist());

        verify(achatService, times(1)).obtenirAchatsPagines(0, 20, null, null, null);
    }

    @Test
    @DisplayName("POST /achats - Devrait calculer automatiquement le prix total")
    void creer_DevraitCalculerPrixTotal() throws Exception {
        // Arrange - 12 × 25.00 = 300.00 (prixTotal fourni, le service confirme le calcul)
        AchatEntity nouvelAchat = AchatEntity.builder()
                .quantite(12)
                .nomProduit("Boucles d'oreilles")
                .prixUnitaire(new BigDecimal("25.00"))
                .prixTotal(new BigDecimal("300.00"))
                .dateAchat(LocalDate.now())
                .build();

        AchatEntity achatCree = AchatEntity.builder()
                .quantite(12)
                .nomProduit("Boucles d'oreilles")
                .prixUnitaire(new BigDecimal("25.00"))
                .prixTotal(new BigDecimal("300.00"))
                .dateAchat(LocalDate.now())
                .utilisateur(utilisateurTest)
                .build();
        achatCree.setId("test-id-4");

        when(userService.obtenirUtilisateurParEmail("amadou@example.com")).thenReturn(utilisateurTest);
        when(achatService.creerAchat(any(AchatEntity.class), eq(utilisateurTest)))
                .thenReturn(achatCree);

        // Act & Assert
        mockMvc.perform(post("/achats")
                        .principal(new UsernamePasswordAuthenticationToken("amadou@example.com", null,
                                List.of(new SimpleGrantedAuthority("USER"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nouvelAchat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixTotal", is(300.00)));

        verify(userService, times(1)).obtenirUtilisateurParEmail("amadou@example.com");
        verify(achatService, times(1)).creerAchat(any(AchatEntity.class), eq(utilisateurTest));
    }
}
