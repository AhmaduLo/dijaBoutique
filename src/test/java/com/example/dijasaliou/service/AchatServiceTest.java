package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.AchatEntity;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AchatService
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
@DisplayName("Tests du AchatService")
class AchatServiceTest {

    // Mock du repository des achats
    @Mock
    private AchatRepository achatRepository;

    // Service à tester avec les mocks injectés
    @InjectMocks
    private AchatService achatService;

    // Données de test
    private AchatEntity achatValide;
    private UserEntity utilisateurTest;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    /**
     * Initialisation des données de test avant chaque test
     *
     * Scénario :
     * - Achat de 5 ordinateurs à 500€ = 2500€ total
     * - Fournisseur : Dell
     * - Période de test : année 2024
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

        // Achat valide : 5 ordinateurs à 500€
        achatValide = AchatEntity.builder()
                .id(1L)
                .nomProduit("Ordinateur portable")
                .quantite(5)
                .prixUnitaire(new BigDecimal("500.00"))
                .prixTotal(new BigDecimal("2500.00"))
                .fournisseur("Dell")
                .dateAchat(LocalDate.now())
                .utilisateur(utilisateurTest)
                .build();

        // Périodes de test
        dateDebut = LocalDate.of(2024, 1, 1);
        dateFin = LocalDate.of(2024, 12, 31);
    }

    // ==================== Tests pour obtenirTousLesAchats() ====================

    /**
     * Test : Obtenir tous les achats du système
     *
     * Scénario :
     * - 2 achats dans le système (Ordinateur et Souris)
     * - Le service doit retourner les 2 achats
     */
    @Test
    @DisplayName("obtenirTousLesAchats() - Devrait retourner tous les achats")
    void obtenirTousLesAchats_DevraitRetournerTousLesAchats() {
        // Arrange : Préparer 2 achats
        AchatEntity achat2 = AchatEntity.builder()
                .id(2L)
                .nomProduit("Souris")
                .quantite(10)
                .prixUnitaire(new BigDecimal("15.00"))
                .build();

        List<AchatEntity> achatsAttendus = Arrays.asList(achatValide, achat2);
        when(achatRepository.findAll()).thenReturn(achatsAttendus);

        // Act : Récupérer tous les achats
        List<AchatEntity> resultat = achatService.obtenirTousLesAchats();

        // Assert : Vérifier qu'on a bien 2 achats
        assertThat(resultat)
                .isNotNull()
                .hasSize(2)
                .containsExactly(achatValide, achat2);
        verify(achatRepository, times(1)).findAll();
    }

    /**
     * Test : Liste vide si aucun achat
     */
    @Test
    @DisplayName("obtenirTousLesAchats() - Devrait retourner une liste vide si aucun achat")
    void obtenirTousLesAchats_DevraitRetournerListeVide() {
        // Arrange : Aucun achat
        when(achatRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<AchatEntity> resultat = achatService.obtenirTousLesAchats();

        // Assert : Liste vide
        assertThat(resultat).isEmpty();
        verify(achatRepository, times(1)).findAll();
    }

    // ==================== Tests pour obtenirAchatParId() ====================

    /**
     * Test : Rechercher un achat par son ID
     */
    @Test
    @DisplayName("obtenirAchatParId() - Devrait retourner un achat par son ID")
    void obtenirAchatParId_DevraitRetournerAchat() {
        // Arrange
        Long achatId = 1L;
        when(achatRepository.findById(achatId)).thenReturn(Optional.of(achatValide));

        // Act
        AchatEntity resultat = achatService.obtenirAchatParId(achatId);

        // Assert
        assertThat(resultat).isNotNull();
        assertThat(resultat.getId()).isEqualTo(achatId);
        assertThat(resultat.getNomProduit()).isEqualTo("Ordinateur portable");
        verify(achatRepository, times(1)).findById(achatId);
    }

    @Test
    @DisplayName("obtenirAchatParId() - Devrait lancer une exception si achat non trouvé")
    void obtenirAchatParId_DevraitLancerExceptionSiNonTrouve() {
        // Arrange
        Long achatId = 999L;
        when(achatRepository.findById(achatId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> achatService.obtenirAchatParId(achatId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Achat non trouvé avec l'ID : 999");
        verify(achatRepository, times(1)).findById(achatId);
    }

    // ==================== Tests pour creerAchat() ====================

    @Test
    @DisplayName("creerAchat() - Devrait créer un achat valide")
    void creerAchat_DevraitCreerAchatValide() {
        // Arrange
        AchatEntity nouveauAchat = AchatEntity.builder()
                .nomProduit("Clavier")
                .quantite(3)
                .prixUnitaire(new BigDecimal("50.00"))
                .fournisseur("Logitech")
                .dateAchat(LocalDate.now())
                .build();

        when(achatRepository.save(any(AchatEntity.class))).thenReturn(nouveauAchat);

        // Act
        AchatEntity resultat = achatService.creerAchat(nouveauAchat, utilisateurTest);

        // Assert
        assertThat(resultat).isNotNull();
        assertThat(resultat.getUtilisateur()).isEqualTo(utilisateurTest);
        assertThat(resultat.getPrixTotal()).isNotNull();
        verify(achatRepository, times(1)).save(any(AchatEntity.class));
    }

    @Test
    @DisplayName("creerAchat() - Devrait calculer le prix total si non fourni")
    void creerAchat_DevraitCalculerPrixTotal() {
        // Arrange
        AchatEntity achatSansPrixTotal = AchatEntity.builder()
                .nomProduit("Moniteur")
                .quantite(2)
                .prixUnitaire(new BigDecimal("300.00"))
                .fournisseur("Samsung")
                .dateAchat(LocalDate.now())
                .build();

        when(achatRepository.save(any(AchatEntity.class))).thenReturn(achatSansPrixTotal);

        // Act
        AchatEntity resultat = achatService.creerAchat(achatSansPrixTotal, utilisateurTest);

        // Assert
        assertThat(resultat).isNotNull();
        verify(achatRepository, times(1)).save(any(AchatEntity.class));
    }

    @Test
    @DisplayName("creerAchat() - Devrait lancer une exception si quantité invalide")
    void creerAchat_DevraitLancerExceptionSiQuantiteInvalide() {
        // Arrange
        AchatEntity achatInvalide = AchatEntity.builder()
                .nomProduit("Produit")
                .quantite(0)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> achatService.creerAchat(achatInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La quantité doit être supérieure à 0");
        verify(achatRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerAchat() - Devrait lancer une exception si quantité nulle")
    void creerAchat_DevraitLancerExceptionSiQuantiteNulle() {
        // Arrange
        AchatEntity achatInvalide = AchatEntity.builder()
                .nomProduit("Produit")
                .quantite(null)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> achatService.creerAchat(achatInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La quantité doit être supérieure à 0");
        verify(achatRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerAchat() - Devrait lancer une exception si prix unitaire invalide")
    void creerAchat_DevraitLancerExceptionSiPrixUnitaireInvalide() {
        // Arrange
        AchatEntity achatInvalide = AchatEntity.builder()
                .nomProduit("Produit")
                .quantite(5)
                .prixUnitaire(BigDecimal.ZERO)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> achatService.creerAchat(achatInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le prix unitaire doit être supérieur à 0");
        verify(achatRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerAchat() - Devrait lancer une exception si prix unitaire négatif")
    void creerAchat_DevraitLancerExceptionSiPrixUnitaireNegatif() {
        // Arrange
        AchatEntity achatInvalide = AchatEntity.builder()
                .nomProduit("Produit")
                .quantite(5)
                .prixUnitaire(new BigDecimal("-10.00"))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> achatService.creerAchat(achatInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le prix unitaire doit être supérieur à 0");
        verify(achatRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerAchat() - Devrait lancer une exception si nom produit vide")
    void creerAchat_DevraitLancerExceptionSiNomProduitVide() {
        // Arrange
        AchatEntity achatInvalide = AchatEntity.builder()
                .nomProduit("")
                .quantite(5)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> achatService.creerAchat(achatInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le nom du produit est obligatoire");
        verify(achatRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerAchat() - Devrait lancer une exception si nom produit null")
    void creerAchat_DevraitLancerExceptionSiNomProduitNull() {
        // Arrange
        AchatEntity achatInvalide = AchatEntity.builder()
                .nomProduit(null)
                .quantite(5)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> achatService.creerAchat(achatInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le nom du produit est obligatoire");
        verify(achatRepository, never()).save(any());
    }

    // ==================== Tests pour modifierAchat() ====================

    @Test
    @DisplayName("modifierAchat() - Devrait modifier un achat existant")
    void modifierAchat_DevraitModifierAchat() {
        // Arrange
        Long achatId = 1L;
        AchatEntity achatModifie = AchatEntity.builder()
                .nomProduit("Ordinateur portable Dell XPS")
                .quantite(10)
                .prixUnitaire(new BigDecimal("600.00"))
                .fournisseur("Dell Premium")
                .dateAchat(LocalDate.now())
                .utilisateur(utilisateurTest)
                .build();

        when(achatRepository.findById(achatId)).thenReturn(Optional.of(achatValide));
        when(achatRepository.save(any(AchatEntity.class))).thenReturn(achatValide);

        // Act
        AchatEntity resultat = achatService.modifierAchat(achatId, achatModifie);

        // Assert
        assertThat(resultat).isNotNull();
        verify(achatRepository, times(1)).findById(achatId);
        verify(achatRepository, times(1)).save(any(AchatEntity.class));
    }

    @Test
    @DisplayName("modifierAchat() - Devrait lancer une exception si achat non trouvé")
    void modifierAchat_DevraitLancerExceptionSiNonTrouve() {
        // Arrange
        Long achatId = 999L;
        AchatEntity achatModifie = AchatEntity.builder()
                .nomProduit("Produit")
                .quantite(5)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        when(achatRepository.findById(achatId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> achatService.modifierAchat(achatId, achatModifie))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Achat non trouvé avec l'ID : 999");
        verify(achatRepository, times(1)).findById(achatId);
        verify(achatRepository, never()).save(any());
    }

    @Test
    @DisplayName("modifierAchat() - Devrait lancer une exception si données invalides")
    void modifierAchat_DevraitLancerExceptionSiDonneesInvalides() {
        // Arrange
        Long achatId = 1L;
        AchatEntity achatInvalide = AchatEntity.builder()
                .nomProduit("Produit")
                .quantite(0)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        when(achatRepository.findById(achatId)).thenReturn(Optional.of(achatValide));

        // Act & Assert
        assertThatThrownBy(() -> achatService.modifierAchat(achatId, achatInvalide))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La quantité doit être supérieure à 0");
        verify(achatRepository, times(1)).findById(achatId);
        verify(achatRepository, never()).save(any());
    }

    // ==================== Tests pour supprimerAchat() ====================

    @Test
    @DisplayName("supprimerAchat() - Devrait supprimer un achat existant")
    void supprimerAchat_DevraitSupprimerAchat() {
        // Arrange
        Long achatId = 1L;
        when(achatRepository.existsById(achatId)).thenReturn(true);
        doNothing().when(achatRepository).deleteById(achatId);

        // Act
        achatService.supprimerAchat(achatId);

        // Assert
        verify(achatRepository, times(1)).existsById(achatId);
        verify(achatRepository, times(1)).deleteById(achatId);
    }

    @Test
    @DisplayName("supprimerAchat() - Devrait lancer une exception si achat non trouvé")
    void supprimerAchat_DevraitLancerExceptionSiNonTrouve() {
        // Arrange
        Long achatId = 999L;
        when(achatRepository.existsById(achatId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> achatService.supprimerAchat(achatId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Achat non trouvé avec l'ID : 999");
        verify(achatRepository, times(1)).existsById(achatId);
        verify(achatRepository, never()).deleteById(any());
    }

    // ==================== Tests pour obtenirAchatsParUtilisateur() ====================

    @Test
    @DisplayName("obtenirAchatsParUtilisateur() - Devrait retourner les achats d'un utilisateur")
    void obtenirAchatsParUtilisateur_DevraitRetournerAchatsUtilisateur() {
        // Arrange
        List<AchatEntity> achatsAttendus = Arrays.asList(achatValide);
        when(achatRepository.findByUtilisateur(utilisateurTest)).thenReturn(achatsAttendus);

        // Act
        List<AchatEntity> resultat = achatService.obtenirAchatsParUtilisateur(utilisateurTest);

        // Assert
        assertThat(resultat)
                .isNotNull()
                .hasSize(1)
                .containsExactly(achatValide);
        verify(achatRepository, times(1)).findByUtilisateur(utilisateurTest);
    }

    @Test
    @DisplayName("obtenirAchatsParUtilisateur() - Devrait retourner liste vide si aucun achat")
    void obtenirAchatsParUtilisateur_DevraitRetournerListeVide() {
        // Arrange
        when(achatRepository.findByUtilisateur(utilisateurTest)).thenReturn(Arrays.asList());

        // Act
        List<AchatEntity> resultat = achatService.obtenirAchatsParUtilisateur(utilisateurTest);

        // Assert
        assertThat(resultat).isEmpty();
        verify(achatRepository, times(1)).findByUtilisateur(utilisateurTest);
    }

    // ==================== Tests pour obtenirAchatsParPeriode() ====================

    @Test
    @DisplayName("obtenirAchatsParPeriode() - Devrait retourner les achats d'une période")
    void obtenirAchatsParPeriode_DevraitRetournerAchatsPeriode() {
        // Arrange
        List<AchatEntity> achatsAttendus = Arrays.asList(achatValide);
        when(achatRepository.findByDateAchatBetween(dateDebut, dateFin)).thenReturn(achatsAttendus);

        // Act
        List<AchatEntity> resultat = achatService.obtenirAchatsParPeriode(dateDebut, dateFin);

        // Assert
        assertThat(resultat)
                .isNotNull()
                .hasSize(1)
                .containsExactly(achatValide);
        verify(achatRepository, times(1)).findByDateAchatBetween(dateDebut, dateFin);
    }

    @Test
    @DisplayName("obtenirAchatsParPeriode() - Devrait retourner liste vide si aucun achat")
    void obtenirAchatsParPeriode_DevraitRetournerListeVide() {
        // Arrange
        when(achatRepository.findByDateAchatBetween(dateDebut, dateFin)).thenReturn(Arrays.asList());

        // Act
        List<AchatEntity> resultat = achatService.obtenirAchatsParPeriode(dateDebut, dateFin);

        // Assert
        assertThat(resultat).isEmpty();
        verify(achatRepository, times(1)).findByDateAchatBetween(dateDebut, dateFin);
    }

    // ==================== Tests pour calculerTotalAchats() ====================

    @Test
    @DisplayName("calculerTotalAchats() - Devrait calculer le total des achats sur une période")
    void calculerTotalAchats_DevraitCalculerTotal() {
        // Arrange
        AchatEntity achat2 = AchatEntity.builder()
                .id(2L)
                .nomProduit("Souris")
                .quantite(10)
                .prixUnitaire(new BigDecimal("15.00"))
                .prixTotal(new BigDecimal("150.00"))
                .build();

        List<AchatEntity> achats = Arrays.asList(achatValide, achat2);
        when(achatRepository.findByDateAchatBetween(dateDebut, dateFin)).thenReturn(achats);

        // Act
        BigDecimal total = achatService.calculerTotalAchats(dateDebut, dateFin);

        // Assert
        assertThat(total).isEqualTo(new BigDecimal("2650.00"));
        verify(achatRepository, times(1)).findByDateAchatBetween(dateDebut, dateFin);
    }

    @Test
    @DisplayName("calculerTotalAchats() - Devrait retourner zéro si aucun achat")
    void calculerTotalAchats_DevraitRetournerZero() {
        // Arrange
        when(achatRepository.findByDateAchatBetween(dateDebut, dateFin)).thenReturn(Arrays.asList());

        // Act
        BigDecimal total = achatService.calculerTotalAchats(dateDebut, dateFin);

        // Assert
        assertThat(total).isEqualTo(BigDecimal.ZERO);
        verify(achatRepository, times(1)).findByDateAchatBetween(dateDebut, dateFin);
    }

    @Test
    @DisplayName("calculerTotalAchats() - Devrait calculer correctement avec un seul achat")
    void calculerTotalAchats_DevraitCalculerAvecUnSeulAchat() {
        // Arrange
        List<AchatEntity> achats = Arrays.asList(achatValide);
        when(achatRepository.findByDateAchatBetween(dateDebut, dateFin)).thenReturn(achats);

        // Act
        BigDecimal total = achatService.calculerTotalAchats(dateDebut, dateFin);

        // Assert
        assertThat(total).isEqualTo(new BigDecimal("2500.00"));
        verify(achatRepository, times(1)).findByDateAchatBetween(dateDebut, dateFin);
    }
}
