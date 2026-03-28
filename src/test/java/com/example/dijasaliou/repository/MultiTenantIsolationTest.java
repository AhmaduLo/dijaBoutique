package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.ClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'isolation multi-tenant via le filtre Hibernate "tenantFilter".
 *
 * Scénario : deux tenants (A et B) ont chacun des ventes et des crédits.
 * Le filtre Hibernate garantit qu'une requête faite dans le contexte du tenant A
 * ne peut jamais retourner les données du tenant B, et vice-versa.
 */
@DataJpaTest
@DisplayName("Tests d'isolation multi-tenant — filtre Hibernate tenantFilter")
class MultiTenantIsolationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private VenteRepository venteRepository;

    @Autowired
    private CreditClientRepository creditClientRepository;

    @Autowired
    private EntityManager entityManager;

    private TenantEntity tenantA;
    private TenantEntity tenantB;
    private UserEntity userA;

    @BeforeEach
    void setUp() {
        tenantA = em.persistAndFlush(TenantEntity.builder()
                .tenantUuid("tenant-iso-A")
                .nomEntreprise("Boutique Alpha")
                .numeroTelephone("+221771111111")
                .build());

        tenantB = em.persistAndFlush(TenantEntity.builder()
                .tenantUuid("tenant-iso-B")
                .nomEntreprise("Boutique Beta")
                .numeroTelephone("+221772222222")
                .build());

        userA = em.persistAndFlush(UserEntity.builder()
                .nom("Alpha").prenom("User")
                .email("user@alpha.com")
                .motDePasse("encoded")
                .nomEntreprise("Boutique Alpha")
                .numeroTelephone("+221771111111")
                .build());

        // Ventes du tenant A (2 ventes)
        em.persistAndFlush(VenteEntity.builder()
                .nomProduit("Collier Alpha")
                .quantite(1.0).prixUnitaire(new BigDecimal("5000"))
                .prixTotal(new BigDecimal("5000"))
                .dateVente(LocalDate.now())
                .utilisateur(userA).tenant(tenantA)
                .build());

        em.persistAndFlush(VenteEntity.builder()
                .nomProduit("Bracelet Alpha")
                .quantite(2.0).prixUnitaire(new BigDecimal("3000"))
                .prixTotal(new BigDecimal("6000"))
                .dateVente(LocalDate.now())
                .utilisateur(userA).tenant(tenantA)
                .build());

        // Vente du tenant B (1 vente — ne doit jamais être visible depuis A)
        UserEntity userB = em.persistAndFlush(UserEntity.builder()
                .nom("Beta").prenom("User")
                .email("user@beta.com")
                .motDePasse("encoded")
                .nomEntreprise("Boutique Beta")
                .numeroTelephone("+221772222222")
                .build());

        em.persistAndFlush(VenteEntity.builder()
                .nomProduit("Bague Beta CONFIDENTIELLE")
                .quantite(1.0).prixUnitaire(new BigDecimal("99999"))
                .prixTotal(new BigDecimal("99999"))
                .dateVente(LocalDate.now())
                .utilisateur(userB).tenant(tenantB)
                .build());

        // Crédits
        ClientEntity clientA = em.persistAndFlush(
                ClientEntity.builder().nom("Client Alpha").tenant(tenantA).build());
        ClientEntity clientB = em.persistAndFlush(
                ClientEntity.builder().nom("Client Beta CONFIDENTIEL").tenant(tenantB).build());

        em.persistAndFlush(CreditClientEntity.builder()
                .client(clientA)
                .montantInitial(new BigDecimal("10000"))
                .montantRestant(new BigDecimal("5000"))
                .statut(StatutCredit.EN_ATTENTE)
                .tenant(tenantA)
                .build());

        em.persistAndFlush(CreditClientEntity.builder()
                .client(clientB)
                .montantInitial(new BigDecimal("99999"))
                .montantRestant(new BigDecimal("99999"))
                .statut(StatutCredit.EN_ATTENTE)
                .tenant(tenantB)
                .build());

        em.flush();
    }

    // ==================== Ventes — isolation via filtre ====================

    @Test
    @DisplayName("Sans filtre — findAll() retourne les ventes des deux tenants")
    void sansFiltre_findAll_retourneVentesDesTenants() {
        // Pas de filtre actif — accès global (contexte sans tenant)
        List<VenteEntity> toutes = venteRepository.findAll();

        assertThat(toutes).hasSize(3); // 2 tenant-A + 1 tenant-B
    }

    @Test
    @DisplayName("Filtre tenant-A — findAll() retourne uniquement les ventes de A")
    void avecFiltreA_findAll_retourneSeulementVentesA() {
        activerFiltre("tenant-iso-A");

        List<VenteEntity> ventes = venteRepository.findAll();

        assertThat(ventes).hasSize(2);
        assertThat(ventes).extracting(VenteEntity::getNomProduit)
                .doesNotContain("Bague Beta CONFIDENTIELLE")
                .contains("Collier Alpha", "Bracelet Alpha");

        desactiverFiltre();
    }

    @Test
    @DisplayName("Filtre tenant-B — findAll() retourne uniquement la vente de B")
    void avecFiltreB_findAll_retourneSeulementVentesB() {
        activerFiltre("tenant-iso-B");

        List<VenteEntity> ventes = venteRepository.findAll();

        assertThat(ventes).hasSize(1);
        assertThat(ventes.get(0).getNomProduit()).isEqualTo("Bague Beta CONFIDENTIELLE");

        desactiverFiltre();
    }

    // ==================== Crédits — isolation via filtre ====================

    @Test
    @DisplayName("Filtre tenant-A — les crédits du tenant B sont invisibles")
    void avecFiltreA_creditsB_invisibles() {
        activerFiltre("tenant-iso-A");

        List<CreditClientEntity> credits = creditClientRepository.findAll();

        assertThat(credits).hasSize(1);
        assertThat(credits.get(0).getMontantInitial())
                .isEqualByComparingTo(new BigDecimal("10000")); // uniquement le crédit A
        // Le crédit B (99999) est invisible

        desactiverFiltre();
    }

    @Test
    @DisplayName("Agrégat sumMontantRestantActif — isolé par tenant UUID")
    void sumMontantRestantActif_filtreTenantUuid_isolationCorrecte() {
        // Sans filtre Hibernate — utilise le paramètre :tenantUuid dans la requête JPQL
        BigDecimal totalA = creditClientRepository.sumMontantRestantActif(
                StatutCredit.SOLDE, "tenant-iso-A");
        BigDecimal totalB = creditClientRepository.sumMontantRestantActif(
                StatutCredit.SOLDE, "tenant-iso-B");

        // Les agrégats JPQL avec :tenantUuid sont déjà isolés par conception
        assertThat(totalA).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(totalB).isEqualByComparingTo(new BigDecimal("99999"));
    }

    // ==================== Helpers ====================

    private void activerFiltre(String tenantUuid) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantUuid);
    }

    private void desactiverFiltre() {
        Session session = entityManager.unwrap(Session.class);
        session.disableFilter("tenantFilter");
    }
}
