package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.CreditClientDto;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — CreditClientService")
class CreditClientServiceTest {

    @Mock private CreditClientRepository creditClientRepository;
    @Mock private PaiementCreditRepository paiementCreditRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private VenteRepository venteRepository;
    @Mock private TenantService tenantService;

    @InjectMocks
    private CreditClientService creditClientService;

    private TenantEntity tenantTest;
    private ClientEntity client;
    private UserEntity employe;
    private VenteEntity vente;
    private CreditClientEntity credit;

    @BeforeEach
    void setUp() {
        tenantTest = new TenantEntity();
        tenantTest.setTenantUuid("uuid-tenant-test");

        employe = UserEntity.builder()
                .id(1L).nom("Diop").prenom("Amadou")
                .email("employe@example.com").build();

        client = new ClientEntity();
        client.setId(10L);
        client.setNom("Client Test");
        client.setDetteTotale(BigDecimal.ZERO);
        client.setTenant(tenantTest);

        vente = VenteEntity.builder()
                .nomProduit("Téléphone")
                .prixTotal(new BigDecimal("200.00"))
                .modePaiement(VenteEntity.ModePaiementVente.CREDIT)
                .tenant(tenantTest)
                .build();
        vente.setId(5L);

        credit = CreditClientEntity.builder()
                .client(client)
                .vente(vente)
                .montantInitial(new BigDecimal("200.00"))
                .montantRestant(new BigDecimal("200.00"))
                .statut(StatutCredit.EN_ATTENTE)
                .employe(employe)
                .employeNom("Amadou Diop")
                .tenant(tenantTest)
                .build();
        credit.setId(20L);
    }

    // =========================================================
    // creerCreditDepuisVente
    // =========================================================

    @Test
    @DisplayName("creerCreditDepuisVente() — crée le crédit et augmente la dette du client")
    void creerCreditDepuisVente_creeCredit() {
        when(creditClientRepository.save(any())).thenReturn(credit);
        when(clientRepository.save(any())).thenReturn(client);

        CreditClientEntity resultat = creditClientService.creerCreditDepuisVente(
                vente, client, employe, LocalDate.now().plusDays(30));

        assertThat(resultat).isNotNull();
        assertThat(client.getDetteTotale()).isEqualByComparingTo(new BigDecimal("200.00"));
        verify(creditClientRepository).save(any());
        verify(clientRepository).save(client);
    }

    @Test
    @DisplayName("creerCreditDepuisVente() — le montant initial = montant restant = prixTotal vente")
    void creerCreditDepuisVente_montantsCorrects() {
        when(creditClientRepository.save(any(CreditClientEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.save(any())).thenReturn(client);

        CreditClientEntity resultat = creditClientService.creerCreditDepuisVente(
                vente, client, employe, null);

        assertThat(resultat.getMontantInitial()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(resultat.getMontantRestant()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(resultat.getStatut()).isEqualTo(StatutCredit.EN_ATTENTE);
    }

    // =========================================================
    // enregistrerPaiement — validations
    // =========================================================

    @Test
    @DisplayName("enregistrerPaiement() — lève exception si crédit non trouvé")
    void enregistrerPaiement_leveExceptionCreditIntrouvable() {
        when(creditClientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creditClientService.enregistrerPaiement(
                999L, new BigDecimal("100.00"), ModePaiement.ESPECES, null, employe))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Crédit introuvable");
    }

    @Test
    @DisplayName("enregistrerPaiement() — lève exception si crédit déjà soldé")
    void enregistrerPaiement_leveExceptionDejasSolde() {
        credit.setStatut(StatutCredit.SOLDE);
        when(creditClientRepository.findById(20L)).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        assertThatThrownBy(() -> creditClientService.enregistrerPaiement(
                20L, new BigDecimal("100.00"), ModePaiement.ESPECES, null, employe))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà soldé");
    }

    @Test
    @DisplayName("enregistrerPaiement() — lève exception si montant = 0")
    void enregistrerPaiement_leveExceptionMontantZero() {
        when(creditClientRepository.findById(20L)).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        assertThatThrownBy(() -> creditClientService.enregistrerPaiement(
                20L, BigDecimal.ZERO, ModePaiement.ESPECES, null, employe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montant doit être positif");
    }

    @Test
    @DisplayName("enregistrerPaiement() — lève exception si montant dépasse le restant dû")
    void enregistrerPaiement_leveExceptionMontantTropEleve() {
        when(creditClientRepository.findById(20L)).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        assertThatThrownBy(() -> creditClientService.enregistrerPaiement(
                20L, new BigDecimal("999.00"), ModePaiement.ESPECES, null, employe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dépasse le restant dû");
    }

    // =========================================================
    // enregistrerPaiement — paiement partiel
    // =========================================================

    @Test
    @DisplayName("enregistrerPaiement() — paiement partiel : statut devient PARTIEL")
    void enregistrerPaiement_partiel_statutPartiel() {
        credit.setMontantRestant(new BigDecimal("200.00"));
        when(creditClientRepository.findById(20L)).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(paiementCreditRepository.save(any())).thenReturn(new PaiementCreditEntity());
        when(creditClientRepository.save(any())).thenReturn(credit);
        when(clientRepository.save(any())).thenReturn(client);

        CreditClientDto resultat = creditClientService.enregistrerPaiement(
                20L, new BigDecimal("100.00"), ModePaiement.ESPECES, "acompte", employe);

        assertThat(resultat).isNotNull();
        assertThat(credit.getStatut()).isEqualTo(StatutCredit.PARTIEL);
        assertThat(credit.getMontantRestant()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    // =========================================================
    // enregistrerPaiement — paiement total
    // =========================================================

    @Test
    @DisplayName("enregistrerPaiement() — paiement total : statut devient SOLDE et vente mise à jour")
    void enregistrerPaiement_total_statutSolde() {
        credit.setMontantRestant(new BigDecimal("200.00"));
        client.setDetteTotale(new BigDecimal("200.00"));
        when(creditClientRepository.findById(20L)).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(paiementCreditRepository.save(any())).thenReturn(new PaiementCreditEntity());
        when(venteRepository.save(any())).thenReturn(vente);
        when(creditClientRepository.save(any())).thenReturn(credit);
        when(clientRepository.save(any())).thenReturn(client);

        CreditClientDto resultat = creditClientService.enregistrerPaiement(
                20L, new BigDecimal("200.00"), ModePaiement.ESPECES, null, employe);

        assertThat(resultat).isNotNull();
        assertThat(credit.getStatut()).isEqualTo(StatutCredit.SOLDE);
        assertThat(credit.getMontantRestant()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(client.getDetteTotale()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(venteRepository).save(vente);
    }

    // =========================================================
    // supprimerCreditsDeLaVente
    // =========================================================

    @Test
    @DisplayName("supprimerCreditsDeLaVente() — supprime les crédits non soldés et réduit la dette")
    void supprimerCreditsDeLaVente_supprimeCreditEtReduitDette() {
        client.setDetteTotale(new BigDecimal("200.00"));
        credit.setStatut(StatutCredit.EN_ATTENTE);
        credit.setMontantRestant(new BigDecimal("200.00"));

        when(creditClientRepository.findByVenteId(5L)).thenReturn(List.of(credit));
        when(clientRepository.save(any())).thenReturn(client);

        creditClientService.supprimerCreditsDeLaVente(5L);

        verify(creditClientRepository).delete(credit);
        assertThat(client.getDetteTotale()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("supprimerCreditsDeLaVente() — ne modifie pas la dette si crédit déjà soldé")
    void supprimerCreditsDeLaVente_neTouchePasDetteCredinSolde() {
        client.setDetteTotale(BigDecimal.ZERO);
        credit.setStatut(StatutCredit.SOLDE);

        when(creditClientRepository.findByVenteId(5L)).thenReturn(List.of(credit));

        creditClientService.supprimerCreditsDeLaVente(5L);

        verify(creditClientRepository).delete(credit);
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("supprimerCreditsDeLaVente() — ne fait rien si aucun crédit lié")
    void supprimerCreditsDeLaVente_sansCredits() {
        when(creditClientRepository.findByVenteId(99L)).thenReturn(Collections.emptyList());

        creditClientService.supprimerCreditsDeLaVente(99L);

        verify(creditClientRepository, never()).delete(any());
    }

    // =========================================================
    // obtenirStats
    // =========================================================

    @Test
    @DisplayName("obtenirStats() — retourne les clés statistiques attendues")
    void obtenirStats_retourneStats() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(creditClientRepository.sumMontantRestantActif(StatutCredit.SOLDE, "uuid-tenant-test"))
                .thenReturn(new BigDecimal("500.00"));
        when(creditClientRepository.sumMontantInitialActif(StatutCredit.SOLDE, "uuid-tenant-test"))
                .thenReturn(new BigDecimal("1000.00"));
        when(creditClientRepository.countCreditsActifs(StatutCredit.SOLDE, "uuid-tenant-test"))
                .thenReturn(3L);
        when(creditClientRepository.countClientsCrediteurs(StatutCredit.SOLDE, "uuid-tenant-test"))
                .thenReturn(2L);
        when(creditClientRepository.countCreditsEnRetard(eq(StatutCredit.SOLDE), any(LocalDate.class), eq("uuid-tenant-test")))
                .thenReturn(1L);

        var stats = creditClientService.obtenirStats();

        assertThat(stats).containsKeys("totalEnAttente", "montantTotalDu",
                "nombreCreditsActifs", "nombreClientsCrediteurs",
                "creditsEnRetard", "tauxRecouvrement");
        assertThat((double) stats.get("tauxRecouvrement")).isEqualTo(50.0);
    }

    @Test
    @DisplayName("obtenirStats() — taux recouvrement = 0 si aucun crédit initial")
    void obtenirStats_tauxZeroSiAucunCredit() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(creditClientRepository.sumMontantRestantActif(any(), any())).thenReturn(null);
        when(creditClientRepository.sumMontantInitialActif(any(), any())).thenReturn(null);
        when(creditClientRepository.countCreditsActifs(any(), any())).thenReturn(0L);
        when(creditClientRepository.countClientsCrediteurs(any(), any())).thenReturn(0L);
        when(creditClientRepository.countCreditsEnRetard(any(), any(), any())).thenReturn(0L);

        var stats = creditClientService.obtenirStats();

        assertThat((double) stats.get("tauxRecouvrement")).isEqualTo(0.0);
    }
}
