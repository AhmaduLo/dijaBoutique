package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.CreditClientDto;
import com.example.dijasaliou.dto.PaiementCreditDto;
import com.example.dijasaliou.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
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
        client.setId("test-id-10");
        client.setNom("Client Test");
        client.setDetteTotale(BigDecimal.ZERO);
        client.setTenant(tenantTest);

        vente = VenteEntity.builder()
                .nomProduit("Téléphone")
                .prixTotal(new BigDecimal("200.00"))
                .modePaiement(VenteEntity.ModePaiementVente.CREDIT)
                .tenant(tenantTest)
                .build();
        vente.setId("test-id-5");

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
        credit.setId("test-id-20");
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
        when(creditClientRepository.findByIdForUpdate("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creditClientService.enregistrerPaiement(
                "999", new BigDecimal("100.00"), ModePaiement.ESPECES, null, employe))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Crédit introuvable");
    }

    @Test
    @DisplayName("enregistrerPaiement() — lève exception si crédit déjà soldé")
    void enregistrerPaiement_leveExceptionDejasSolde() {
        credit.setStatut(StatutCredit.SOLDE);
        when(creditClientRepository.findByIdForUpdate("test-id-20")).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        assertThatThrownBy(() -> creditClientService.enregistrerPaiement(
                "test-id-20", new BigDecimal("100.00"), ModePaiement.ESPECES, null, employe))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà soldé");
    }

    @Test
    @DisplayName("enregistrerPaiement() — lève exception si montant = 0")
    void enregistrerPaiement_leveExceptionMontantZero() {
        when(creditClientRepository.findByIdForUpdate("test-id-20")).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        assertThatThrownBy(() -> creditClientService.enregistrerPaiement(
                "test-id-20", BigDecimal.ZERO, ModePaiement.ESPECES, null, employe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montant doit être positif");
    }

    @Test
    @DisplayName("enregistrerPaiement() — lève exception si montant dépasse le restant dû")
    void enregistrerPaiement_leveExceptionMontantTropEleve() {
        when(creditClientRepository.findByIdForUpdate("test-id-20")).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        assertThatThrownBy(() -> creditClientService.enregistrerPaiement(
                "test-id-20", new BigDecimal("999.00"), ModePaiement.ESPECES, null, employe))
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
        when(creditClientRepository.findByIdForUpdate("test-id-20")).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(paiementCreditRepository.save(any())).thenReturn(new PaiementCreditEntity());
        when(creditClientRepository.save(any())).thenReturn(credit);
        when(clientRepository.save(any())).thenReturn(client);

        CreditClientDto resultat = creditClientService.enregistrerPaiement(
                "test-id-20", new BigDecimal("100.00"), ModePaiement.ESPECES, "acompte", employe);

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
        when(creditClientRepository.findByIdForUpdate("test-id-20")).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(paiementCreditRepository.save(any())).thenReturn(new PaiementCreditEntity());
        when(venteRepository.save(any())).thenReturn(vente);
        when(creditClientRepository.save(any())).thenReturn(credit);
        when(clientRepository.save(any())).thenReturn(client);

        CreditClientDto resultat = creditClientService.enregistrerPaiement(
                "test-id-20", new BigDecimal("200.00"), ModePaiement.ESPECES, null, employe);

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

        when(creditClientRepository.findByVenteId("test-id-5")).thenReturn(List.of(credit));
        when(clientRepository.save(any())).thenReturn(client);

        creditClientService.supprimerCreditsDeLaVente("test-id-5");

        verify(creditClientRepository).delete(credit);
        assertThat(client.getDetteTotale()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("supprimerCreditsDeLaVente() — ne modifie pas la dette si crédit déjà soldé")
    void supprimerCreditsDeLaVente_neTouchePasDetteCredinSolde() {
        client.setDetteTotale(BigDecimal.ZERO);
        credit.setStatut(StatutCredit.SOLDE);

        when(creditClientRepository.findByVenteId("test-id-5")).thenReturn(List.of(credit));

        creditClientService.supprimerCreditsDeLaVente("test-id-5");

        verify(creditClientRepository).delete(credit);
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("supprimerCreditsDeLaVente() — ne fait rien si aucun crédit lié")
    void supprimerCreditsDeLaVente_sansCredits() {
        when(creditClientRepository.findByVenteId("99")).thenReturn(Collections.emptyList());

        creditClientService.supprimerCreditsDeLaVente("99");

        verify(creditClientRepository, never()).delete(any(CreditClientEntity.class));
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
        when(creditClientRepository.sumMontantInitialTous("uuid-tenant-test"))
                .thenReturn(new BigDecimal("1000.00"));
        when(creditClientRepository.countCreditsActifs(StatutCredit.SOLDE, "uuid-tenant-test"))
                .thenReturn(3L);
        when(creditClientRepository.countClientsCrediteurs(StatutCredit.SOLDE, "uuid-tenant-test"))
                .thenReturn(2L);
        when(tenantService.todayInTenantTz()).thenReturn(LocalDate.now());
        when(creditClientRepository.countCreditsEnRetard(eq(StatutCredit.SOLDE), any(LocalDate.class), eq("uuid-tenant-test")))
                .thenReturn(1L);

        var stats = creditClientService.obtenirStats(null);

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
        when(creditClientRepository.sumMontantInitialTous(any())).thenReturn(null);
        when(creditClientRepository.countCreditsActifs(any(), any())).thenReturn(0L);
        when(creditClientRepository.countClientsCrediteurs(any(), any())).thenReturn(0L);
        when(creditClientRepository.countCreditsEnRetard(any(), any(), any())).thenReturn(0L);

        var stats = creditClientService.obtenirStats(null);

        assertThat((double) stats.get("tauxRecouvrement")).isEqualTo(0.0);
    }

    // =========================================================
    // obtenirCredits
    // =========================================================

    @Test
    @DisplayName("obtenirCredits() — retourne une page de crédits du tenant")
    void obtenirCredits_retournePage() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(creditClientRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(Collections.emptyList());

        var resultat = creditClientService.obtenirCredits(0, 10, null, "TOUS", null, null, null);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getContent()).isEmpty();
    }

    @Test
    @DisplayName("obtenirCredits() — filtre par statut EN_ATTENTE")
    void obtenirCredits_filtreParStatut() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(creditClientRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(Collections.emptyList());

        var resultat = creditClientService.obtenirCredits(0, 10, null, "EN_ATTENTE", null, null, null);

        assertThat(resultat).isNotNull();
        verify(creditClientRepository).findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class));
    }

    // =========================================================
    // obtenirPaiements
    // =========================================================

    @Test
    @DisplayName("obtenirPaiements() — retourne la liste des paiements d'un crédit")
    void obtenirPaiements_retourneListe() {
        when(creditClientRepository.findById("test-id-20")).thenReturn(Optional.of(credit));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(paiementCreditRepository.findByCreditOrderByCreatedDateDesc(credit))
                .thenReturn(Collections.emptyList());

        List<PaiementCreditDto> resultat = creditClientService.obtenirPaiements("test-id-20");

        assertThat(resultat).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("obtenirPaiements() — lève exception si crédit introuvable")
    void obtenirPaiements_leveExceptionCreditIntrouvable() {
        when(creditClientRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creditClientService.obtenirPaiements("999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Crédit introuvable");
    }

    // =========================================================
    // obtenirHistoriqueClient
    // =========================================================

    @Test
    @DisplayName("obtenirHistoriqueClient() — retourne les crédits d'un client")
    void obtenirHistoriqueClient_retourneListe() {
        when(clientRepository.findById("test-id-10")).thenReturn(Optional.of(client));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(creditClientRepository.findByClientOrderByCreatedDateAsc(client))
                .thenReturn(Collections.emptyList());

        List<CreditClientDto> resultat = creditClientService.obtenirHistoriqueClient("test-id-10");

        assertThat(resultat).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("obtenirHistoriqueClient() — lève exception si client introuvable")
    void obtenirHistoriqueClient_leveExceptionClientIntrouvable() {
        when(clientRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creditClientService.obtenirHistoriqueClient("999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Client introuvable");
    }

    // =========================================================
    // obtenirCredits — une vente à crédit = une seule carte, dans un seul onglet
    // =========================================================

    /** Crédit d'un article d'une vente (groupeVenteId commun à tous les articles de la vente). */
    private CreditClientEntity creditArticle(String id, String groupeVenteId, String initial, String restant,
                                             StatutCredit statut) {
        VenteEntity v = VenteEntity.builder()
                .nomProduit("article-" + id)
                .prixTotal(new BigDecimal(initial))
                .modePaiement(VenteEntity.ModePaiementVente.CREDIT)
                .groupeVenteId(groupeVenteId)
                .tenant(tenantTest)
                .build();
        v.setId("vente-" + id);
        CreditClientEntity c = CreditClientEntity.builder()
                .client(client).vente(v)
                .montantInitial(new BigDecimal(initial))
                .montantRestant(new BigDecimal(restant))
                .statut(statut)
                .employe(employe).employeNom("Amadou Diop")
                .tenant(tenantTest)
                .build();
        c.setId(id);
        return c;
    }

    /** Cas réel : vente Palla de 3 articles (cesar soldé, arachides partiel, bracelet en attente). */
    private List<CreditClientEntity> ventePallaPayeeEnPartie() {
        return List.of(
                creditArticle("cesar", "vente-palla", "600", "0", StatutCredit.SOLDE),
                creditArticle("arachides", "vente-palla", "90000", "89200", StatutCredit.PARTIEL),
                creditArticle("bracelet", "vente-palla", "1800", "1800", StatutCredit.EN_ATTENTE));
    }

    @Test
    @DisplayName("obtenirCredits() — une vente payée en partie n'apparaît que dans PARTIEL, avec tous ses articles")
    @SuppressWarnings("unchecked")
    void obtenirCredits_venteMultiArticlesPayeeEnPartie_uniquementDansPartiel() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(creditClientRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(ventePallaPayeeEnPartie());

        var partiel = creditClientService.obtenirCredits(0, 20, null, "PARTIEL", null, null, null);
        assertThat(partiel.getTotalElements()).isEqualTo(1);
        assertThat(partiel.getContent()).hasSize(3)
                .extracting(CreditClientDto::getGroupeCreditId)
                .containsOnly("G:vente-palla");

        assertThat(creditClientService.obtenirCredits(0, 20, null, "EN_ATTENTE", null, null, null).getContent()).isEmpty();
        assertThat(creditClientService.obtenirCredits(0, 20, null, "SOLDE", null, null, null).getContent()).isEmpty();
    }

    @Test
    @DisplayName("obtenirCredits() — deux ventes du même client = deux cartes ; pagination par vente")
    @SuppressWarnings("unchecked")
    void obtenirCredits_deuxVentesMemeClient_paginationParVente() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(creditClientRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(
                        creditArticle("a1", "vente-lundi", "1000", "1000", StatutCredit.EN_ATTENTE),
                        creditArticle("a2", "vente-lundi", "2000", "2000", StatutCredit.EN_ATTENTE),
                        creditArticle("b1", "vente-mardi", "500", "500", StatutCredit.EN_ATTENTE)));

        var page0 = creditClientService.obtenirCredits(0, 1, null, "EN_ATTENTE", null, null, null);
        assertThat(page0.getTotalElements()).isEqualTo(2);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.getContent()).extracting(CreditClientDto::getId).containsExactly("a1", "a2");

        var page1 = creditClientService.obtenirCredits(1, 1, null, "EN_ATTENTE", null, null, null);
        assertThat(page1.getContent()).extracting(CreditClientDto::getId).containsExactly("b1");
        assertThat(page1.isLast()).isTrue();
    }

    @Test
    @DisplayName("statutGroupe() — rien payé / partiel / soldé / perte, calculé sur la vente entière")
    void statutGroupe_calculeSurLaVenteEntiere() {
        assertThat(CreditClientService.statutGroupe(List.of(
                creditArticle("1", "g", "100", "100", StatutCredit.EN_ATTENTE),
                creditArticle("2", "g", "200", "200", StatutCredit.EN_ATTENTE))))
                .isEqualTo(StatutCredit.EN_ATTENTE);

        assertThat(CreditClientService.statutGroupe(ventePallaPayeeEnPartie()))
                .isEqualTo(StatutCredit.PARTIEL);

        assertThat(CreditClientService.statutGroupe(List.of(
                creditArticle("1", "g", "100", "0", StatutCredit.SOLDE),
                creditArticle("2", "g", "200", "0", StatutCredit.SOLDE))))
                .isEqualTo(StatutCredit.SOLDE);

        // Article payé + article passé en perte : la vente est close en perte, pas "entièrement remboursée"
        assertThat(CreditClientService.statutGroupe(List.of(
                creditArticle("1", "g", "100", "0", StatutCredit.SOLDE),
                creditArticle("2", "g", "200", "200", StatutCredit.PERTE))))
                .isEqualTo(StatutCredit.PERTE);

        // Article en perte mais un autre reste à payer : la vente reste active
        assertThat(CreditClientService.statutGroupe(List.of(
                creditArticle("1", "g", "100", "100", StatutCredit.EN_ATTENTE),
                creditArticle("2", "g", "200", "200", StatutCredit.PERTE))))
                .isEqualTo(StatutCredit.EN_ATTENTE);
    }

    @Test
    @DisplayName("cleGroupe() — vente ancienne sans groupeVenteId : regroupée par client + date de vente")
    void cleGroupe_venteAncienneSansGroupeVenteId() {
        CreditClientEntity c1 = creditArticle("1", null, "100", "100", StatutCredit.EN_ATTENTE);
        CreditClientEntity c2 = creditArticle("2", null, "200", "200", StatutCredit.EN_ATTENTE);
        java.time.LocalDateTime date = java.time.LocalDateTime.of(2026, 1, 15, 10, 0);
        c1.getVente().setDateVente(date);
        c2.getVente().setDateVente(date);

        assertThat(CreditClientDto.cleGroupe(c1)).isEqualTo(CreditClientDto.cleGroupe(c2));

        c2.getVente().setDateVente(date.plusDays(1));
        assertThat(CreditClientDto.cleGroupe(c1)).isNotEqualTo(CreditClientDto.cleGroupe(c2));
    }
}
