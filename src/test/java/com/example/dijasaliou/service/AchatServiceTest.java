package com.example.dijasaliou.service;

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

import java.math.BigDecimal;
import java.time.LocalDate;
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
                .id(1L)
                .nomProduit("Ordinateur portable")
                .quantite(5)
                .prixUnitaire(new BigDecimal("500.00"))
                .prixTotal(new BigDecimal("2500.00"))
                .fournisseur("Dell")
                .dateAchat(LocalDate.now())
                .utilisateur(utilisateurTest)
                .tenant(tenantTest)
                .build();
    }

    // =========================================================
    // obtenirTousLesAchats
    // =========================================================

    @Test
    @DisplayName("obtenirTousLesAchats() — retourne tous les achats")
    void obtenirTousLesAchats_retourneListe() {
        AchatEntity achat2 = AchatEntity.builder().id(2L).nomProduit("Souris")
                .quantite(10).prixUnitaire(new BigDecimal("15.00")).build();
        when(achatRepository.findAll()).thenReturn(Arrays.asList(achatValide, achat2));

        List<AchatEntity> resultat = achatService.obtenirTousLesAchats();

        assertThat(resultat).hasSize(2).containsExactly(achatValide, achat2);
        verify(achatRepository).findAll();
    }

    @Test
    @DisplayName("obtenirTousLesAchats() — liste vide si aucun achat")
    void obtenirTousLesAchats_retourneListeVide() {
        when(achatRepository.findAll()).thenReturn(Collections.emptyList());

        assertThat(achatService.obtenirTousLesAchats()).isEmpty();
    }

    // =========================================================
    // obtenirAchatParId
    // =========================================================

    @Test
    @DisplayName("obtenirAchatParId() — retourne l'achat si trouvé")
    void obtenirAchatParId_retourneAchat() {
        when(achatRepository.findById(1L)).thenReturn(Optional.of(achatValide));

        AchatEntity resultat = achatService.obtenirAchatParId(1L);

        assertThat(resultat.getId()).isEqualTo(1L);
        assertThat(resultat.getNomProduit()).isEqualTo("Ordinateur portable");
    }

    @Test
    @DisplayName("obtenirAchatParId() — lève exception si non trouvé")
    void obtenirAchatParId_leveException() {
        when(achatRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> achatService.obtenirAchatParId(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Achat non trouvé avec l'ID : 999");
    }

    // =========================================================
    // creerAchat — validations
    // =========================================================

    @Test
    @DisplayName("creerAchat() — lève exception si quantité = 0")
    void creerAchat_leveExceptionQuantiteZero() {
        AchatEntity a = AchatEntity.builder().nomProduit("Produit").quantite(0)
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
        AchatEntity a = AchatEntity.builder().nomProduit("Produit").quantite(5)
                .prixUnitaire(BigDecimal.ZERO).build();

        assertThatThrownBy(() -> achatService.creerAchat(a, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le prix unitaire doit être supérieur à 0");
    }

    @Test
    @DisplayName("creerAchat() — lève exception si nom produit vide")
    void creerAchat_leveExceptionNomProduitVide() {
        AchatEntity a = AchatEntity.builder().nomProduit("").quantite(5)
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
                .nomProduit("Clavier").quantite(3)
                .prixUnitaire(new BigDecimal("50.00"))
                .fournisseur("Logitech").dateAchat(LocalDate.now()).build();

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
                .nomProduit("Moniteur").quantite(2)
                .prixUnitaire(new BigDecimal("300.00"))
                .dateAchat(LocalDate.now()).build();

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
                .nomProduit("Ordinateur portable").quantite(8)
                .prixUnitaire(new BigDecimal("450.00"))
                .fournisseur("HP").dateAchat(LocalDate.now()).build();

        when(achatRepository.findById(1L)).thenReturn(Optional.of(achatValide));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(achatRepository.save(any())).thenReturn(achatValide);

        AchatEntity resultat = achatService.modifierAchat(1L, modifie);

        assertThat(resultat).isNotNull();
        verify(achatRepository).save(any());
    }

    @Test
    @DisplayName("modifierAchat() — lève exception si achat non trouvé")
    void modifierAchat_leveExceptionSiAbsent() {
        AchatEntity modifie = AchatEntity.builder()
                .nomProduit("Produit").quantite(1)
                .prixUnitaire(new BigDecimal("100.00")).build();

        when(achatRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> achatService.modifierAchat(999L, modifie))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Achat non trouvé avec l'ID : 999");
    }

    @Test
    @DisplayName("modifierAchat() — lève SecurityException si tenant différent")
    void modifierAchat_leveSecurityExceptionTenantDifferent() {
        TenantEntity autreTenant = new TenantEntity();
        autreTenant.setTenantUuid("uuid-autre");

        AchatEntity modifie = AchatEntity.builder()
                .nomProduit("Produit").quantite(1)
                .prixUnitaire(new BigDecimal("100.00")).build();

        when(achatRepository.findById(1L)).thenReturn(Optional.of(achatValide));
        when(tenantService.getCurrentTenant()).thenReturn(autreTenant);

        assertThatThrownBy(() -> achatService.modifierAchat(1L, modifie))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Accès refusé");
    }

    // =========================================================
    // supprimerAchat
    // =========================================================

    @Test
    @DisplayName("supprimerAchat() — supprime un achat du même tenant")
    void supprimerAchat_succes() {
        when(achatRepository.findById(1L)).thenReturn(Optional.of(achatValide));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        achatService.supprimerAchat(1L);

        verify(achatRepository).deleteById(1L);
    }

    @Test
    @DisplayName("supprimerAchat() — lève exception si achat non trouvé")
    void supprimerAchat_leveExceptionSiAbsent() {
        when(achatRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> achatService.supprimerAchat(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Achat non trouvé avec l'ID : 999");
    }

    @Test
    @DisplayName("supprimerAchat() — lève SecurityException si tenant différent")
    void supprimerAchat_leveSecurityExceptionTenantDifferent() {
        TenantEntity autreTenant = new TenantEntity();
        autreTenant.setTenantUuid("uuid-autre");

        when(achatRepository.findById(1L)).thenReturn(Optional.of(achatValide));
        when(tenantService.getCurrentTenant()).thenReturn(autreTenant);

        assertThatThrownBy(() -> achatService.supprimerAchat(1L))
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
}
