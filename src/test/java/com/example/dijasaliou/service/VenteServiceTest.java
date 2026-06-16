package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.*;
import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import com.example.dijasaliou.entity.PaiementCreditEntity.ModePaiement;
import com.example.dijasaliou.repository.ClientRepository;
import com.example.dijasaliou.repository.CreditClientRepository;
import com.example.dijasaliou.repository.PaiementCreditRepository;
import com.example.dijasaliou.repository.VenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — VenteService")
class VenteServiceTest {

    @Mock private VenteRepository venteRepository;
    @Mock private StockService stockService;
    @Mock private TenantService tenantService;
    @Mock private StockAlertService stockAlertService;
    @Mock private CreditClientService creditClientService;
    @Mock private ClientRepository clientRepository;
    @Mock private CreditClientRepository creditClientRepository;
    @Mock private PaiementCreditRepository paiementCreditRepository;

    @InjectMocks
    private VenteService venteService;

    private VenteEntity venteValide;
    private UserEntity utilisateurTest;
    private TenantEntity tenantTest;

    @BeforeEach
    void setUp() {
        tenantTest = new TenantEntity();
        tenantTest.setTenantUuid("uuid-tenant-test");

        utilisateurTest = UserEntity.builder()
                .nom("Diop")
                .email("amadou@example.com")
                .build();

        venteValide = VenteEntity.builder()
                .nomProduit("Ordinateur")
                .quantite(2.0)
                .prixUnitaire(new BigDecimal("500.00"))
                .prixTotal(new BigDecimal("1000.00"))
                .client("Client A")
                .dateVente(LocalDateTime.now())
                .utilisateur(utilisateurTest)
                .tenant(tenantTest)
                .build();
    }

    // =========================================================
    // obtenirToutesLesVentes
    // =========================================================

    @Test
    @DisplayName("obtenirToutesLesVentes() — retourne toutes les ventes")
    void obtenirToutesLesVentes_retourneListe() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.findAllByTenant(tenantTest)).thenReturn(List.of(venteValide));

        List<VenteEntity> resultat = venteService.obtenirToutesLesVentes();

        assertThat(resultat).hasSize(1);
        verify(venteRepository).findAllByTenant(tenantTest);
    }

    // =========================================================
    // obtenirVenteParId
    // =========================================================

    @Test
    @DisplayName("obtenirVenteParId() — retourne la vente si elle existe")
    void obtenirVenteParId_retourneVente() {
        venteValide.setId("test-id-1");
        when(venteRepository.findById("test-id-1")).thenReturn(Optional.of(venteValide));

        VenteEntity resultat = venteService.obtenirVenteParId("test-id-1");

        assertThat(resultat.getId()).isEqualTo("test-id-1");
        assertThat(resultat.getNomProduit()).isEqualTo("Ordinateur");
    }

    @Test
    @DisplayName("obtenirVenteParId() — lève RuntimeException si non trouvée")
    void obtenirVenteParId_leveExceptionSiAbsente() {
        when(venteRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venteService.obtenirVenteParId("999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Vente non trouvée");
    }

    // =========================================================
    // creerVente — validations
    // =========================================================

    @Test
    @DisplayName("creerVente() — lève exception si quantité = 0")
    void creerVente_leveExceptionQuantiteZero() {
        VenteEntity vente = VenteEntity.builder()
                .nomProduit("Produit")
                .quantite(0.0)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> venteService.creerVente(vente, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantité");
    }

    @Test
    @DisplayName("creerVente() — lève exception si quantité négative")
    void creerVente_leveExceptionQuantiteNegative() {
        VenteEntity vente = VenteEntity.builder()
                .nomProduit("Produit")
                .quantite(-5.0)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> venteService.creerVente(vente, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantité");
    }

    @Test
    @DisplayName("creerVente() — lève exception si prix = 0")
    void creerVente_leveExceptionPrixZero() {
        VenteEntity vente = VenteEntity.builder()
                .nomProduit("Produit")
                .quantite(5.0)
                .prixUnitaire(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> venteService.creerVente(vente, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prix");
    }

    @Test
    @DisplayName("creerVente() — lève exception si nom produit vide")
    void creerVente_leveExceptionNomProduitVide() {
        VenteEntity vente = VenteEntity.builder()
                .nomProduit("")
                .quantite(5.0)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> venteService.creerVente(vente, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nom du produit");
    }

    @Test
    @DisplayName("creerVente() — lève exception si nom produit null")
    void creerVente_leveExceptionNomProduitNull() {
        VenteEntity vente = VenteEntity.builder()
                .nomProduit(null)
                .quantite(5.0)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> venteService.creerVente(vente, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nom du produit");
    }

    // =========================================================
    // creerVente — stock
    // =========================================================

    @Test
    @DisplayName("creerVente() — succès avec stock suffisant")
    void creerVente_succes_stockSuffisant() {
        StockDto stock = StockDto.builder()
                .nomProduit("Ordinateur")
                .stockDisponible(10.0)
                .build();

        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(stockService.obtenirStockParNomProduit("Ordinateur")).thenReturn(stock);
        when(venteRepository.save(any())).thenReturn(venteValide);
        doNothing().when(stockAlertService).verifierEtEnvoyerAlerte(any());

        VenteEntity resultat = venteService.creerVente(venteValide, utilisateurTest);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getNomProduit()).isEqualTo("Ordinateur");
        verify(venteRepository).save(any());
    }

    @Test
    @DisplayName("creerVente() — lève exception si stock insuffisant")
    void creerVente_leveExceptionStockInsuffisant() {
        StockDto stock = StockDto.builder()
                .nomProduit("Ordinateur")
                .stockDisponible(1.0)
                .build();

        when(stockService.obtenirStockParNomProduit("Ordinateur")).thenReturn(stock);

        assertThatThrownBy(() -> venteService.creerVente(venteValide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuffisant");
    }

    @Test
    @DisplayName("creerVente() — autorisée si produit jamais acheté (pas dans le stock)")
    void creerVente_autorisee_produitInexistantDansStock() {
        when(stockService.obtenirStockParNomProduit("Ordinateur"))
                .thenThrow(new RuntimeException("Produit non trouvé : Ordinateur"));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.save(any())).thenReturn(venteValide);
        doNothing().when(stockAlertService).verifierEtEnvoyerAlerte(any());

        VenteEntity resultat = venteService.creerVente(venteValide, utilisateurTest);

        assertThat(resultat).isNotNull();
    }

    // =========================================================
    // creerVente — mode CREDIT
    // =========================================================

    @Test
    @DisplayName("creerVente() — mode CREDIT avec client enregistré crée un crédit")
    void creerVente_modeCREDIT_creeCredit() {
        ClientEntity client = new ClientEntity();
        client.setId("test-id-10");
        client.setNom("Client Test");

        VenteEntity venteCredit = VenteEntity.builder()
                .nomProduit("Téléphone")
                .quantite(1.0)
                .prixUnitaire(new BigDecimal("200.00"))
                .prixTotal(new BigDecimal("200.00"))
                .dateVente(LocalDateTime.now())
                .modePaiement(VenteEntity.ModePaiementVente.CREDIT)
                .clientRef(client)
                .dateEcheance(LocalDate.now().plusDays(30))
                .build();

        StockDto stock = StockDto.builder().nomProduit("Téléphone").stockDisponible(5.0).build();

        when(stockService.obtenirStockParNomProduit("Téléphone")).thenReturn(stock);
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.save(any())).thenReturn(venteCredit);
        doNothing().when(stockAlertService).verifierEtEnvoyerAlerte(any());

        venteService.creerVente(venteCredit, utilisateurTest);

        verify(creditClientService).creerCreditDepuisVente(any(), eq(client), eq(utilisateurTest), any());
    }

    @Test
    @DisplayName("creerVente() — mode CREDIT sans client lève exception")
    void creerVente_modeCREDIT_sansClient_leveException() {
        VenteEntity venteCredit = VenteEntity.builder()
                .nomProduit("Téléphone")
                .quantite(1.0)
                .prixUnitaire(new BigDecimal("200.00"))
                .dateVente(LocalDateTime.now())
                .modePaiement(VenteEntity.ModePaiementVente.CREDIT)
                .clientRef(null)
                .clientId(null)
                .build();

        StockDto stock = StockDto.builder().nomProduit("Téléphone").stockDisponible(5.0).build();

        when(stockService.obtenirStockParNomProduit("Téléphone")).thenReturn(stock);
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.save(any())).thenReturn(venteCredit);
        doNothing().when(stockAlertService).verifierEtEnvoyerAlerte(any());

        assertThatThrownBy(() -> venteService.creerVente(venteCredit, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("client enregistré");
    }

    // =========================================================
    // supprimerVente
    // =========================================================

    @Test
    @DisplayName("supprimerVente() — supprime une vente ESPECES")
    void supprimerVente_supprimeSansCredit() {
        venteValide.setId("test-id-1");
        venteValide.setModePaiement(VenteEntity.ModePaiementVente.ESPECES);

        when(venteRepository.findById("test-id-1")).thenReturn(Optional.of(venteValide));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        venteService.supprimerVente("test-id-1");

        verify(venteRepository).deleteById("test-id-1");
        verify(creditClientService, never()).supprimerCreditsDeLaVente(any());
    }

    @Test
    @DisplayName("supprimerVente() — supprime les crédits liés si mode CREDIT")
    void supprimerVente_supprimeCreditLies() {
        venteValide.setId("test-id-2");
        venteValide.setModePaiement(VenteEntity.ModePaiementVente.CREDIT);

        when(venteRepository.findById("test-id-2")).thenReturn(Optional.of(venteValide));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        venteService.supprimerVente("test-id-2");

        verify(creditClientService).solderEtDetacherCreditsDeLaVente("test-id-2");
        verify(venteRepository).deleteById("test-id-2");
    }

    @Test
    @DisplayName("supprimerVente() — lève exception si vente non trouvée")
    void supprimerVente_leveExceptionSiAbsente() {
        when(venteRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venteService.supprimerVente("999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Vente non trouvée");
    }

    @Test
    @DisplayName("supprimerVente() — lève SecurityException si tenant différent")
    void supprimerVente_leveSecurityExceptionTenantDifferent() {
        TenantEntity autretenant = new TenantEntity();
        autretenant.setTenantUuid("uuid-autre-tenant");
        venteValide.setId("test-id-3");

        when(venteRepository.findById("test-id-3")).thenReturn(Optional.of(venteValide));
        when(tenantService.getCurrentTenant()).thenReturn(autreenant());

        assertThatThrownBy(() -> venteService.supprimerVente("test-id-3"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Accès refusé");
    }

    // =========================================================
    // obtenirVentesParUtilisateur / obtenirVentesParPeriode
    // =========================================================

    @Test
    @DisplayName("obtenirVentesParUtilisateur() — retourne les ventes de l'utilisateur")
    void obtenirVentesParUtilisateur_retourneListe() {
        when(venteRepository.findByUtilisateur(utilisateurTest)).thenReturn(List.of(venteValide));

        List<VenteEntity> resultat = venteService.obtenirVentesParUtilisateur(utilisateurTest);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getClient()).isEqualTo("Client A");
    }

    @Test
    @DisplayName("obtenirVentesParPeriode() — retourne les ventes de la période")
    void obtenirVentesParPeriode_retourneListe() {
        LocalDate debut = LocalDate.of(2025, 1, 1);
        LocalDate fin = LocalDate.of(2025, 12, 31);
        when(venteRepository.findByDateVenteBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(venteValide));

        List<VenteEntity> resultat = venteService.obtenirVentesParPeriode(debut, fin);

        assertThat(resultat).hasSize(1);
    }

    // =========================================================
    // calculerChiffreAffaires
    // =========================================================

    @Test
    @DisplayName("calculerChiffreAffaires() — cash basis : ventes non-crédit + paiements crédit reçus")
    void calculerChiffreAffaires_sommeListe() {
        LocalDate debut = LocalDate.of(2025, 1, 1);
        LocalDate fin = LocalDate.of(2025, 12, 31);
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.sumChiffreAffairesNonCreditPeriode(any(LocalDateTime.class), any(LocalDateTime.class), eq(tenantTest.getTenantUuid())))
                .thenReturn(new BigDecimal("1000.00"));
        when(paiementCreditRepository.sumMontantPayeBetweenAndTenant(eq(debut), eq(fin), eq(tenantTest.getTenantUuid())))
                .thenReturn(new BigDecimal("500.00"));

        BigDecimal ca = venteService.calculerChiffreAffaires(debut, fin);

        assertThat(ca).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("calculerChiffreAffaires() — retourne zéro si aucune vente ni paiement crédit")
    void calculerChiffreAffaires_retourneZeroSiVide() {
        LocalDate debut = LocalDate.of(2025, 1, 1);
        LocalDate fin = LocalDate.of(2025, 12, 31);
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.sumChiffreAffairesNonCreditPeriode(any(LocalDateTime.class), any(LocalDateTime.class), eq(tenantTest.getTenantUuid())))
                .thenReturn(BigDecimal.ZERO);
        when(paiementCreditRepository.sumMontantPayeBetweenAndTenant(eq(debut), eq(fin), eq(tenantTest.getTenantUuid())))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal ca = venteService.calculerChiffreAffaires(debut, fin);

        assertThat(ca).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================
    // getProchainNumeroFacture
    // =========================================================

    @Test
    @DisplayName("getProchainNumeroFacture() — format FAC-XXX avec padding 3 chiffres")
    void getProchainNumeroFacture_formatCorrect() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.countByTenantUuid("uuid-tenant-test")).thenReturn(4L);

        String numero = venteService.getProchainNumeroFacture();

        assertThat(numero).isEqualTo("FAC-005");
    }

    @Test
    @DisplayName("getProchainNumeroFacture() — premier numéro = FAC-001 quand aucune vente")
    void getProchainNumeroFacture_premierNumero() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.countByTenantUuid("uuid-tenant-test")).thenReturn(0L);

        String numero = venteService.getProchainNumeroFacture();

        assertThat(numero).isEqualTo("FAC-001");
    }

    @Test
    @DisplayName("getProchainNumeroFacture() — padding correct à 3+ chiffres (FAC-100)")
    void getProchainNumeroFacture_paddingGrandNombre() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.countByTenantUuid("uuid-tenant-test")).thenReturn(99L);

        String numero = venteService.getProchainNumeroFacture();

        assertThat(numero).isEqualTo("FAC-100");
    }

    // =========================================================
    // calculerRapportModePaiement
    // =========================================================

    @Test
    @DisplayName("calculerRapportModePaiement() — retourne les 4 modes même si aucune vente")
    void calculerRapportModePaiement_retourne4ModesSansVentes() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.sumDirectVentesParModeEtPeriode(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(creditClientRepository.sumCreditsRestantParPeriode(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(paiementCreditRepository.sumParModeEtPeriode(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Map<String, Object> rapport = venteService.calculerRapportModePaiement(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        assertThat(rapport).containsKeys("ESPECES", "WAVE", "ORANGE_MONEY", "CREDIT");
    }

    @Test
    @DisplayName("calculerRapportModePaiement() — total ESPECES correct avec ventes directes")
    void calculerRapportModePaiement_totalEspecesCorrect() {
        List<Object[]> directVentes = new java.util.ArrayList<>();
        directVentes.add(new Object[]{VenteEntity.ModePaiementVente.ESPECES, 3L, new BigDecimal("1500.00")});

        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.sumDirectVentesParModeEtPeriode(any(), any(), any(), any()))
                .thenReturn(directVentes);
        when(creditClientRepository.sumCreditsRestantParPeriode(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(paiementCreditRepository.sumParModeEtPeriode(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Map<String, Object> rapport = venteService.calculerRapportModePaiement(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        @SuppressWarnings("unchecked")
        Map<String, Object> especes = (Map<String, Object>) rapport.get("ESPECES");
        assertThat(especes.get("nombre")).isEqualTo(3L);
        assertThat((BigDecimal) especes.get("total")).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("calculerRapportModePaiement() — total CREDIT depuis les crédits restants")
    void calculerRapportModePaiement_totalCreditCorrect() {
        List<Object[]> creditRows = new java.util.ArrayList<>();
        creditRows.add(new Object[]{2L, new BigDecimal("800.00")});

        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.sumDirectVentesParModeEtPeriode(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(creditClientRepository.sumCreditsRestantParPeriode(any(), any(), any(), any()))
                .thenReturn(creditRows);
        when(paiementCreditRepository.sumParModeEtPeriode(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Map<String, Object> rapport = venteService.calculerRapportModePaiement(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        @SuppressWarnings("unchecked")
        Map<String, Object> credit = (Map<String, Object>) rapport.get("CREDIT");
        assertThat(credit.get("nombre")).isEqualTo(2L);
        assertThat((BigDecimal) credit.get("total")).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    @DisplayName("calculerRapportModePaiement() — remboursements ajoutés aux totaux directs")
    void calculerRapportModePaiement_remboursementsAjoutesAuxTotaux() {
        List<Object[]> directVentes = new java.util.ArrayList<>();
        directVentes.add(new Object[]{VenteEntity.ModePaiementVente.ESPECES, 2L, new BigDecimal("1000.00")});

        List<Object[]> remboursements = new java.util.ArrayList<>();
        remboursements.add(new Object[]{ModePaiement.ESPECES, 1L, new BigDecimal("300.00")});

        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.sumDirectVentesParModeEtPeriode(any(), any(), any(), any()))
                .thenReturn(directVentes);
        when(creditClientRepository.sumCreditsRestantParPeriode(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(paiementCreditRepository.sumParModeEtPeriode(any(), any(), any()))
                .thenReturn(remboursements);

        Map<String, Object> rapport = venteService.calculerRapportModePaiement(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        @SuppressWarnings("unchecked")
        Map<String, Object> especes = (Map<String, Object>) rapport.get("ESPECES");
        // 1000 + 300 = 1300
        assertThat((BigDecimal) especes.get("total")).isEqualByComparingTo(new BigDecimal("1300.00"));
        assertThat(especes.get("nombre")).isEqualTo(3L); // 2 + 1
    }

    // =========================================================
    // modifierVente
    // =========================================================

    @Test
    @DisplayName("modifierVente() — modifie et sauvegarde la vente")
    void modifierVente_succes() {
        venteValide.setId("test-id-1");
        VenteEntity modifications = VenteEntity.builder()
                .quantite(1.0) // quantité réduite → même nom + qté ≤ ancienne → pas d'appel au StockService
                .nomProduit("Ordinateur")
                .prixUnitaire(new BigDecimal("600.00"))
                .dateVente(LocalDateTime.now())
                .build();

        when(venteRepository.findById("test-id-1")).thenReturn(Optional.of(venteValide));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(venteRepository.save(any())).thenReturn(venteValide);

        VenteEntity resultat = venteService.modifierVente("test-id-1", modifications);

        assertThat(resultat).isNotNull();
        verify(venteRepository).save(venteValide);
        verify(stockService, never()).obtenirStockParNomProduit(any());
    }

    @Test
    @DisplayName("modifierVente() — lève SecurityException si tenant différent")
    void modifierVente_leveSecurityExceptionTenantDifferent() {
        venteValide.setId("test-id-1");
        VenteEntity modifications = VenteEntity.builder()
                .quantite(3.0).nomProduit("Ordinateur")
                .prixUnitaire(new BigDecimal("600.00"))
                .dateVente(LocalDateTime.now())
                .build();

        when(venteRepository.findById("test-id-1")).thenReturn(Optional.of(venteValide));
        when(tenantService.getCurrentTenant()).thenReturn(autreenant());

        assertThatThrownBy(() -> venteService.modifierVente("test-id-1", modifications))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Accès refusé");
    }

    // =========================================================
    // obtenirVentesPaginees / obtenirVentesParUtilisateurPaginees
    // =========================================================

    @Test
    @DisplayName("obtenirVentesPaginees() — retourne une page de ventes")
    void obtenirVentesPaginees_retournePage() {
        Page<VenteEntity> pageMock = new PageImpl<>(Collections.emptyList());
        when(venteRepository.findAllWithSearch(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(pageMock);

        var resultat = venteService.obtenirVentesPaginees(0, 10, null, null, null);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getContent()).isEmpty();
        verify(venteRepository).findAllWithSearch(anyString(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("obtenirVentesParUtilisateurPaginees() — retourne une page filtrée par utilisateur")
    void obtenirVentesParUtilisateurPaginees_retournePage() {
        Page<VenteEntity> pageMock = new PageImpl<>(Collections.emptyList());
        when(venteRepository.findByUtilisateurWithSearch(
                eq(utilisateurTest), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(pageMock);

        var resultat = venteService.obtenirVentesParUtilisateurPaginees(
                utilisateurTest, 0, 10, null, null, null);

        assertThat(resultat).isNotNull();
        verify(venteRepository).findByUtilisateurWithSearch(
                eq(utilisateurTest), anyString(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    // =========================================================
    // Helpers
    // =========================================================

    private TenantEntity autreenant() {
        TenantEntity autre = new TenantEntity();
        autre.setTenantUuid("uuid-autre-tenant");
        return autre;
    }
}
