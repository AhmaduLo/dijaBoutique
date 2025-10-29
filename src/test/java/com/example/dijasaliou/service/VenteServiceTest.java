package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du VenteService")
class VenteServiceTest {

    @Mock
    private VenteRepository venteRepository;

    @Mock
    private StockService stockService;

    @InjectMocks
    private VenteService venteService;

    private VenteEntity venteValide;
    private UserEntity utilisateurTest;

    @BeforeEach
    void setUp() {
        utilisateurTest = UserEntity.builder()
                .id(1L)
                .nom("Diop")
                .email("amadou@example.com")
                .build();

        venteValide = VenteEntity.builder()
                .id(1L)
                .nomProduit("Ordinateur")
                .quantite(2)
                .prixUnitaire(new BigDecimal("500.00"))
                .prixTotal(new BigDecimal("1000.00"))
                .client("Client A")
                .dateVente(LocalDate.now())
                .utilisateur(utilisateurTest)
                .build();
    }

    @Test
    @DisplayName("obtenirToutesLesVentes() - Devrait retourner toutes les ventes")
    void obtenirToutesLesVentes_DevraitRetourner() {
        when(venteRepository.findAll()).thenReturn(Arrays.asList(venteValide));

        List<VenteEntity> resultat = venteService.obtenirToutesLesVentes();

        assertThat(resultat).hasSize(1);
        verify(venteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("obtenirVenteParId() - Devrait retourner une vente")
    void obtenirVenteParId_DevraitRetourner() {
        when(venteRepository.findById(1L)).thenReturn(Optional.of(venteValide));

        VenteEntity resultat = venteService.obtenirVenteParId(1L);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("obtenirVenteParId() - Devrait lancer exception si non trouvée")
    void obtenirVenteParId_DevraitLancerException() {
        when(venteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venteService.obtenirVenteParId(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Vente non trouvée");
    }

    @Test
    @DisplayName("creerVente() - Devrait créer une vente avec stock suffisant")
    void creerVente_DevraitCreerAvecStockSuffisant() {
        StockDto stock = StockDto.builder()
                .nomProduit("Ordinateur")
                .stockDisponible(10)
                .build();

        when(stockService.obtenirStockParNomProduit("Ordinateur")).thenReturn(stock);
        when(venteRepository.save(any(VenteEntity.class))).thenReturn(venteValide);

        VenteEntity resultat = venteService.creerVente(venteValide, utilisateurTest);

        assertThat(resultat).isNotNull();
        verify(venteRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("creerVente() - Devrait lancer exception si stock insuffisant")
    void creerVente_DevraitLancerExceptionStockInsuffisant() {
        StockDto stock = StockDto.builder()
                .nomProduit("Ordinateur")
                .stockDisponible(1)
                .build();

        when(stockService.obtenirStockParNomProduit("Ordinateur")).thenReturn(stock);

        assertThatThrownBy(() -> venteService.creerVente(venteValide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuffisant");
    }

    @Test
    @DisplayName("creerVente() - Devrait lancer exception si quantité invalide")
    void creerVente_DevraitLancerExceptionQuantiteInvalide() {
        VenteEntity venteInvalide = VenteEntity.builder()
                .nomProduit("Produit")
                .quantite(0)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> venteService.creerVente(venteInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantité");
    }

    @Test
    @DisplayName("creerVente() - Devrait lancer exception si prix invalide")
    void creerVente_DevraitLancerExceptionPrixInvalide() {
        VenteEntity venteInvalide = VenteEntity.builder()
                .nomProduit("Produit")
                .quantite(5)
                .prixUnitaire(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> venteService.creerVente(venteInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prix");
    }

    @Test
    @DisplayName("creerVente() - Devrait lancer exception si nom produit vide")
    void creerVente_DevraitLancerExceptionNomProduitVide() {
        VenteEntity venteInvalide = VenteEntity.builder()
                .nomProduit("")
                .quantite(5)
                .prixUnitaire(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> venteService.creerVente(venteInvalide, utilisateurTest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nom du produit");
    }

    @Test
    @DisplayName("supprimerVente() - Devrait supprimer une vente")
    void supprimerVente_DevraitSupprimer() {
        when(venteRepository.existsById(1L)).thenReturn(true);

        venteService.supprimerVente(1L);

        verify(venteRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("supprimerVente() - Devrait lancer exception si non trouvée")
    void supprimerVente_DevraitLancerException() {
        when(venteRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> venteService.supprimerVente(999L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("obtenirVentesParUtilisateur() - Devrait retourner les ventes")
    void obtenirVentesParUtilisateur_DevraitRetourner() {
        when(venteRepository.findByUtilisateur(utilisateurTest))
                .thenReturn(Arrays.asList(venteValide));

        List<VenteEntity> resultat = venteService.obtenirVentesParUtilisateur(utilisateurTest);

        assertThat(resultat).hasSize(1);
    }

    @Test
    @DisplayName("obtenirVentesParPeriode() - Devrait retourner les ventes d'une période")
    void obtenirVentesParPeriode_DevraitRetourner() {
        LocalDate debut = LocalDate.of(2024, 1, 1);
        LocalDate fin = LocalDate.of(2024, 12, 31);

        when(venteRepository.findByDateVenteBetween(debut, fin))
                .thenReturn(Arrays.asList(venteValide));

        List<VenteEntity> resultat = venteService.obtenirVentesParPeriode(debut, fin);

        assertThat(resultat).hasSize(1);
    }

    @Test
    @DisplayName("calculerChiffreAffaires() - Devrait calculer le CA")
    void calculerChiffreAffaires_DevraitCalculer() {
        LocalDate debut = LocalDate.of(2024, 1, 1);
        LocalDate fin = LocalDate.of(2024, 12, 31);

        VenteEntity vente2 = VenteEntity.builder()
                .prixTotal(new BigDecimal("500.00"))
                .build();

        when(venteRepository.findByDateVenteBetween(debut, fin))
                .thenReturn(Arrays.asList(venteValide, vente2));

        BigDecimal ca = venteService.calculerChiffreAffaires(debut, fin);

        assertThat(ca).isEqualTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("calculerChiffreAffaires() - Devrait retourner zéro si aucune vente")
    void calculerChiffreAffaires_DevraitRetournerZero() {
        LocalDate debut = LocalDate.of(2024, 1, 1);
        LocalDate fin = LocalDate.of(2024, 12, 31);

        when(venteRepository.findByDateVenteBetween(debut, fin))
                .thenReturn(Arrays.asList());

        BigDecimal ca = venteService.calculerChiffreAffaires(debut, fin);

        assertThat(ca).isEqualTo(BigDecimal.ZERO);
    }
}
