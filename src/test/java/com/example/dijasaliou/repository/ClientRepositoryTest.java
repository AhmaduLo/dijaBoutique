package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.ClientEntity;
import com.example.dijasaliou.entity.TenantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Tests ClientRepository — requêtes @Query")
class ClientRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClientRepository clientRepository;

    private TenantEntity tenant;
    private TenantEntity autreTenant;

    @BeforeEach
    void setUp() {
        tenant = entityManager.persistAndFlush(TenantEntity.builder()
                .tenantUuid("client-tenant-001")
                .nomEntreprise("Boutique Client Test")
                .numeroTelephone("+221781111111")
                .plan(TenantEntity.Plan.PRO)
                .build());

        autreTenant = entityManager.persistAndFlush(TenantEntity.builder()
                .tenantUuid("client-tenant-002")
                .nomEntreprise("Autre Boutique Client")
                .numeroTelephone("+221782222222")
                .plan(TenantEntity.Plan.STARTER)
                .build());
    }

    // ==================== findAllWithSearch (List) ====================

    @Test
    @DisplayName("findAllWithSearch — sans search, tenantUuid correspond → retourne tous les clients du tenant")
    void findAllWithSearch_SansSearch_RetourneTousLesClients() {
        // Arrange
        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Aminata Diallo")
                .telephone("+221771000001")
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Moussa Traoré")
                .telephone("+221771000002")
                .tenant(tenant)
                .build());

        // Act
        List<ClientEntity> clients = clientRepository.findAllWithSearch(null, "client-tenant-001");

        // Assert
        assertThat(clients).hasSize(2);
    }

    @Test
    @DisplayName("findAllWithSearch — recherche par nom → retourne correspondances")
    void findAllWithSearch_RechercheParNom_RetourneCorrespondances() {
        // Arrange
        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Fatou Ndiaye")
                .telephone("+221771100001")
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Ibrahim Sow")
                .telephone("+221771100002")
                .tenant(tenant)
                .build());

        // Act — recherche insensible à la casse
        List<ClientEntity> clients = clientRepository.findAllWithSearch("fatou", "client-tenant-001");

        // Assert
        assertThat(clients).hasSize(1);
        assertThat(clients.get(0).getNom()).isEqualTo("Fatou Ndiaye");
    }

    @Test
    @DisplayName("findAllWithSearch — recherche par téléphone → retourne correspondances")
    void findAllWithSearch_RechercheParTelephone_RetourneCorrespondances() {
        // Arrange
        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Seydou Konaté")
                .telephone("+221779988776")
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Mariam Coulibaly")
                .telephone("+221776655443")
                .tenant(tenant)
                .build());

        // Act — recherche sur fragment de téléphone
        List<ClientEntity> clients = clientRepository.findAllWithSearch("779988", "client-tenant-001");

        // Assert
        assertThat(clients).hasSize(1);
        assertThat(clients.get(0).getNom()).isEqualTo("Seydou Konaté");
    }

    @Test
    @DisplayName("findAllWithSearch — tenantUuid différent → retourne liste vide")
    void findAllWithSearch_TenantDifferent_RetourneListeVide() {
        // Arrange — clients dans tenant principal
        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Oumou Balde")
                .telephone("+221770000001")
                .tenant(tenant)
                .build());

        // Act — chercher avec l'UUID de l'autre tenant (qui n'a aucun client)
        List<ClientEntity> clients = clientRepository.findAllWithSearch(null, "client-tenant-002");

        // Assert
        assertThat(clients).isEmpty();
    }

    // ==================== findAllWithSearchPaged ====================

    @Test
    @DisplayName("findAllWithSearchPaged — page 0, size 1 → retourne uniquement la première page")
    void findAllWithSearchPaged_PageZereTailleUn_RetournePremierePage() {
        // Arrange
        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Alpha Diallo")
                .telephone("+221770011111")
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Beta Diallo")
                .telephone("+221770022222")
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Gamma Diallo")
                .telephone("+221770033333")
                .tenant(tenant)
                .build());

        // Act
        Page<ClientEntity> page = clientRepository.findAllWithSearchPaged(
                null, "client-tenant-001", PageRequest.of(0, 1));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(3L);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    // ==================== findClientsAvecDette ====================

    @Test
    @DisplayName("findClientsAvecDette — aucun client avec dette → retourne liste vide")
    void findClientsAvecDette_AucunClientAvecDette_RetourneListeVide() {
        // Arrange — clients sans dette
        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Client Sans Dette")
                .telephone("+221771500001")
                .detteTotale(BigDecimal.ZERO)
                .tenant(tenant)
                .build());

        // Act
        List<ClientEntity> result = clientRepository.findClientsAvecDette("client-tenant-001");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findClientsAvecDette — client avec dette > 0 → retourné, trié par dette DESC")
    void findClientsAvecDette_ClientsAvecDette_RetournesOrdreDesc() {
        // Arrange
        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Petit Débiteur")
                .telephone("+221771600001")
                .detteTotale(new BigDecimal("5000.00"))
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Grand Débiteur")
                .telephone("+221771600002")
                .detteTotale(new BigDecimal("50000.00"))
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Client Soldé")
                .telephone("+221771600003")
                .detteTotale(BigDecimal.ZERO)
                .tenant(tenant)
                .build());

        // Act
        List<ClientEntity> result = clientRepository.findClientsAvecDette("client-tenant-001");

        // Assert — 2 clients avec dette, triés DESC
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNom()).isEqualTo("Grand Débiteur");
        assertThat(result.get(1).getNom()).isEqualTo("Petit Débiteur");
    }

    @Test
    @DisplayName("findClientsAvecDette — retourne uniquement les clients du tenant spécifié")
    void findClientsAvecDette_IsolationTenant_RetourneUniquementClientsDuTenant() {
        // Arrange — client avec dette dans tenant principal
        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Client Tenant Principal")
                .telephone("+221771700001")
                .detteTotale(new BigDecimal("10000.00"))
                .tenant(tenant)
                .build());

        // Arrange — client avec dette dans autre tenant
        entityManager.persistAndFlush(ClientEntity.builder()
                .nom("Client Autre Tenant")
                .telephone("+221771700002")
                .detteTotale(new BigDecimal("20000.00"))
                .tenant(autreTenant)
                .build());

        // Act — chercher uniquement pour tenant principal
        List<ClientEntity> result = clientRepository.findClientsAvecDette("client-tenant-001");

        // Assert — seul le client du tenant principal est retourné
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).isEqualTo("Client Tenant Principal");
    }
}
