package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.StockAlertHistory;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.repository.StockAlertHistoryRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.repository.VenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — StockAlertService")
class StockAlertServiceTest {

    @Mock private AchatRepository achatRepository;
    @Mock private VenteRepository venteRepository;
    @Mock private StockAlertHistoryRepository stockAlertHistoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private TenantService tenantService;

    @InjectMocks
    private StockAlertService stockAlertService;

    private TenantEntity tenantGratuit;
    private TenantEntity tenantBasic;
    private TenantEntity tenantPremium;
    private TenantEntity tenantEntreprise;
    private UserEntity adminUser;

    @BeforeEach
    void setUp() {
        tenantGratuit = TenantEntity.builder()
                .tenantUuid("tenant-gratuit-001")
                .nomEntreprise("Boutique Gratuite")
                .numeroTelephone("+221770000001")
                .plan(TenantEntity.Plan.GRATUIT)
                .build();

        tenantBasic = TenantEntity.builder()
                .tenantUuid("tenant-basic-001")
                .nomEntreprise("Boutique Basic")
                .numeroTelephone("+221770000002")
                .plan(TenantEntity.Plan.STARTER)
                .build();

        tenantPremium = TenantEntity.builder()
                .tenantUuid("tenant-premium-001")
                .nomEntreprise("Boutique Premium")
                .numeroTelephone("+221770000003")
                .plan(TenantEntity.Plan.PRO)
                .build();

        tenantEntreprise = TenantEntity.builder()
                .tenantUuid("tenant-entreprise-001")
                .nomEntreprise("Boutique Entreprise")
                .numeroTelephone("+221770000004")
                .plan(TenantEntity.Plan.BUSINESS)
                .build();

        adminUser = UserEntity.builder()
                .email("admin@boutique.com")
                .prenom("Aminata")
                .nom("Diallo")
                .role(UserEntity.Role.ADMIN)
                .build();
    }

    // ==================== Tests plan non éligible ====================

    @Test
    @DisplayName("Plan GRATUIT → aucun email envoyé, aucune sauvegarde")
    void verifierEtEnvoyerAlerte_PlanGratuit_AucunEmail() {
        // Arrange
        when(tenantService.getCurrentTenant()).thenReturn(tenantGratuit);

        // Act
        stockAlertService.verifierEtEnvoyerAlerte("Collier en or");

        // Assert
        verify(emailService, never()).sendStockAlertEmail(any(), any(), any(), any(), anyInt(), anyInt());
        verify(stockAlertHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Plan BASIC → aucun email envoyé, aucune sauvegarde")
    void verifierEtEnvoyerAlerte_PlanBasic_AucunEmail() {
        // Arrange
        when(tenantService.getCurrentTenant()).thenReturn(tenantBasic);

        // Act
        stockAlertService.verifierEtEnvoyerAlerte("Bracelet argent");

        // Assert
        verify(emailService, never()).sendStockAlertEmail(any(), any(), any(), any(), anyInt(), anyInt());
        verify(stockAlertHistoryRepository, never()).save(any());
    }

    // ==================== Test stock hors seuil ====================

    @Test
    @DisplayName("Plan PREMIUM, stock=7 (pas un seuil) → aucun email")
    void verifierEtEnvoyerAlerte_PremiumStockHorsSeuil_AucunEmail() {
        // Arrange
        when(tenantService.getCurrentTenant()).thenReturn(tenantPremium);
        when(achatRepository.sumQuantiteByNomProduitAndTenant(eq("Bague diamant"), eq(tenantPremium))).thenReturn(10.0);
        when(venteRepository.sumQuantiteByNomProduitAndTenant(eq("Bague diamant"), eq(tenantPremium))).thenReturn(3.0);
        // stock = 10 - 3 = 7, pas dans [15, 10, 5, 0]

        // Act
        stockAlertService.verifierEtEnvoyerAlerte("Bague diamant");

        // Assert
        verify(emailService, never()).sendStockAlertEmail(any(), any(), any(), any(), anyInt(), anyInt());
        verify(stockAlertHistoryRepository, never()).save(any());
    }

    // ==================== Test alerte déjà envoyée ====================

    @Test
    @DisplayName("Plan PREMIUM, stock=5, alerte déjà envoyée → aucun email")
    void verifierEtEnvoyerAlerte_PremiumStockCinqAlerteDejaEnvoyee_AucunEmail() {
        // Arrange
        when(tenantService.getCurrentTenant()).thenReturn(tenantPremium);
        when(achatRepository.sumQuantiteByNomProduitAndTenant(eq("Pendentif"), eq(tenantPremium))).thenReturn(15.0);
        when(venteRepository.sumQuantiteByNomProduitAndTenant(eq("Pendentif"), eq(tenantPremium))).thenReturn(10.0);
        // stock = 15 - 10 = 5 (seuil exact)
        when(stockAlertHistoryRepository.existsRecentAlert(eq("Pendentif"), eq(5), eq(tenantPremium), any()))
                .thenReturn(true);

        // Act
        stockAlertService.verifierEtEnvoyerAlerte("Pendentif");

        // Assert
        verify(emailService, never()).sendStockAlertEmail(any(), any(), any(), any(), anyInt(), anyInt());
        verify(stockAlertHistoryRepository, never()).save(any());
    }

    // ==================== Test email envoyé + historique sauvegardé ====================

    @Test
    @DisplayName("Plan PREMIUM, stock=5, pas d'alerte récente, admin trouvé → email envoyé + historique sauvegardé")
    void verifierEtEnvoyerAlerte_PremiumStockCinqSansAlerteRecente_EmailEnvoyeEtHistoriqueSauvegarde() {
        // Arrange
        when(tenantService.getCurrentTenant()).thenReturn(tenantPremium);
        when(achatRepository.sumQuantiteByNomProduitAndTenant(eq("Collier or"), eq(tenantPremium))).thenReturn(20.0);
        when(venteRepository.sumQuantiteByNomProduitAndTenant(eq("Collier or"), eq(tenantPremium))).thenReturn(15.0);
        // stock = 20 - 15 = 5
        when(stockAlertHistoryRepository.existsRecentAlert(eq("Collier or"), eq(5), eq(tenantPremium), any()))
                .thenReturn(false);
        when(userRepository.findFirstByTenantAndRole(eq(tenantPremium), eq(UserEntity.Role.ADMIN)))
                .thenReturn(Optional.of(adminUser));

        // Act
        stockAlertService.verifierEtEnvoyerAlerte("Collier or");

        // Assert — email envoyé
        verify(emailService).sendStockAlertEmail(
                eq("admin@boutique.com"),
                eq("Aminata Diallo"),
                eq("Boutique Premium"),
                eq("Collier or"),
                eq(5),
                eq(5)
        );

        // Assert — historique sauvegardé
        ArgumentCaptor<StockAlertHistory> captor = ArgumentCaptor.forClass(StockAlertHistory.class);
        verify(stockAlertHistoryRepository).save(captor.capture());
        StockAlertHistory saved = captor.getValue();
        assertThat(saved.getNomProduit()).isEqualTo("Collier or");
        assertThat(saved.getSeuilAlerte()).isEqualTo(5);
        assertThat(saved.getStockActuel()).isEqualTo(5);
        assertThat(saved.getEmailDestinataire()).isEqualTo("admin@boutique.com");
        assertThat(saved.getTenant()).isEqualTo(tenantPremium);
    }

    // ==================== Test admin null ====================

    @Test
    @DisplayName("Plan ENTREPRISE, stock=0, pas d'alerte, admin null → aucun email, pas de crash")
    void verifierEtEnvoyerAlerte_EntrepriseStockZeroAdminNull_AucunEmailSansCrash() {
        // Arrange
        when(tenantService.getCurrentTenant()).thenReturn(tenantEntreprise);
        when(achatRepository.sumQuantiteByNomProduitAndTenant(eq("Montre"), eq(tenantEntreprise))).thenReturn(5.0);
        when(venteRepository.sumQuantiteByNomProduitAndTenant(eq("Montre"), eq(tenantEntreprise))).thenReturn(5.0);
        // stock = 5 - 5 = 0 (seuil exact)
        when(stockAlertHistoryRepository.existsRecentAlert(eq("Montre"), eq(0), eq(tenantEntreprise), any()))
                .thenReturn(false);
        when(userRepository.findFirstByTenantAndRole(eq(tenantEntreprise), eq(UserEntity.Role.ADMIN)))
                .thenReturn(Optional.empty());

        // Act — ne doit pas lever d'exception
        stockAlertService.verifierEtEnvoyerAlerte("Montre");

        // Assert
        verify(emailService, never()).sendStockAlertEmail(any(), any(), any(), any(), anyInt(), anyInt());
        verify(stockAlertHistoryRepository, never()).save(any());
    }

    // ==================== Test achats/ventes null → stock=0 ====================

    @Test
    @DisplayName("Plan PREMIUM, achats=null + ventes=null → stock=0, pas d'alerte récente, admin trouvé → email envoyé")
    void verifierEtEnvoyerAlerte_PremiumAchatsVentesNull_StockZeroEmailEnvoye() {
        // Arrange
        when(tenantService.getCurrentTenant()).thenReturn(tenantPremium);
        when(achatRepository.sumQuantiteByNomProduitAndTenant(eq("Produit Neuf"), eq(tenantPremium))).thenReturn(null);
        when(venteRepository.sumQuantiteByNomProduitAndTenant(eq("Produit Neuf"), eq(tenantPremium))).thenReturn(null);
        // null - null → 0 - 0 = 0 (seuil exact)
        when(stockAlertHistoryRepository.existsRecentAlert(eq("Produit Neuf"), eq(0), eq(tenantPremium), any()))
                .thenReturn(false);
        when(userRepository.findFirstByTenantAndRole(eq(tenantPremium), eq(UserEntity.Role.ADMIN)))
                .thenReturn(Optional.of(adminUser));

        // Act
        stockAlertService.verifierEtEnvoyerAlerte("Produit Neuf");

        // Assert — email envoyé avec stock=0
        verify(emailService).sendStockAlertEmail(
                eq("admin@boutique.com"),
                eq("Aminata Diallo"),
                eq("Boutique Premium"),
                eq("Produit Neuf"),
                eq(0),
                eq(0)
        );
        verify(stockAlertHistoryRepository).save(any(StockAlertHistory.class));
    }
}
