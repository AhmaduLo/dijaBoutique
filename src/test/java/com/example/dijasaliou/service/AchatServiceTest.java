package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.AchatRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — AchatService")
class AchatServiceTest {

    @Mock private AchatRepository achatRepository;
    @Mock private TenantService tenantService;
    @Mock private StockService stockService;

    @InjectMocks
    private AchatService achatService;

    private AchatEntity achatValide;
    private UserEntity utilisateurTest;
    private TenantEntity tenantTest;

    @BeforeEach
    void setUp() {
        tenantTest = new TenantEntity();
        tenantTest.setTenantUuid("uuid-tenant-test");

        utilisateurTest = UserEntity.builder()
                .id(1L).nom("Diop").prenom("Amadou")
                .email("amadou@example.com").role(UserEntity.Role.USER)
                .build();

        achatValide = AchatEntity.builder()
                .nomProduit("Ordinateur portable")
                .quantite(5.0)
                .prixUnitaire(new BigDecimal("500.00"))
                .prixTotal(new BigDecimal("2500.00"))
                .fournisseur("Dell")
                .dateAchat(LocalDateTime.now())
                .utilisateur(utilisateurTest)
                .tenant(tenantTest)
                .build();
        achatValide.setId("test-id-1");
    }

    // =========================================================
    // obtenirTousLesAchats
    // =========================================================

    @Test
    @DisplayName("obtenirTousLesAchats() — retourne tous les achats")
    void obtenirTousLesAchats_retourneListe() {
        AchatEntity achat2 = AchatEntity.builder().nomProduit("Souris")
                .quantite(10.0).prixUnitaire(new BigDecimal("15.00")).build();
        achat2.setId("test-id-2");
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.findAllByTenant(tenantTest)).thenReturn(Arrays.asList(achatValide, achat2));

        List<AchatEntity> resultat = achatService.obtenirTousLesAchats();

        assertThat(resultat).hasSize(2).containsExactly(achatValide, achat2);
        verify(achatRepository).findAllByTenant(tenantTest);
    }

    @Test
    @DisplayName("obtenirTousLesAchats() — liste vide si aucun achat")
    void obtenirTousLesAchats_retourneListeVide() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.findAllByTenant(tenantTest)).thenReturn(Collections.emptyList());

        assertThat(achatService.obtenirTousLesAchats()).isEmpty();
    }

    // =========================================================
    // obtenirAchatParId
    // =========================================================

    @Test
    @DisplayName("obtenirAchatParId() — retourne l'achat si trouvé")
    void obtenirAchatParId_retourneAchat() {
        when(achatRepository.findById("test-id-1")).thenReturn(Optional.of(achatValide));

        AchatEntity resultat = achatService.obtenirAchatParId("test-id-1");

        assertThat(resultat.getId()).isEqualTo("test-id-1");
        assertThat(resultat.getNomProduit()).isEqualTo("Ordinateur portable");
    }

    @Test
    @DisplayName("obtenirAchatParId() — lève exception si non trouvé")
    void obtenirAchatParId_leveException() {
        when(achatRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> achatService.obtenirAchatParId("999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Achat non trouvé avec l'ID : 999");
    }

    // =========================================================
    // creerAchat — validations
    // =========================================================

    @Test
    @DisplayName("creerAchat() — lève exception si quantité = 0")
    void creerAchat_leveExceptionQuantiteZero() {
        AchatEntity a = AchatEntity.builder().nomProduit("Produit").quantite(0.0)
                .prixUnitaire(new BigDecimal("100.00")).build();

        assertThatThrownBy(() -> achatService.creerAchat(a, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La quantité doit être supérieure à 0");
        verify(achatRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerAchat() — lève exception si quantité nulle")
    void creerAchat_leveExceptionQuantiteNull() {
        AchatEntity a = AchatEntity.builder().nomProduit("Produit").quantite(null)
                .prixUnitaire(new BigDecimal("100.00")).build();

        assertThatThrownBy(() -> achatService.creerAchat(a, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("creerAchat() — lève exception si prix = 0")
    void creerAchat_leveExceptionPrixZero() {
        AchatEntity a = AchatEntity.builder().nomProduit("Produit").quantite(5.0)
                .prixUnitaire(BigDecimal.ZERO).build();

        assertThatThrownBy(() -> achatService.creerAchat(a, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le prix unitaire doit être supérieur à 0");
    }

    @Test
    @DisplayName("creerAchat() — lève exception si nom produit vide")
    void creerAchat_leveExceptionNomProduitVide() {
        AchatEntity a = AchatEntity.builder().nomProduit("").quantite(5.0)
                .prixUnitaire(new BigDecimal("100.00")).build();

        assertThatThrownBy(() -> achatService.creerAchat(a, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le nom du produit est obligatoire");
    }

    // =========================================================
    // creerAchat — succès
    // =========================================================

    @Test
    @DisplayName("creerAchat() — crée l'achat et assigne l'utilisateur et le tenant")
    void creerAchat_succes() {
        AchatEntity nouvelAchat = AchatEntity.builder()
                .nomProduit("Clavier").quantite(3.0)
                .prixUnitaire(new BigDecimal("50.00"))
                .fournisseur("Logitech").dateAchat(LocalDateTime.now()).build();

        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.save(any())).thenReturn(nouvelAchat);

        AchatEntity resultat = achatService.creerAchat(nouvelAchat, utilisateurTest);

        assertThat(resultat).isNotNull();
        assertThat(nouvelAchat.getUtilisateur()).isEqualTo(utilisateurTest);
        verify(achatRepository).save(any());
    }

    @Test
    @DisplayName("creerAchat() — calcule le prix total si absent")
    void creerAchat_calculePrixTotal() {
        AchatEntity achatSansPrixTotal = AchatEntity.builder()
                .nomProduit("Moniteur").quantite(2.0)
                .prixUnitaire(new BigDecimal("300.00"))
                .dateAchat(LocalDateTime.now()).build();

        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.save(any())).thenReturn(achatSansPrixTotal);

        achatService.creerAchat(achatSansPrixTotal, utilisateurTest);

        verify(achatRepository).save(any());
    }

    // =========================================================
    // modifierAchat
    // =========================================================

    @Test
    @DisplayName("modifierAchat() — modifie un achat existant du même tenant")
    void modifierAchat_succes() {
        AchatEntity modifie = AchatEntity.builder()
                .nomProduit("Ordinateur portable").quantite(8.0)
                .prixUnitaire(new BigDecimal("450.00"))
                .fournisseur("HP").dateAchat(LocalDateTime.now()).build();

        when(achatRepository.findById("test-id-1")).thenReturn(Optional.of(achatValide));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.save(any())).thenReturn(achatValide);

        AchatEntity resultat = achatService.modifierAchat("test-id-1", modifie);

        assertThat(resultat).isNotNull();
        verify(achatRepository).save(any());
    }

    @Test
    @DisplayName("modifierAchat() — lève exception si achat non trouvé")
    void modifierAchat_leveExceptionSiAbsent() {
        AchatEntity modifie = AchatEntity.builder()
                .nomProduit("Produit").quantite(1.0)
                .prixUnitaire(new BigDecimal("100.00")).build();

        when(achatRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> achatService.modifierAchat("999", modifie))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Achat non trouvé avec l'ID : 999");
    }

    @Test
    @DisplayName("modifierAchat() — lève SecurityException si tenant différent")
    void modifierAchat_leveSecurityExceptionTenantDifferent() {
        TenantEntity autreTenant = new TenantEntity();
        autreTenant.setTenantUuid("uuid-autre");

        AchatEntity modifie = AchatEntity.builder()
                .nomProduit("Produit").quantite(1.0)
                .prixUnitaire(new BigDecimal("100.00")).build();

        when(achatRepository.findById("test-id-1")).thenReturn(Optional.of(achatValide));
        when(tenantService.getCurrentTenant()).thenReturn(autreTenant);

        assertThatThrownBy(() -> achatService.modifierAchat("test-id-1", modifie))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Accès refusé");
    }

    // =========================================================
    // supprimerAchat
    // =========================================================

    @Test
    @DisplayName("supprimerAchat() — supprime un achat du même tenant")
    void supprimerAchat_succes() {
        when(achatRepository.findById("test-id-1")).thenReturn(Optional.of(achatValide));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        achatService.supprimerAchat("test-id-1");

        verify(achatRepository).deleteById("test-id-1");
    }

    @Test
    @DisplayName("supprimerAchat() — lève exception si achat non trouvé")
    void supprimerAchat_leveExceptionSiAbsent() {
        when(achatRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> achatService.supprimerAchat("999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Achat non trouvé avec l'ID : 999");
    }

    @Test
    @DisplayName("supprimerAchat() — lève SecurityException si tenant différent")
    void supprimerAchat_leveSecurityExceptionTenantDifferent() {
        TenantEntity autreTenant = new TenantEntity();
        autreTenant.setTenantUuid("uuid-autre");

        when(achatRepository.findById("test-id-1")).thenReturn(Optional.of(achatValide));
        when(tenantService.getCurrentTenant()).thenReturn(autreTenant);

        assertThatThrownBy(() -> achatService.supprimerAchat("test-id-1"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Accès refusé");
    }

    // =========================================================
    // obtenirAchatsParUtilisateur
    // =========================================================

    @Test
    @DisplayName("obtenirAchatsParUtilisateur() — retourne les achats de l'utilisateur")
    void obtenirAchatsParUtilisateur_retourneListe() {
        when(achatRepository.findByUtilisateur(utilisateurTest)).thenReturn(Arrays.asList(achatValide));

        List<AchatEntity> resultat = achatService.obtenirAchatsParUtilisateur(utilisateurTest);

        assertThat(resultat).hasSize(1);
    }

    // =========================================================
    // obtenirAchatsPagines
    // =========================================================

    @Test
    @DisplayName("obtenirAchatsPagines() — retourne une page d'achats")
    void obtenirAchatsPagines_retournePage() {
        Page<AchatEntity> pageMock = new PageImpl<>(Collections.emptyList());
        when(achatRepository.findAllWithSearch(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(pageMock);

        var resultat = achatService.obtenirAchatsPagines(0, 10, null, null, null);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getContent()).isEmpty();
    }

    // =========================================================
    // obtenirAchatsParPeriode
    // =========================================================

    @Test
    @DisplayName("obtenirAchatsParPeriode() — retourne les achats entre deux dates")
    void obtenirAchatsParPeriode_retourneListe() {
        LocalDate debut = LocalDate.of(2025, 1, 1);
        LocalDate fin = LocalDate.of(2025, 12, 31);
        when(achatRepository.findByDateAchatBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Arrays.asList(achatValide));

        List<AchatEntity> resultat = achatService.obtenirAchatsParPeriode(debut, fin);

        assertThat(resultat).hasSize(1);
    }

    // =========================================================
    // calculerTotalAchats
    // =========================================================

    @Test
    @DisplayName("calculerTotalAchats() — somme correcte sur la période")
    void calculerTotalAchats_sommeCorrect() {
        LocalDate debut = LocalDate.of(2025, 1, 1);
        LocalDate fin = LocalDate.of(2025, 12, 31);
        AchatEntity achat2 = AchatEntity.builder()
                .prixTotal(new BigDecimal("1000.00"))
                .build();
        when(achatRepository.findByDateAchatBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(achatValide, achat2));

        BigDecimal total = achatService.calculerTotalAchats(debut, fin);

        assertThat(total).isEqualByComparingTo(new BigDecimal("3500.00")); // 2500 + 1000
    }

    @Test
    @DisplayName("calculerTotalAchats() — retourne zéro si aucun achat")
    void calculerTotalAchats_retourneZeroSiVide() {
        LocalDate debut = LocalDate.of(2025, 1, 1);
        LocalDate fin = LocalDate.of(2025, 12, 31);
        when(achatRepository.findByDateAchatBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        BigDecimal total = achatService.calculerTotalAchats(debut, fin);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================
    // obtenirProduitsAvecPrixVente
    // =========================================================

    @Test
    @DisplayName("obtenirProduitsAvecPrixVente() — retourne tous les achats")
    void obtenirProduitsAvecPrixVente_retourneListe() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.findAllByTenant(tenantTest)).thenReturn(Arrays.asList(achatValide));

        List<AchatEntity> resultat = achatService.obtenirProduitsAvecPrixVente();

        assertThat(resultat).hasSize(1);
        verify(achatRepository).findAllByTenant(tenantTest);
    }
}
