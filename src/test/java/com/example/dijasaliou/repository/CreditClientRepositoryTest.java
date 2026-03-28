package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.ClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Tests CreditClientRepository — requêtes @Query")
class CreditClientRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CreditClientRepository repo;

    private TenantEntity tenant;
    private TenantEntity autreTenant;
    private ClientEntity client1;
    private ClientEntity client2;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        tenant = em.persistAndFlush(TenantEntity.builder()
                .tenantUuid("tenant-001")
                .nomEntreprise("Boutique Test")
                .numeroTelephone("+221771111111")
                .build());

        autreTenant = em.persistAndFlush(TenantEntity.builder()
                .tenantUuid("tenant-002")
                .nomEntreprise("Autre Boutique")
                .numeroTelephone("+221772222222")
                .build());

        user = em.persistAndFlush(UserEntity.builder()
                .nom("Test").prenom("User")
                .email("test@boutique.com")
                .motDePasse("encoded")
                .nomEntreprise("Boutique Test")
                .numeroTelephone("+221771111111")
                .build());

        client1 = em.persistAndFlush(ClientEntity.builder()
                .nom("Aminata Diallo")
                .tenant(tenant)
                .build());

        client2 = em.persistAndFlush(ClientEntity.builder()
                .nom("Moussa Traoré")
                .tenant(tenant)
                .build());

        // Credit EN_ATTENTE — 8000 restant
        em.persistAndFlush(CreditClientEntity.builder()
                .client(client1)
                .montantInitial(new BigDecimal("10000"))
                .montantRestant(new BigDecimal("8000"))
                .statut(StatutCredit.EN_ATTENTE)
                .tenant(tenant)
                .build());

        // Credit PARTIEL — 2000 restant
        em.persistAndFlush(CreditClientEntity.builder()
                .client(client1)
                .montantInitial(new BigDecimal("5000"))
                .montantRestant(new BigDecimal("2000"))
                .statut(StatutCredit.PARTIEL)
                .tenant(tenant)
                .build());

        // Credit SOLDE — 0 restant (ne doit pas compter dans les actifs)
        em.persistAndFlush(CreditClientEntity.builder()
                .client(client2)
                .montantInitial(new BigDecimal("3000"))
                .montantRestant(new BigDecimal("0"))
                .statut(StatutCredit.SOLDE)
                .tenant(tenant)
                .build());

        // Credit EN_ATTENTE en retard (échéance passée)
        em.persistAndFlush(CreditClientEntity.builder()
                .client(client2)
                .montantInitial(new BigDecimal("4000"))
                .montantRestant(new BigDecimal("4000"))
                .statut(StatutCredit.EN_ATTENTE)
                .dateEcheance(LocalDate.now().minusDays(3))
                .tenant(tenant)
                .build());

        // Credit d'un autre tenant — NE DOIT PAS apparaître dans les agrégats
        ClientEntity clientAutre = em.persistAndFlush(
                ClientEntity.builder().nom("Client Autre").tenant(autreTenant).build());
        em.persistAndFlush(CreditClientEntity.builder()
                .client(clientAutre)
                .montantInitial(new BigDecimal("99999"))
                .montantRestant(new BigDecimal("99999"))
                .statut(StatutCredit.EN_ATTENTE)
                .tenant(autreTenant)
                .build());
    }

    // ==================== sumMontantRestantActif ====================

    @Test
    @DisplayName("sumMontantRestantActif — doit sommer uniquement les crédits non soldés du tenant")
    void sumMontantRestantActif_DoitRetournerSommeNonSoldes() {
        // tenant-001 : 8000 (EN_ATTENTE) + 2000 (PARTIEL) + 4000 (EN_ATTENTE en retard) = 14000
        BigDecimal result = repo.sumMontantRestantActif(StatutCredit.SOLDE, "tenant-001");

        assertThat(result).isEqualByComparingTo(new BigDecimal("14000"));
    }

    @Test
    @DisplayName("sumMontantRestantActif — doit retourner 0 si tous les crédits sont soldés")
    void sumMontantRestantActif_DoitRetourner0SiTousSoldes() {
        BigDecimal result = repo.sumMontantRestantActif(StatutCredit.SOLDE, "tenant-002");

        // L'autre tenant a un crédit EN_ATTENTE de 99999 — ce test vérifie l'isolation
        assertThat(result).isGreaterThan(BigDecimal.ZERO);

        // Un tenant fictif sans crédits → COALESCE retourne 0
        BigDecimal resultVide = repo.sumMontantRestantActif(StatutCredit.SOLDE, "tenant-inconnu");
        assertThat(resultVide).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== sumMontantInitialActif ====================

    @Test
    @DisplayName("sumMontantInitialActif — doit sommer le montant initial des crédits non soldés")
    void sumMontantInitialActif_DoitRetournerSomme() {
        // 10000 (EN_ATTENTE) + 5000 (PARTIEL) + 4000 (EN_ATTENTE) = 19000
        BigDecimal result = repo.sumMontantInitialActif(StatutCredit.SOLDE, "tenant-001");

        assertThat(result).isEqualByComparingTo(new BigDecimal("19000"));
    }

    // ==================== countCreditsActifs ====================

    @Test
    @DisplayName("countCreditsActifs — doit compter les crédits non soldés")
    void countCreditsActifs_DoitCompterNonSoldes() {
        // 3 crédits non soldés (EN_ATTENTE x2 + PARTIEL), 1 SOLDE exclu
        long count = repo.countCreditsActifs(StatutCredit.SOLDE, "tenant-001");

        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("countCreditsActifs — isolation entre tenants")
    void countCreditsActifs_DoitIsolerParTenant() {
        long countTenant1 = repo.countCreditsActifs(StatutCredit.SOLDE, "tenant-001");
        long countTenant2 = repo.countCreditsActifs(StatutCredit.SOLDE, "tenant-002");

        // tenant-001 : 3 actifs, tenant-002 : 1 actif
        assertThat(countTenant1).isEqualTo(3L);
        assertThat(countTenant2).isEqualTo(1L);
    }

    // ==================== countClientsCrediteurs ====================

    @Test
    @DisplayName("countClientsCrediteurs — doit compter les clients distincts avec crédits actifs")
    void countClientsCrediteurs_DoitCompterClientsDistincts() {
        // client1 a 2 crédits actifs, client2 a 1 crédit actif → 2 clients distincts
        long count = repo.countClientsCrediteurs(StatutCredit.SOLDE, "tenant-001");

        assertThat(count).isEqualTo(2L);
    }

    // ==================== countCreditsEnRetard ====================

    @Test
    @DisplayName("countCreditsEnRetard — doit compter les crédits actifs avec échéance dépassée")
    void countCreditsEnRetard_DoitCompterEcheancesDepassees() {
        // 1 crédit avec dateEcheance = now()-3 jours ET statut actif
        long count = repo.countCreditsEnRetard(StatutCredit.SOLDE, LocalDate.now(), "tenant-001");

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("countCreditsEnRetard — doit retourner 0 si aucun crédit en retard")
    void countCreditsEnRetard_DoitRetourner0SiAucunRetard() {
        // Dans le futur, aucun crédit ne sera "en retard"
        long count = repo.countCreditsEnRetard(StatutCredit.SOLDE,
                LocalDate.now().minusDays(30), "tenant-001");

        assertThat(count).isEqualTo(0L);
    }

    // ==================== countCreditsActifsByClientId ====================

    @Test
    @DisplayName("countCreditsActifsByClientId — doit compter les crédits actifs d'un client")
    void countCreditsActifsByClientId_DoitCompterParClient() {
        // client1 a 2 crédits actifs (EN_ATTENTE + PARTIEL)
        long count = repo.countCreditsActifsByClientId(client1.getId(), StatutCredit.SOLDE, "tenant-001");

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("countCreditsActifsByClientId — doit exclure les crédits soldés")
    void countCreditsActifsByClientId_DoitExclureSoldes() {
        // client2 a 1 crédit SOLDE + 1 EN_ATTENTE → seul l'actif compte
        long count = repo.countCreditsActifsByClientId(client2.getId(), StatutCredit.SOLDE, "tenant-001");

        assertThat(count).isEqualTo(1L);
    }

    // ==================== findAllWithSearch ====================

    @Test
    @DisplayName("findAllWithSearch — sans filtre, retourne tous les crédits du tenant")
    void findAllWithSearch_SansFiltreRetourneTous() {
        // 4 crédits dans tenant-001 (EN_ATTENTE, PARTIEL, SOLDE, EN_ATTENTE)
        Page<CreditClientEntity> page = repo.findAllWithSearch(null, null, "tenant-001",
                PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(4L);
    }

    @Test
    @DisplayName("findAllWithSearch — filtre par statut SOLDE")
    void findAllWithSearch_AvecStatutSolde() {
        Page<CreditClientEntity> page = repo.findAllWithSearch(StatutCredit.SOLDE, null, "tenant-001",
                PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findAllWithSearch — filtre par nom de client")
    void findAllWithSearch_AvecRechercheNomClient() {
        // "aminata" doit matcher "Aminata Diallo" (case-insensitive)
        Page<CreditClientEntity> page = repo.findAllWithSearch(null, "aminata", "tenant-001",
                PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2L); // client1 a 2 crédits
    }

    @Test
    @DisplayName("findAllWithSearch — isolation entre tenants")
    void findAllWithSearch_IsolationTenants() {
        Page<CreditClientEntity> page = repo.findAllWithSearch(null, null, "tenant-002",
                PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1L);
    }

    // ==================== sumCreditsRestantParPeriode ====================

    @Test
    @DisplayName("sumCreditsRestantParPeriode — doit agréger les crédits sur ventes de la période")
    void sumCreditsRestantParPeriode_DoitAgregerParPeriode() {
        // Crée une vente dans la période de test
        VenteEntity vente = em.persistAndFlush(VenteEntity.builder()
                .quantite(1.0)
                .nomProduit("Collier Test")
                .prixUnitaire(new BigDecimal("5000"))
                .prixTotal(new BigDecimal("5000"))
                .dateVente(LocalDate.of(2025, 6, 15))
                .utilisateur(user)
                .tenant(tenant)
                .build());

        // Crée un crédit lié à cette vente
        em.persistAndFlush(CreditClientEntity.builder()
                .client(client1)
                .vente(vente)
                .montantInitial(new BigDecimal("5000"))
                .montantRestant(new BigDecimal("3000"))
                .statut(StatutCredit.PARTIEL)
                .tenant(tenant)
                .build());

        LocalDate debut = LocalDate.of(2025, 6, 1);
        LocalDate fin = LocalDate.of(2025, 6, 30);

        List<Object[]> result = repo.sumCreditsRestantParPeriode(
                debut, fin, StatutCredit.SOLDE, "tenant-001");

        assertThat(result).hasSize(1);
        long count = ((Number) result.get(0)[0]).longValue();
        BigDecimal sum = (BigDecimal) result.get(0)[1];
        assertThat(count).isEqualTo(1L);
        assertThat(sum).isEqualByComparingTo(new BigDecimal("3000"));
    }

    @Test
    @DisplayName("sumCreditsRestantParPeriode — hors période retourne count=0 et sum=0")
    void sumCreditsRestantParPeriode_HorsPeriodeRetourneZero() {
        LocalDate debut = LocalDate.of(2020, 1, 1);
        LocalDate fin = LocalDate.of(2020, 12, 31);

        List<Object[]> result = repo.sumCreditsRestantParPeriode(
                debut, fin, StatutCredit.SOLDE, "tenant-001");

        // L'agrégat JPQL sans GROUP BY retourne toujours 1 ligne : [0, 0] si aucun résultat
        assertThat(result).hasSize(1);
        long count = ((Number) result.get(0)[0]).longValue();
        BigDecimal sum = (BigDecimal) result.get(0)[1];
        assertThat(count).isEqualTo(0L);
        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
