package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.TenantEntity;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — StockService")
class StockServiceTest {

    @Mock private AchatRepository achatRepository;
    @Mock private VenteRepository venteRepository;
    @Mock private TenantService tenantService;

    @InjectMocks
    private StockService stockService;

    private AchatEntity achat1;
    private AchatEntity achat2;
    private VenteEntity vente1;
    private TenantEntity tenantTest;

    @BeforeEach
    void setUp() {
        tenantTest = new TenantEntity();
        tenantTest.setTenantUuid("uuid-test");
        tenantTest.setPlan(TenantEntity.Plan.GRATUIT);

        achat1 = AchatEntity.builder()
                .nomProduit("Ordinateur")
                .quantite(10.0)
                .prixUnitaire(new BigDecimal("500.00"))
                .dateAchat(LocalDate.now())
                .build();

        achat2 = AchatEntity.builder()
                .nomProduit("Souris")
                .quantite(50.0)
                .prixUnitaire(new BigDecimal("10.00"))
                .dateAchat(LocalDate.now())
                .build();

        vente1 = VenteEntity.builder()
                .nomProduit("Ordinateur")
                .quantite(3.0)
                .prixUnitaire(new BigDecimal("700.00"))
                .dateVente(LocalDate.now())
                .build();
    }

    // =========================================================
    // obtenirTousLesStocks
    // =========================================================

    @Test
    @DisplayName("obtenirTousLesStocks() — calcule les stocks de 2 produits")
    void obtenirTousLesStocks_calculeCorrectement() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.findAllByTenant(any())).thenReturn(Arrays.asList(achat1, achat2));
        when(venteRepository.findAllByTenant(any())).thenReturn(Arrays.asList(vente1));

        List<StockDto> resultat = stockService.obtenirTousLesStocks();

        assertThat(resultat).hasSize(2);
    }

    @Test
    @DisplayName("obtenirTousLesStocks() — liste vide si aucun achat")
    void obtenirTousLesStocks_retourneVideSiAucunAchat() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.findAllByTenant(any())).thenReturn(Collections.emptyList());
        when(venteRepository.findAllByTenant(any())).thenReturn(Collections.emptyList());

        List<StockDto> resultat = stockService.obtenirTousLesStocks();

        assertThat(resultat).isEmpty();
    }

    // =========================================================
    // obtenirStockParNomProduit
    // =========================================================

    @Test
    @DisplayName("obtenirStockParNomProduit() — calcule le stock correct (10-3=7)")
    void obtenirStockParNomProduit_retourneStock() {
        when(achatRepository.findByNomProduit("Ordinateur")).thenReturn(Arrays.asList(achat1));
        when(venteRepository.findByNomProduit("Ordinateur")).thenReturn(Arrays.asList(vente1));

        StockDto resultat = stockService.obtenirStockParNomProduit("Ordinateur");

        assertThat(resultat).isNotNull();
        assertThat(resultat.getQuantiteAchetee()).isEqualTo(10);
        assertThat(resultat.getQuantiteVendue()).isEqualTo(3);
        assertThat(resultat.getStockDisponible()).isEqualTo(7);
    }

    @Test
    @DisplayName("obtenirStockParNomProduit() — insensible à la casse via fallback")
    void obtenirStockParNomProduit_insensibleCasse() {
        // Exact match retourne rien → fallback vers findByNomProduitContaining
        when(achatRepository.findByNomProduit("ORDINATEUR")).thenReturn(Collections.emptyList());
        when(venteRepository.findByNomProduit("ORDINATEUR")).thenReturn(Collections.emptyList());
        when(achatRepository.findByNomProduitContaining("ORDINATEUR")).thenReturn(Arrays.asList(achat1));
        when(venteRepository.findByNomProduitContaining("ORDINATEUR")).thenReturn(Collections.emptyList());

        StockDto resultat = stockService.obtenirStockParNomProduit("ORDINATEUR");

        assertThat(resultat).isNotNull();
        assertThat(resultat.getStockDisponible()).isEqualTo(10);
    }

    @Test
    @DisplayName("obtenirStockParNomProduit() — lève RuntimeException si produit inexistant")
    void obtenirStockParNomProduit_leveExceptionSiInexistant() {
        when(achatRepository.findByNomProduit("Inexistant")).thenReturn(Collections.emptyList());
        when(venteRepository.findByNomProduit("Inexistant")).thenReturn(Collections.emptyList());
        when(achatRepository.findByNomProduitContaining("Inexistant")).thenReturn(Collections.emptyList());
        when(venteRepository.findByNomProduitContaining("Inexistant")).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> stockService.obtenirStockParNomProduit("Inexistant"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Produit non trouvé");
    }

    // =========================================================
    // obtenirProduitsEnRupture
    // =========================================================

    @Test
    @DisplayName("obtenirProduitsEnRupture() — retourne les produits à stock = 0")
    void obtenirProduitsEnRupture_retourneRupture() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        AchatEntity achatRupture = AchatEntity.builder()
                .nomProduit("Clavier").quantite(5.0).prixUnitaire(new BigDecimal("30.00"))
                .dateAchat(LocalDate.now()).build();
        VenteEntity venteRupture = VenteEntity.builder()
                .nomProduit("Clavier").quantite(5.0).prixUnitaire(new BigDecimal("50.00"))
                .dateVente(LocalDate.now()).build();

        when(achatRepository.findAllByTenant(any())).thenReturn(Arrays.asList(achatRupture));
        when(venteRepository.findAllByTenant(any())).thenReturn(Arrays.asList(venteRupture));

        List<StockDto> resultat = stockService.obtenirProduitsEnRupture();

        assertThat(resultat).isNotEmpty();
        assertThat(resultat.get(0).getStockDisponible()).isEqualTo(0);
    }

    // =========================================================
    // obtenirProduitsStockBas
    // =========================================================

    @Test
    @DisplayName("obtenirProduitsStockBas() — retourne les produits avec stock 1–9")
    void obtenirProduitsStockBas_retourneStockBas() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        AchatEntity achatStockBas = AchatEntity.builder()
                .nomProduit("Ecran").quantite(15.0).prixUnitaire(new BigDecimal("200.00"))
                .dateAchat(LocalDate.now()).build();
        VenteEntity venteStockBas = VenteEntity.builder()
                .nomProduit("Ecran").quantite(10.0).prixUnitaire(new BigDecimal("300.00"))
                .dateVente(LocalDate.now()).build();

        when(achatRepository.findAllByTenant(any())).thenReturn(Arrays.asList(achatStockBas));
        when(venteRepository.findAllByTenant(any())).thenReturn(Arrays.asList(venteStockBas));

        List<StockDto> resultat = stockService.obtenirProduitsStockBas();

        assertThat(resultat).isNotEmpty();
        assertThat(resultat.get(0).getStockDisponible()).isGreaterThan(0).isLessThan(10);
    }

    // =========================================================
    // verifierStockDisponible
    // =========================================================

    @Test
    @DisplayName("verifierStockDisponible() — retourne true si stock suffisant")
    void verifierStockDisponible_retourneTrue() {
        when(achatRepository.findByNomProduit("Ordinateur")).thenReturn(Arrays.asList(achat1));
        when(venteRepository.findByNomProduit("Ordinateur")).thenReturn(Arrays.asList(vente1));

        // Stock = 7, demande = 5
        boolean resultat = stockService.verifierStockDisponible("Ordinateur", 5);

        assertThat(resultat).isTrue();
    }

    @Test
    @DisplayName("verifierStockDisponible() — retourne false si stock insuffisant")
    void verifierStockDisponible_retourneFalse() {
        when(achatRepository.findByNomProduit("Ordinateur")).thenReturn(Arrays.asList(achat1));
        when(venteRepository.findByNomProduit("Ordinateur")).thenReturn(Arrays.asList(vente1));

        // Stock = 7, demande = 20
        boolean resultat = stockService.verifierStockDisponible("Ordinateur", 20);

        assertThat(resultat).isFalse();
    }

    @Test
    @DisplayName("verifierStockDisponible() — retourne false si produit inexistant")
    void verifierStockDisponible_retourneFalseProduitInexistant() {
        when(achatRepository.findByNomProduit("Inexistant")).thenReturn(Collections.emptyList());
        when(venteRepository.findByNomProduit("Inexistant")).thenReturn(Collections.emptyList());
        when(achatRepository.findByNomProduitContaining("Inexistant")).thenReturn(Collections.emptyList());
        when(venteRepository.findByNomProduitContaining("Inexistant")).thenReturn(Collections.emptyList());

        boolean resultat = stockService.verifierStockDisponible("Inexistant", 1);

        assertThat(resultat).isFalse();
    }

    // =========================================================
    // obtenirValeurTotaleStock
    // =========================================================

    @Test
    @DisplayName("obtenirValeurTotaleStock() — somme > 0 si stock disponible")
    void obtenirValeurTotaleStock_retourneValeur() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.findAllByTenant(any())).thenReturn(Arrays.asList(achat1, achat2));
        when(venteRepository.findAllByTenant(any())).thenReturn(Arrays.asList(vente1));

        BigDecimal valeur = stockService.obtenirValeurTotaleStock();

        assertThat(valeur).isNotNull().isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("obtenirValeurTotaleStock() — zéro si aucun stock")
    void obtenirValeurTotaleStock_retourneZeroSiVide() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.findAllByTenant(any())).thenReturn(Collections.emptyList());
        when(venteRepository.findAllByTenant(any())).thenReturn(Collections.emptyList());

        BigDecimal valeur = stockService.obtenirValeurTotaleStock();

        assertThat(valeur).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
