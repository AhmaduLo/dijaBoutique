package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.repository.VenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour StockService
 *
 * Bonnes pratiques appliquées :
 * - @ExtendWith(MockitoExtension.class) : Active Mockito pour les tests
 * - @Mock : Créer des mocks des repositories
 * - @InjectMocks : Injecter les mocks dans le service testé
 * - AssertJ : Assertions fluides et lisibles
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - @DisplayName : Descriptions claires en français
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du StockService")
class StockServiceTest {

    // Mock du repository des achats
    @Mock
    private AchatRepository achatRepository;

    // Mock du repository des ventes
    @Mock
    private VenteRepository venteRepository;

    // Service à tester avec les mocks injectés
    @InjectMocks
    private StockService stockService;

    // Données de test
    private AchatEntity achat1;
    private AchatEntity achat2;
    private VenteEntity vente1;

    /**
     * Initialisation des données de test avant chaque test
     *
     * Scénario :
     * - Achat de 10 ordinateurs à 500€
     * - Achat de 50 souris à 10€
     * - Vente de 3 ordinateurs à 700€
     * => Stock ordinateur = 10 - 3 = 7
     */
    @BeforeEach
    void setUp() {
        // Achat 1 : 10 ordinateurs
        achat1 = AchatEntity.builder()
                .nomProduit("Ordinateur")
                .quantite(10)
                .prixUnitaire(new BigDecimal("500.00"))
                .dateAchat(LocalDate.now())
                .build();

        // Achat 2 : 50 souris
        achat2 = AchatEntity.builder()
                .nomProduit("Souris")
                .quantite(50)
                .prixUnitaire(new BigDecimal("10.00"))
                .dateAchat(LocalDate.now())
                .build();

        // Vente 1 : 3 ordinateurs
        vente1 = VenteEntity.builder()
                .nomProduit("Ordinateur")
                .quantite(3)
                .prixUnitaire(new BigDecimal("700.00"))
                .dateVente(LocalDate.now())
                .build();
    }

    // ==================== Tests pour obtenirTousLesStocks() ====================

    /**
     * Test : Obtenir tous les stocks avec calcul correct
     *
     * Scénario :
     * - 2 produits achetés (Ordinateur et Souris)
     * - 1 vente (Ordinateur)
     * - Le service doit calculer le stock de chaque produit
     */
    @Test
    @DisplayName("obtenirTousLesStocks() - Devrait calculer les stocks correctement")
    void obtenirTousLesStocks_DevraitCalculer() {
        // Arrange : Préparer les données mockées
        when(achatRepository.findAll()).thenReturn(Arrays.asList(achat1, achat2));
        when(venteRepository.findAll()).thenReturn(Arrays.asList(vente1));

        // Act : Appeler la méthode à tester
        List<StockDto> resultat = stockService.obtenirTousLesStocks();

        // Assert : Vérifier les résultats
        assertThat(resultat).isNotEmpty();
        assertThat(resultat).hasSize(2); // 2 produits : Ordinateur et Souris
    }

    /**
     * Test : Liste vide si aucun achat dans le système
     */
    @Test
    @DisplayName("obtenirTousLesStocks() - Devrait retourner liste vide si aucun achat")
    void obtenirTousLesStocks_DevraitRetournerListeVide() {
        // Arrange : Aucun achat ni vente
        when(achatRepository.findAll()).thenReturn(Arrays.asList());
        when(venteRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<StockDto> resultat = stockService.obtenirTousLesStocks();

        // Assert : La liste doit être vide
        assertThat(resultat).isEmpty();
    }

    // ==================== Tests pour obtenirStockParNomProduit() ====================

    /**
     * Test : Obtenir le stock d'un produit spécifique
     *
     * Scénario :
     * - 10 ordinateurs achetés
     * - 3 ordinateurs vendus
     * - Stock disponible = 10 - 3 = 7
     */
    @Test
    @DisplayName("obtenirStockParNomProduit() - Devrait retourner le stock d'un produit")
    void obtenirStockParNomProduit_DevraitRetourner() {
        // Arrange
        when(achatRepository.findAll()).thenReturn(Arrays.asList(achat1));
        when(venteRepository.findAll()).thenReturn(Arrays.asList(vente1));

        // Act
        StockDto resultat = stockService.obtenirStockParNomProduit("Ordinateur");

        // Assert : Vérifier le calcul du stock
        assertThat(resultat).isNotNull();
        assertThat(resultat.getNomProduit()).isEqualToIgnoringCase("Ordinateur");
        assertThat(resultat.getQuantiteAchetee()).isEqualTo(10);
        assertThat(resultat.getQuantiteVendue()).isEqualTo(3);
        assertThat(resultat.getStockDisponible()).isEqualTo(7); // 10 - 3 = 7
    }

    /**
     * Test : La recherche de produit doit être insensible à la casse
     *
     * Scénario :
     * - Produit enregistré : "Ordinateur"
     * - Recherche avec : "ORDINATEUR" (majuscules)
     * - Doit quand même trouver le produit
     */
    @Test
    @DisplayName("obtenirStockParNomProduit() - Devrait être insensible à la casse")
    void obtenirStockParNomProduit_DevraitEtreInsensibleCasse() {
        // Arrange
        when(achatRepository.findAll()).thenReturn(Arrays.asList(achat1));
        when(venteRepository.findAll()).thenReturn(Arrays.asList());

        // Act : Rechercher avec des majuscules
        StockDto resultat = stockService.obtenirStockParNomProduit("ORDINATEUR");

        // Assert : Le produit doit être trouvé malgré la différence de casse
        assertThat(resultat).isNotNull();
        assertThat(resultat.getStockDisponible()).isEqualTo(10);
    }

    /**
     * Test : Exception si le produit n'existe pas
     *
     * Cas d'erreur : Recherche d'un produit jamais acheté
     */
    @Test
    @DisplayName("obtenirStockParNomProduit() - Devrait lancer exception si produit non trouvé")
    void obtenirStockParNomProduit_DevraitLancerException() {
        // Arrange : Aucun produit dans le système
        when(achatRepository.findAll()).thenReturn(Arrays.asList());
        when(venteRepository.findAll()).thenReturn(Arrays.asList());

        // Act & Assert : Doit lancer une exception
        assertThatThrownBy(() -> stockService.obtenirStockParNomProduit("Inexistant"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Produit non trouvé");
    }

    // ==================== Tests pour obtenirProduitsEnRupture() ====================

    /**
     * Test : Détecter les produits en rupture de stock
     *
     * Scénario :
     * - 5 claviers achetés
     * - 5 claviers vendus
     * - Stock = 0 => Produit en rupture
     */
    @Test
    @DisplayName("obtenirProduitsEnRupture() - Devrait retourner les produits en rupture")
    void obtenirProduitsEnRupture_DevraitRetourner() {
        // Arrange : Créer un produit en rupture (stock = 0)
        AchatEntity achatRupture = AchatEntity.builder()
                .nomProduit("Clavier")
                .quantite(5)
                .prixUnitaire(new BigDecimal("30.00"))
                .build();

        VenteEntity venteRupture = VenteEntity.builder()
                .nomProduit("Clavier")
                .quantite(5)
                .prixUnitaire(new BigDecimal("50.00"))
                .build();

        when(achatRepository.findAll()).thenReturn(Arrays.asList(achatRupture));
        when(venteRepository.findAll()).thenReturn(Arrays.asList(venteRupture));

        // Act
        List<StockDto> resultat = stockService.obtenirProduitsEnRupture();

        // Assert : Le produit doit être en rupture
        assertThat(resultat).isNotEmpty();
        assertThat(resultat.get(0).getStockDisponible()).isEqualTo(0);
    }

    // ==================== Tests pour obtenirProduitsStockBas() ====================

    /**
     * Test : Détecter les produits avec stock bas (< 10 unités)
     *
     * Scénario :
     * - 15 écrans achetés
     * - 10 écrans vendus
     * - Stock = 5 => Stock bas (entre 1 et 9)
     */
    @Test
    @DisplayName("obtenirProduitsStockBas() - Devrait retourner les produits avec stock bas")
    void obtenirProduitsStockBas_DevraitRetourner() {
        // Arrange : Créer un produit avec stock bas
        AchatEntity achatStockBas = AchatEntity.builder()
                .nomProduit("Ecran")
                .quantite(15)
                .prixUnitaire(new BigDecimal("200.00"))
                .build();

        VenteEntity venteStockBas = VenteEntity.builder()
                .nomProduit("Ecran")
                .quantite(10)
                .prixUnitaire(new BigDecimal("300.00"))
                .build();

        when(achatRepository.findAll()).thenReturn(Arrays.asList(achatStockBas));
        when(venteRepository.findAll()).thenReturn(Arrays.asList(venteStockBas));

        // Act
        List<StockDto> resultat = stockService.obtenirProduitsStockBas();

        // Assert : Le stock doit être bas (< 10 et > 0)
        assertThat(resultat).isNotEmpty();
        assertThat(resultat.get(0).getStockDisponible()).isLessThan(10);
        assertThat(resultat.get(0).getStockDisponible()).isGreaterThan(0);
    }

    // ==================== Tests pour verifierStockDisponible() ====================

    /**
     * Test : Vérifier si le stock est suffisant pour une vente
     *
     * Scénario :
     * - Stock disponible : 7 ordinateurs
     * - Demande : 5 ordinateurs
     * - Résultat : true (stock suffisant)
     */
    @Test
    @DisplayName("verifierStockDisponible() - Devrait retourner true si stock suffisant")
    void verifierStockDisponible_DevraitRetournerTrue() {
        // Arrange : Stock = 7 (10 achetés - 3 vendus)
        when(achatRepository.findAll()).thenReturn(Arrays.asList(achat1));
        when(venteRepository.findAll()).thenReturn(Arrays.asList(vente1));

        // Act : Vérifier si on peut vendre 5 unités
        boolean resultat = stockService.verifierStockDisponible("Ordinateur", 5);

        // Assert : 7 >= 5 => true
        assertThat(resultat).isTrue();
    }

    /**
     * Test : Stock insuffisant pour une vente
     *
     * Scénario :
     * - Stock disponible : 7 ordinateurs
     * - Demande : 20 ordinateurs
     * - Résultat : false (stock insuffisant)
     */
    @Test
    @DisplayName("verifierStockDisponible() - Devrait retourner false si stock insuffisant")
    void verifierStockDisponible_DevraitRetournerFalse() {
        // Arrange : Stock = 7
        when(achatRepository.findAll()).thenReturn(Arrays.asList(achat1));
        when(venteRepository.findAll()).thenReturn(Arrays.asList(vente1));

        // Act : Vérifier si on peut vendre 20 unités
        boolean resultat = stockService.verifierStockDisponible("Ordinateur", 20);

        // Assert : 7 < 20 => false
        assertThat(resultat).isFalse();
    }

    /**
     * Test : Produit inexistant = stock insuffisant
     */
    @Test
    @DisplayName("verifierStockDisponible() - Devrait retourner false si produit non trouvé")
    void verifierStockDisponible_DevraitRetournerFalseProduitNonTrouve() {
        // Arrange : Aucun produit
        when(achatRepository.findAll()).thenReturn(Arrays.asList());
        when(venteRepository.findAll()).thenReturn(Arrays.asList());

        // Act : Vérifier un produit inexistant
        boolean resultat = stockService.verifierStockDisponible("Inexistant", 1);

        // Assert : Produit non trouvé => false
        assertThat(resultat).isFalse();
    }

    // ==================== Tests pour obtenirValeurTotaleStock() ====================

    /**
     * Test : Calculer la valeur totale du stock
     *
     * Formule : Σ (stockDisponible × prixMoyenAchat) pour chaque produit
     */
    @Test
    @DisplayName("obtenirValeurTotaleStock() - Devrait calculer la valeur totale")
    void obtenirValeurTotaleStock_DevraitCalculer() {
        // Arrange : 2 produits avec du stock
        when(achatRepository.findAll()).thenReturn(Arrays.asList(achat1, achat2));
        when(venteRepository.findAll()).thenReturn(Arrays.asList(vente1));

        // Act
        BigDecimal valeurTotale = stockService.obtenirValeurTotaleStock();

        // Assert : La valeur doit être > 0
        assertThat(valeurTotale).isNotNull();
        assertThat(valeurTotale).isGreaterThan(BigDecimal.ZERO);
    }

    /**
     * Test : Valeur nulle si aucun stock
     */
    @Test
    @DisplayName("obtenirValeurTotaleStock() - Devrait retourner zéro si aucun stock")
    void obtenirValeurTotaleStock_DevraitRetournerZero() {
        // Arrange : Aucun produit
        when(achatRepository.findAll()).thenReturn(Arrays.asList());
        when(venteRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        BigDecimal valeurTotale = stockService.obtenirValeurTotaleStock();

        // Assert : Valeur = 0
        assertThat(valeurTotale).isEqualTo(BigDecimal.ZERO);
    }
}
