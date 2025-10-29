package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.DepenseEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.DepenseRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour DepenseService
 *
 * Bonnes pratiques appliquées :
 * - @ExtendWith(MockitoExtension.class) : Active Mockito pour les tests
 * - @Mock : Créer des mocks des dépendances
 * - @InjectMocks : Injecter les mocks dans le service testé
 * - AssertJ : Assertions fluides et lisibles
 * - Arrange-Act-Assert : Structure AAA dans chaque test
 * - @DisplayName : Descriptions claires en français
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du DepenseService")
class DepenseServiceTest {

    // Mock du repository des dépenses
    @Mock
    private DepenseRepository depenseRepository;

    // Service à tester avec les mocks injectés
    @InjectMocks
    private DepenseService depenseService;

    // Données de test
    private DepenseEntity depenseValide;
    private UserEntity utilisateurTest;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    /**
     * Initialisation des données de test avant chaque test
     *
     * Scénario :
     * - Dépense de loyer bureau : 500€
     * - Catégorie : LOYER (récurrente)
     * - Période de test : année 2024
     * - Notes : Paiement mensuel
     */
    @BeforeEach
    void setUp() {
        // Utilisateur de test
        utilisateurTest = UserEntity.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Amadou")
                .email("amadou@example.com")
                .role(UserEntity.Role.USER)
                .build();

        // Dépense valide : Loyer bureau 500€
        depenseValide = DepenseEntity.builder()
                .id(1L)
                .libelle("Loyer bureau")
                .montant(new BigDecimal("500.00"))
                .dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.LOYER)
                .estRecurrente(true)
                .notes("Paiement mensuel")
                .utilisateur(utilisateurTest)
                .build();

        // Périodes de test
        dateDebut = LocalDate.of(2024, 1, 1);
        dateFin = LocalDate.of(2024, 12, 31);
    }

    // ==================== Tests pour obtenirToutesLesDepenses() ====================

    /**
     * Test : Obtenir toutes les dépenses du système
     *
     * Scénario :
     * - 2 dépenses dans le système (Loyer 500€ et Électricité 150€)
     * - Le service doit retourner les 2 dépenses
     */
    @Test
    @DisplayName("obtenirToutesLesDepenses() - Devrait retourner toutes les dépenses")
    void obtenirToutesLesDepenses_DevraitRetournerToutesLesDepenses() {
        // Arrange : Préparer 2 dépenses
        DepenseEntity depense2 = DepenseEntity.builder()
                .id(2L)
                .libelle("Electricité")
                .montant(new BigDecimal("150.00"))
                .categorie(DepenseEntity.CategorieDepense.ELECTRICITE)
                .build();

        List<DepenseEntity> depensesAttendues = Arrays.asList(depenseValide, depense2);
        when(depenseRepository.findAll()).thenReturn(depensesAttendues);

        // Act : Récupérer toutes les dépenses
        List<DepenseEntity> resultat = depenseService.obtenirToutesLesDepenses();

        // Assert : Vérifier qu'on a bien 2 dépenses
        assertThat(resultat).hasSize(2).containsExactly(depenseValide, depense2);
        verify(depenseRepository, times(1)).findAll();
    }

    /**
     * Test : Retourner liste vide si aucune dépense
     */
    @Test
    @DisplayName("obtenirToutesLesDepenses() - Devrait retourner liste vide si aucune dépense")
    void obtenirToutesLesDepenses_DevraitRetournerListeVide() {
        // Arrange : Aucune dépense
        when(depenseRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<DepenseEntity> resultat = depenseService.obtenirToutesLesDepenses();

        // Assert : Liste vide
        assertThat(resultat).isEmpty();
        verify(depenseRepository, times(1)).findAll();
    }

    // ==================== Tests pour obtenirDepenseParId() ====================

    /**
     * Test : Rechercher une dépense par son ID
     */
    @Test
    @DisplayName("obtenirDepenseParId() - Devrait retourner une dépense par son ID")
    void obtenirDepenseParId_DevraitRetournerDepense() {
        // Arrange : ID de la dépense à rechercher
        Long depenseId = 1L;
        when(depenseRepository.findById(depenseId)).thenReturn(Optional.of(depenseValide));

        // Act : Rechercher la dépense
        DepenseEntity resultat = depenseService.obtenirDepenseParId(depenseId);

        // Assert : Vérifier que la dépense est trouvée
        assertThat(resultat).isNotNull();
        assertThat(resultat.getId()).isEqualTo(depenseId);
        assertThat(resultat.getLibelle()).isEqualTo("Loyer bureau");
        verify(depenseRepository, times(1)).findById(depenseId);
    }

    /**
     * Test : Lancer exception si dépense non trouvée
     */
    @Test
    @DisplayName("obtenirDepenseParId() - Devrait lancer exception si dépense non trouvée")
    void obtenirDepenseParId_DevraitLancerExceptionSiNonTrouvee() {
        // Arrange : ID inexistant
        Long depenseId = 999L;
        when(depenseRepository.findById(depenseId)).thenReturn(Optional.empty());

        // Act & Assert : Vérifier l'exception
        assertThatThrownBy(() -> depenseService.obtenirDepenseParId(depenseId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dépense non trouvée avec l'ID : 999");
        verify(depenseRepository, times(1)).findById(depenseId);
    }

    // ==================== Tests pour creerDepense() ====================

    /**
     * Test : Créer une dépense valide
     *
     * Scénario :
     * - Nouvelle dépense : Eau 50€
     * - La dépense doit être associée à l'utilisateur
     */
    @Test
    @DisplayName("creerDepense() - Devrait créer une dépense valide")
    void creerDepense_DevraitCreerDepenseValide() {
        // Arrange : Nouvelle dépense valide
        DepenseEntity nouvelleDepense = DepenseEntity.builder()
                .libelle("Eau")
                .montant(new BigDecimal("50.00"))
                .dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.EAU)
                .build();

        when(depenseRepository.save(any(DepenseEntity.class))).thenReturn(nouvelleDepense);

        // Act : Créer la dépense
        DepenseEntity resultat = depenseService.creerDepense(nouvelleDepense, utilisateurTest);

        // Assert : Vérifier la création
        assertThat(resultat).isNotNull();
        assertThat(resultat.getUtilisateur()).isEqualTo(utilisateurTest);
        verify(depenseRepository, times(1)).save(any(DepenseEntity.class));
    }

    /**
     * Test : Validation - Libellé obligatoire
     */
    @Test
    @DisplayName("creerDepense() - Devrait lancer exception si libellé vide")
    void creerDepense_DevraitLancerExceptionSiLibelleVide() {
        // Arrange : Dépense avec libellé vide
        DepenseEntity depenseInvalide = DepenseEntity.builder()
                .libelle("")
                .montant(new BigDecimal("100.00"))
                .categorie(DepenseEntity.CategorieDepense.AUTRE)
                .build();

        // Act & Assert : Vérifier l'exception
        assertThatThrownBy(() -> depenseService.creerDepense(depenseInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le libellé est obligatoire");
        verify(depenseRepository, never()).save(any());
    }

    /**
     * Test : Validation - Montant doit être > 0
     */
    @Test
    @DisplayName("creerDepense() - Devrait lancer exception si montant invalide")
    void creerDepense_DevraitLancerExceptionSiMontantInvalide() {
        // Arrange : Dépense avec montant zéro
        DepenseEntity depenseInvalide = DepenseEntity.builder()
                .libelle("Test")
                .montant(BigDecimal.ZERO)
                .categorie(DepenseEntity.CategorieDepense.AUTRE)
                .build();

        // Act & Assert : Vérifier l'exception
        assertThatThrownBy(() -> depenseService.creerDepense(depenseInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le montant doit être supérieur à 0");
        verify(depenseRepository, never()).save(any());
    }

    /**
     * Test : Validation - Catégorie obligatoire
     */
    @Test
    @DisplayName("creerDepense() - Devrait lancer exception si catégorie nulle")
    void creerDepense_DevraitLancerExceptionSiCategorieNulle() {
        // Arrange : Dépense sans catégorie
        DepenseEntity depenseInvalide = DepenseEntity.builder()
                .libelle("Test")
                .montant(new BigDecimal("100.00"))
                .categorie(null)
                .build();

        // Act & Assert : Vérifier l'exception
        assertThatThrownBy(() -> depenseService.creerDepense(depenseInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La catégorie est obligatoire");
        verify(depenseRepository, never()).save(any());
    }

    // ==================== Tests pour modifierDepense() ====================

    /**
     * Test : Modifier une dépense existante
     */
    @Test
    @DisplayName("modifierDepense() - Devrait modifier une dépense existante")
    void modifierDepense_DevraitModifierDepense() {
        // Arrange : Dépense modifiée (loyer passé de 500€ à 600€)
        Long depenseId = 1L;
        DepenseEntity depenseModifiee = DepenseEntity.builder()
                .libelle("Loyer bureau modifié")
                .montant(new BigDecimal("600.00"))
                .dateDepense(LocalDate.now())
                .categorie(DepenseEntity.CategorieDepense.LOYER)
                .estRecurrente(true)
                .utilisateur(utilisateurTest)
                .build();

        when(depenseRepository.findById(depenseId)).thenReturn(Optional.of(depenseValide));
        when(depenseRepository.save(any(DepenseEntity.class))).thenReturn(depenseValide);

        // Act : Modifier la dépense
        DepenseEntity resultat = depenseService.modifierDepense(depenseId, depenseModifiee);

        // Assert : Vérifier la modification
        assertThat(resultat).isNotNull();
        verify(depenseRepository, times(1)).findById(depenseId);
        verify(depenseRepository, times(1)).save(any(DepenseEntity.class));
    }

    /**
     * Test : Lancer exception si dépense à modifier n'existe pas
     */
    @Test
    @DisplayName("modifierDepense() - Devrait lancer exception si dépense non trouvée")
    void modifierDepense_DevraitLancerExceptionSiNonTrouvee() {
        // Arrange : ID inexistant
        Long depenseId = 999L;
        DepenseEntity depenseModifiee = DepenseEntity.builder()
                .libelle("Test")
                .montant(new BigDecimal("100.00"))
                .categorie(DepenseEntity.CategorieDepense.AUTRE)
                .build();

        when(depenseRepository.findById(depenseId)).thenReturn(Optional.empty());

        // Act & Assert : Vérifier l'exception
        assertThatThrownBy(() -> depenseService.modifierDepense(depenseId, depenseModifiee))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dépense non trouvée avec l'ID : 999");
        verify(depenseRepository, times(1)).findById(depenseId);
        verify(depenseRepository, never()).save(any());
    }

    // ==================== Tests pour supprimerDepense() ====================

    /**
     * Test : Supprimer une dépense existante
     */
    @Test
    @DisplayName("supprimerDepense() - Devrait supprimer une dépense existante")
    void supprimerDepense_DevraitSupprimerDepense() {
        // Arrange : Dépense existante
        Long depenseId = 1L;
        when(depenseRepository.existsById(depenseId)).thenReturn(true);
        doNothing().when(depenseRepository).deleteById(depenseId);

        // Act : Supprimer la dépense
        depenseService.supprimerDepense(depenseId);

        // Assert : Vérifier la suppression
        verify(depenseRepository, times(1)).existsById(depenseId);
        verify(depenseRepository, times(1)).deleteById(depenseId);
    }

    /**
     * Test : Lancer exception si dépense à supprimer n'existe pas
     */
    @Test
    @DisplayName("supprimerDepense() - Devrait lancer exception si dépense non trouvée")
    void supprimerDepense_DevraitLancerExceptionSiNonTrouvee() {
        // Arrange : ID inexistant
        Long depenseId = 999L;
        when(depenseRepository.existsById(depenseId)).thenReturn(false);

        // Act & Assert : Vérifier l'exception
        assertThatThrownBy(() -> depenseService.supprimerDepense(depenseId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dépense non trouvée avec l'ID : 999");
        verify(depenseRepository, times(1)).existsById(depenseId);
        verify(depenseRepository, never()).deleteById(any());
    }

    // ==================== Tests pour obtenirDepensesParUtilisateur() ====================

    /**
     * Test : Filtrer les dépenses par utilisateur
     */
    @Test
    @DisplayName("obtenirDepensesParUtilisateur() - Devrait retourner les dépenses d'un utilisateur")
    void obtenirDepensesParUtilisateur_DevraitRetournerDepensesUtilisateur() {
        // Arrange : Dépenses de l'utilisateur
        List<DepenseEntity> depensesAttendues = Arrays.asList(depenseValide);
        when(depenseRepository.findByUtilisateur(utilisateurTest)).thenReturn(depensesAttendues);

        // Act : Récupérer les dépenses de l'utilisateur
        List<DepenseEntity> resultat = depenseService.obtenirDepensesParUtilisateur(utilisateurTest);

        // Assert : Vérifier les dépenses de l'utilisateur
        assertThat(resultat).hasSize(1).containsExactly(depenseValide);
        verify(depenseRepository, times(1)).findByUtilisateur(utilisateurTest);
    }

    // ==================== Tests pour obtenirDepensesParPeriode() ====================

    /**
     * Test : Filtrer les dépenses par période
     */
    @Test
    @DisplayName("obtenirDepensesParPeriode() - Devrait retourner les dépenses d'une période")
    void obtenirDepensesParPeriode_DevraitRetournerDepensesPeriode() {
        // Arrange : Période 2024
        List<DepenseEntity> depensesAttendues = Arrays.asList(depenseValide);
        when(depenseRepository.findByDateDepenseBetween(dateDebut, dateFin)).thenReturn(depensesAttendues);

        // Act : Récupérer les dépenses de la période
        List<DepenseEntity> resultat = depenseService.obtenirDepensesParPeriode(dateDebut, dateFin);

        // Assert : Vérifier les dépenses de la période
        assertThat(resultat).hasSize(1).containsExactly(depenseValide);
        verify(depenseRepository, times(1)).findByDateDepenseBetween(dateDebut, dateFin);
    }

    // ==================== Tests pour calculerTotalDepenses() ====================

    /**
     * Test : Calculer le total des dépenses sur une période
     *
     * Scénario : 500€ (Loyer) + 150€ (Autre) = 650€
     */
    @Test
    @DisplayName("calculerTotalDepenses() - Devrait calculer le total des dépenses")
    void calculerTotalDepenses_DevraitCalculerTotal() {
        // Arrange : 2 dépenses (500€ + 150€)
        DepenseEntity depense2 = DepenseEntity.builder()
                .montant(new BigDecimal("150.00"))
                .build();

        List<DepenseEntity> depenses = Arrays.asList(depenseValide, depense2);
        when(depenseRepository.findByDateDepenseBetween(dateDebut, dateFin)).thenReturn(depenses);

        // Act : Calculer le total
        BigDecimal total = depenseService.calculerTotalDepenses(dateDebut, dateFin);

        // Assert : Vérifier le total (650€)
        assertThat(total).isEqualTo(new BigDecimal("650.00"));
        verify(depenseRepository, times(1)).findByDateDepenseBetween(dateDebut, dateFin);
    }

    /**
     * Test : Retourner zéro si aucune dépense
     */
    @Test
    @DisplayName("calculerTotalDepenses() - Devrait retourner zéro si aucune dépense")
    void calculerTotalDepenses_DevraitRetournerZero() {
        // Arrange : Aucune dépense
        when(depenseRepository.findByDateDepenseBetween(dateDebut, dateFin)).thenReturn(Arrays.asList());

        // Act : Calculer le total
        BigDecimal total = depenseService.calculerTotalDepenses(dateDebut, dateFin);

        // Assert : Vérifier zéro
        assertThat(total).isEqualTo(BigDecimal.ZERO);
        verify(depenseRepository, times(1)).findByDateDepenseBetween(dateDebut, dateFin);
    }

    // ==================== Tests pour obtenirDepensesParCategorie() ====================

    /**
     * Test : Filtrer les dépenses par catégorie
     */
    @Test
    @DisplayName("obtenirDepensesParCategorie() - Devrait retourner les dépenses par catégorie")
    void obtenirDepensesParCategorie_DevraitRetournerDepensesCategorie() {
        // Arrange : Catégorie LOYER
        List<DepenseEntity> depensesAttendues = Arrays.asList(depenseValide);
        when(depenseRepository.findByCategorie(DepenseEntity.CategorieDepense.LOYER))
                .thenReturn(depensesAttendues);

        // Act : Récupérer les dépenses de catégorie LOYER
        List<DepenseEntity> resultat = depenseService.obtenirDepensesParCategorie(
                DepenseEntity.CategorieDepense.LOYER);

        // Assert : Vérifier les dépenses de catégorie LOYER
        assertThat(resultat).hasSize(1).containsExactly(depenseValide);
        verify(depenseRepository, times(1)).findByCategorie(DepenseEntity.CategorieDepense.LOYER);
    }
}
