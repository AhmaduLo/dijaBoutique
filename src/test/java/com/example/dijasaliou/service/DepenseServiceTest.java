package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.DepenseEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.DepenseRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — DepenseService")
class DepenseServiceTest {

    @Mock private DepenseRepository depenseRepository;
    @Mock private TenantService tenantService;

    @InjectMocks
    private DepenseService depenseService;

    private DepenseEntity depenseValide;
    private UserEntity utilisateurTest;
    private TenantEntity tenantTest;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    @BeforeEach
    void setUp() {
        tenantTest = new TenantEntity();
        tenantTest.setTenantUuid("uuid-tenant-test");

        utilisateurTest = UserEntity.builder()
                .id(1L).nom("Diop").prenom("Amadou")
                .email("amadou@example.com").role(UserEntity.Role.USER)
                .build();

        depenseValide = DepenseEntity.builder()
                .id(1L)
                .libelle("Loyer bureau")
                .montant(new BigDecimal("500.00"))
                .dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.LOYER)
                .estRecurrente(true)
                .notes("Paiement mensuel")
                .utilisateur(utilisateurTest)
                .tenant(tenantTest)
                .build();

        dateDebut = LocalDate.of(2025, 1, 1);
        dateFin = LocalDate.of(2025, 12, 31);
    }

    // =========================================================
    // obtenirToutesLesDepenses
    // =========================================================

    @Test
    @DisplayName("obtenirToutesLesDepenses() — retourne toutes les dépenses")
    void obtenirToutesLesDepenses_retourneListe() {
        DepenseEntity d2 = DepenseEntity.builder().id(2L).libelle("Electricité")
                .montant(new BigDecimal("150.00")).categorie(DepenseEntity.CategorieDepense.ELECTRICITE).build();
        when(depenseRepository.findAll()).thenReturn(Arrays.asList(depenseValide, d2));

        List<DepenseEntity> resultat = depenseService.obtenirToutesLesDepenses();

        assertThat(resultat).hasSize(2);
        verify(depenseRepository).findAll();
    }

    @Test
    @DisplayName("obtenirToutesLesDepenses() — liste vide si aucune dépense")
    void obtenirToutesLesDepenses_retourneListeVide() {
        when(depenseRepository.findAll()).thenReturn(Collections.emptyList());

        assertThat(depenseService.obtenirToutesLesDepenses()).isEmpty();
    }

    // =========================================================
    // obtenirDepenseParId
    // =========================================================

    @Test
    @DisplayName("obtenirDepenseParId() — retourne la dépense si trouvée")
    void obtenirDepenseParId_retourneDepense() {
        when(depenseRepository.findById(1L)).thenReturn(Optional.of(depenseValide));

        DepenseEntity resultat = depenseService.obtenirDepenseParId(1L);

        assertThat(resultat.getId()).isEqualTo(1L);
        assertThat(resultat.getLibelle()).isEqualTo("Loyer bureau");
    }

    @Test
    @DisplayName("obtenirDepenseParId() — lève exception si non trouvée")
    void obtenirDepenseParId_leveException() {
        when(depenseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> depenseService.obtenirDepenseParId(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dépense non trouvée avec l'ID : 999");
    }

    // =========================================================
    // creerDepense — validations
    // =========================================================

    @Test
    @DisplayName("creerDepense() — lève exception si libellé vide")
    void creerDepense_leveExceptionLibelleVide() {
        DepenseEntity d = DepenseEntity.builder().libelle("").montant(new BigDecimal("100.00"))
                .categorie(DepenseEntity.CategorieDepense.AUTRE).build();

        assertThatThrownBy(() -> depenseService.creerDepense(d, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le libellé est obligatoire");
        verify(depenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerDepense() — lève exception si montant = 0")
    void creerDepense_leveExceptionMontantZero() {
        DepenseEntity d = DepenseEntity.builder().libelle("Test").montant(BigDecimal.ZERO)
                .categorie(DepenseEntity.CategorieDepense.AUTRE).build();

        assertThatThrownBy(() -> depenseService.creerDepense(d, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le montant doit être supérieur à 0");
    }

    @Test
    @DisplayName("creerDepense() — lève exception si catégorie nulle")
    void creerDepense_leveExceptionCategorieNull() {
        DepenseEntity d = DepenseEntity.builder().libelle("Test")
                .montant(new BigDecimal("100.00")).categorie(null).build();

        assertThatThrownBy(() -> depenseService.creerDepense(d, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La catégorie est obligatoire");
    }

    // =========================================================
    // creerDepense — succès
    // =========================================================

    @Test
    @DisplayName("creerDepense() — crée la dépense et assigne l'utilisateur + tenant")
    void creerDepense_succes() {
        DepenseEntity nouvelle = DepenseEntity.builder().libelle("Eau")
                .montant(new BigDecimal("50.00")).dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.EAU).build();

        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(depenseRepository.save(any())).thenReturn(nouvelle);

        DepenseEntity resultat = depenseService.creerDepense(nouvelle, utilisateurTest);

        assertThat(resultat).isNotNull();
        assertThat(nouvelle.getUtilisateur()).isEqualTo(utilisateurTest);
        verify(depenseRepository).save(any());
    }

    // =========================================================
    // modifierDepense
    // =========================================================

    @Test
    @DisplayName("modifierDepense() — modifie une dépense du même tenant")
    void modifierDepense_succes() {
        DepenseEntity modifiee = DepenseEntity.builder().libelle("Loyer modifié")
                .montant(new BigDecimal("600.00")).dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.LOYER).utilisateur(utilisateurTest).build();

        when(depenseRepository.findById(1L)).thenReturn(Optional.of(depenseValide));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(depenseRepository.save(any())).thenReturn(depenseValide);

        DepenseEntity resultat = depenseService.modifierDepense(1L, modifiee);

        assertThat(resultat).isNotNull();
        verify(depenseRepository).save(any());
    }

    @Test
    @DisplayName("modifierDepense() — lève exception si dépense non trouvée")
    void modifierDepense_leveExceptionSiAbsente() {
        DepenseEntity modifiee = DepenseEntity.builder().libelle("Test")
                .montant(new BigDecimal("100.00")).categorie(DepenseEntity.CategorieDepense.AUTRE).build();
        when(depenseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> depenseService.modifierDepense(999L, modifiee))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dépense non trouvée avec l'ID : 999");
    }

    @Test
    @DisplayName("modifierDepense() — lève SecurityException si tenant différent")
    void modifierDepense_leveSecurityExceptionTenantDifferent() {
        TenantEntity autreTenant = new TenantEntity();
        autreTenant.setTenantUuid("uuid-autre");

        DepenseEntity modifiee = DepenseEntity.builder().libelle("Test")
                .montant(new BigDecimal("100.00")).categorie(DepenseEntity.CategorieDepense.AUTRE).build();

        when(depenseRepository.findById(1L)).thenReturn(Optional.of(depenseValide));
        when(tenantService.getCurrentTenant()).thenReturn(autreTenant);

        assertThatThrownBy(() -> depenseService.modifierDepense(1L, modifiee))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Accès refusé");
    }

    // =========================================================
    // supprimerDepense
    // =========================================================

    @Test
    @DisplayName("supprimerDepense() — supprime une dépense du même tenant")
    void supprimerDepense_succes() {
        when(depenseRepository.findById(1L)).thenReturn(Optional.of(depenseValide));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        depenseService.supprimerDepense(1L);

        verify(depenseRepository).deleteById(1L);
    }

    @Test
    @DisplayName("supprimerDepense() — lève exception si non trouvée")
    void supprimerDepense_leveExceptionSiAbsente() {
        when(depenseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> depenseService.supprimerDepense(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dépense non trouvée avec l'ID : 999");
        verify(depenseRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("supprimerDepense() — lève SecurityException si tenant différent")
    void supprimerDepense_leveSecurityExceptionTenantDifferent() {
        TenantEntity autreTenant = new TenantEntity();
        autreTenant.setTenantUuid("uuid-autre");

        when(depenseRepository.findById(1L)).thenReturn(Optional.of(depenseValide));
        when(tenantService.getCurrentTenant()).thenReturn(autreTenant);

        assertThatThrownBy(() -> depenseService.supprimerDepense(1L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Accès refusé");
    }

    // =========================================================
    // obtenirDepensesParUtilisateur / Periode / Categorie
    // =========================================================

    @Test
    @DisplayName("obtenirDepensesParUtilisateur() — retourne les dépenses de l'utilisateur")
    void obtenirDepensesParUtilisateur_retourneListe() {
        when(depenseRepository.findByUtilisateur(utilisateurTest)).thenReturn(Arrays.asList(depenseValide));

        List<DepenseEntity> resultat = depenseService.obtenirDepensesParUtilisateur(utilisateurTest);

        assertThat(resultat).hasSize(1).containsExactly(depenseValide);
    }

    @Test
    @DisplayName("obtenirDepensesParPeriode() — retourne les dépenses de la période")
    void obtenirDepensesParPeriode_retourneListe() {
        when(depenseRepository.findByDateDepenseBetween(dateDebut, dateFin)).thenReturn(Arrays.asList(depenseValide));

        List<DepenseEntity> resultat = depenseService.obtenirDepensesParPeriode(dateDebut, dateFin);

        assertThat(resultat).hasSize(1);
    }

    @Test
    @DisplayName("obtenirDepensesParCategorie() — retourne les dépenses de la catégorie LOYER")
    void obtenirDepensesParCategorie_retourneListe() {
        when(depenseRepository.findByCategorie(DepenseEntity.CategorieDepense.LOYER))
                .thenReturn(Arrays.asList(depenseValide));

        List<DepenseEntity> resultat = depenseService.obtenirDepensesParCategorie(DepenseEntity.CategorieDepense.LOYER);

        assertThat(resultat).hasSize(1).containsExactly(depenseValide);
    }

    // =========================================================
    // calculerTotalDepenses
    // =========================================================

    @Test
    @DisplayName("calculerTotalDepenses() — somme correcte (500 + 150 = 650)")
    void calculerTotalDepenses_sommeCorrect() {
        DepenseEntity d2 = DepenseEntity.builder().montant(new BigDecimal("150.00")).build();
        when(depenseRepository.findByDateDepenseBetween(dateDebut, dateFin))
                .thenReturn(Arrays.asList(depenseValide, d2));

        BigDecimal total = depenseService.calculerTotalDepenses(dateDebut, dateFin);

        assertThat(total).isEqualByComparingTo(new BigDecimal("650.00"));
    }

    @Test
    @DisplayName("calculerTotalDepenses() — retourne zéro si aucune dépense")
    void calculerTotalDepenses_retourneZero() {
        when(depenseRepository.findByDateDepenseBetween(dateDebut, dateFin))
                .thenReturn(Collections.emptyList());

        assertThat(depenseService.calculerTotalDepenses(dateDebut, dateFin))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================
    // obtenirDepensesPaginees
    // =========================================================

    @Test
    @DisplayName("obtenirDepensesPaginees() — retourne une page de dépenses")
    void obtenirDepensesPaginees_retournePage() {
        Page<DepenseEntity> pageMock = new PageImpl<>(Collections.emptyList());
        when(depenseRepository.findAllWithSearch(any(), any(), any(Pageable.class)))
                .thenReturn(pageMock);

        var resultat = depenseService.obtenirDepensesPaginees(0, 10, null, null);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getContent()).isEmpty();
    }

    @Test
    @DisplayName("obtenirDepensesPaginees() — filtre par catégorie valide")
    void obtenirDepensesPaginees_filtreParCategorie() {
        Page<DepenseEntity> pageMock = new PageImpl<>(Arrays.asList(depenseValide));
        when(depenseRepository.findAllWithSearch(any(), any(), any(Pageable.class)))
                .thenReturn(pageMock);

        var resultat = depenseService.obtenirDepensesPaginees(0, 10, null, "LOYER");

        assertThat(resultat).isNotNull();
        assertThat(resultat.getContent()).hasSize(1);
    }
}
