package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.entity.VenteEntity.ModePaiementVente;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Tests VenteRepository — requêtes @Query")
class VenteRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private VenteRepository repo;

    private TenantEntity tenant;
    private TenantEntity autreTenant;
    private UserEntity user;
    private UserEntity autreUser;

    @BeforeEach
    void setUp() {
        tenant = em.persistAndFlush(TenantEntity.builder()
                .tenantUuid("tenant-vente-001")
                .nomEntreprise("Boutique Vente")
                .numeroTelephone("+221771111111")
                .build());

        autreTenant = em.persistAndFlush(TenantEntity.builder()
                .tenantUuid("tenant-vente-002")
                .nomEntreprise("Autre Boutique")
                .numeroTelephone("+221772222222")
                .build());

        user = em.persistAndFlush(UserEntity.builder()
                .nom("Diallo").prenom("Amadou")
                .email("amadou@boutique.com")
                .motDePasse("encoded")
                .nomEntreprise("Boutique Vente")
                .numeroTelephone("+221771111111")
                .build());

        autreUser = em.persistAndFlush(UserEntity.builder()
                .nom("Fall").prenom("Fatou")
                .email("fatou@boutique.com")
                .motDePasse("encoded")
                .nomEntreprise("Boutique Vente")
                .numeroTelephone("+221773333333")
                .build());

        // Vente 1 : Collier — ESPECES — 15 jan 2025
        em.persistAndFlush(VenteEntity.builder()
                .quantite(2.0)
                .nomProduit("Collier en or")
                .prixUnitaire(new BigDecimal("5000"))
                .prixTotal(new BigDecimal("10000"))
                .dateVente(LocalDateTime.of(2025, 1, 15, 0, 0))
                .modePaiement(ModePaiementVente.ESPECES)
                .client("Aminata Diallo")
                .utilisateur(user)
                .tenant(tenant)
                .build());

        // Vente 2 : Bracelet — WAVE — 20 jan 2025
        em.persistAndFlush(VenteEntity.builder()
                .quantite(1.0)
                .nomProduit("Bracelet argent")
                .prixUnitaire(new BigDecimal("3000"))
                .prixTotal(new BigDecimal("3000"))
                .dateVente(LocalDateTime.of(2025, 1, 20, 0, 0))
                .modePaiement(ModePaiementVente.WAVE)
                .utilisateur(user)
                .tenant(tenant)
                .build());

        // Vente 3 : Collier — ESPECES — 10 mars 2025 (hors janvier)
        em.persistAndFlush(VenteEntity.builder()
                .quantite(3.0)
                .nomProduit("Collier en or")
                .prixUnitaire(new BigDecimal("5000"))
                .prixTotal(new BigDecimal("15000"))
                .dateVente(LocalDateTime.of(2025, 3, 10, 0, 0))
                .modePaiement(ModePaiementVente.ESPECES)
                .client("Moussa Traoré")
                .utilisateur(autreUser)
                .tenant(tenant)
                .build());

        // Vente 4 : Bague — CREDIT — 15 jan 2025
        em.persistAndFlush(VenteEntity.builder()
                .quantite(1.0)
                .nomProduit("Bague diamant")
                .prixUnitaire(new BigDecimal("20000"))
                .prixTotal(new BigDecimal("20000"))
                .dateVente(LocalDateTime.of(2025, 1, 15, 0, 0))
                .modePaiement(ModePaiementVente.CREDIT)
                .utilisateur(user)
                .tenant(tenant)
                .build());

        // Vente dans un autre tenant (ne doit pas compter dans les agrégats tenant-001)
        em.persistAndFlush(VenteEntity.builder()
                .quantite(5.0)
                .nomProduit("Collier en or")
                .prixUnitaire(new BigDecimal("5000"))
                .prixTotal(new BigDecimal("25000"))
                .dateVente(LocalDateTime.of(2025, 1, 15, 0, 0))
                .modePaiement(ModePaiementVente.ESPECES)
                .utilisateur(autreUser)
                .tenant(autreTenant)
                .build());
    }

    // ==================== sumQuantiteByNomProduitAndTenant ====================

    @Test
    @DisplayName("sumQuantiteByNomProduitAndTenant — doit sommer toutes les ventes du produit")
    void sumQuantiteByNomProduitAndTenant_DoitSommerParProduit() {
        // "Collier en or" : vente1 (qte=2) + vente3 (qte=3) = 5 pour tenant-001
        Double sum = repo.sumQuantiteByNomProduitAndTenant("Collier en or", tenant);

        assertThat(sum).isEqualTo(5.0);
    }

    @Test
    @DisplayName("sumQuantiteByNomProduitAndTenant — isolation tenant : ne compte pas l'autre tenant")
    void sumQuantiteByNomProduitAndTenant_IsolationTenant() {
        // tenant-001 a 5 Colliers, autreTenant a 5 Colliers séparément
        Double sumTenant1 = repo.sumQuantiteByNomProduitAndTenant("Collier en or", tenant);
        Double sumTenant2 = repo.sumQuantiteByNomProduitAndTenant("Collier en or", autreTenant);

        assertThat(sumTenant1).isEqualTo(5.0);
        assertThat(sumTenant2).isEqualTo(5.0);
    }

    @Test
    @DisplayName("sumQuantiteByNomProduitAndTenant — doit retourner null si produit inexistant")
    void sumQuantiteByNomProduitAndTenant_RetourneNullSiProduitInexistant() {
        Double sum = repo.sumQuantiteByNomProduitAndTenant("Produit Inexistant", tenant);

        assertThat(sum).isNull();
    }

    // ==================== countByTenantUuid ====================

    @Test
    @DisplayName("countByTenantUuid — doit compter toutes les ventes du tenant")
    void countByTenantUuid_DoitCompterToutesLesVentes() {
        // tenant-001 a 4 ventes
        long count = repo.countByTenantUuid("tenant-vente-001");

        assertThat(count).isEqualTo(4L);
    }

    @Test
    @DisplayName("countByTenantUuid — isolation entre tenants")
    void countByTenantUuid_IsolationTenants() {
        long countTenant1 = repo.countByTenantUuid("tenant-vente-001");
        long countTenant2 = repo.countByTenantUuid("tenant-vente-002");

        assertThat(countTenant1).isEqualTo(4L);
        assertThat(countTenant2).isEqualTo(1L);
    }

    @Test
    @DisplayName("countByTenantUuid — doit retourner 0 pour un tenant inconnu")
    void countByTenantUuid_RetourneZeroPourTenantInconnu() {
        long count = repo.countByTenantUuid("tenant-inconnu");

        assertThat(count).isEqualTo(0L);
    }

    // ==================== findAllWithSearch ====================

    @Test
    @DisplayName("findAllWithSearch — sans filtre, retourne uniquement le tenant courant")
    void findAllWithSearch_SansFiltreRetourneTout() {
        // tenant-001 a 4 ventes (l'autreTenant est exclu par le filtre tenant explicite)
        Page<VenteEntity> page = repo.findAllWithSearch("tenant-vente-001", null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(4L);
    }

    @Test
    @DisplayName("findAllWithSearch — filtre par nomProduit (LOWER LIKE)")
    void findAllWithSearch_FiltreProduitInsensibleCasse() {
        // "collier" → vente1 + vente3 (tenant-001 seulement, pas autreTenant)
        Page<VenteEntity> page = repo.findAllWithSearch("tenant-vente-001", "collier", null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("findAllWithSearch — filtre par client")
    void findAllWithSearch_FiltreClient() {
        // "aminata" → client = "Aminata Diallo" (vente 1)
        Page<VenteEntity> page = repo.findAllWithSearch("tenant-vente-001", "aminata", null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findAllWithSearch — filtre dateDebut exclut les ventes antérieures")
    void findAllWithSearch_FiltreDateDebut() {
        // Ventes à partir du 1er mars 2025 → seule vente3 (10 mars)
        Page<VenteEntity> page = repo.findAllWithSearch("tenant-vente-001", null,
                LocalDateTime.of(2025, 3, 1, 0, 0), null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findAllWithSearch — filtre dateFin exclut les ventes postérieures")
    void findAllWithSearch_FiltreDateFin() {
        // Ventes jusqu'au 31 jan 2025 → vente1, vente2, vente4 (autreTenant exclu)
        Page<VenteEntity> page = repo.findAllWithSearch("tenant-vente-001", null,
                null, LocalDateTime.of(2025, 1, 31, 23, 59), PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("findAllWithSearch — plage de dates combinée")
    void findAllWithSearch_PlageDesDates() {
        // 1er–31 jan 2025 → vente1 (15 jan), vente2 (20 jan), vente4 (15 jan) — autreTenant exclu
        Page<VenteEntity> page = repo.findAllWithSearch("tenant-vente-001", null,
                LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 31, 23, 59), PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("findAllWithSearch — isolation tenant : l'autreTenant ne voit pas les ventes de tenant-001")
    void findAllWithSearch_IsolationTenant() {
        Page<VenteEntity> pageTenant2 = repo.findAllWithSearch("tenant-vente-002", null, null, null, PageRequest.of(0, 20));

        assertThat(pageTenant2.getTotalElements()).isEqualTo(1L);
    }

    // ==================== sumDirectVentesParModeEtPeriode ====================

    @Test
    @DisplayName("sumDirectVentesParModeEtPeriode — doit agréger par mode, hors CREDIT")
    void sumDirectVentesParModeEtPeriode_DoitAgregerSaufCredit() {
        LocalDateTime debut = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2025, 1, 31, 23, 59);

        List<Object[]> result = repo.sumDirectVentesParModeEtPeriode(
                debut, fin, ModePaiementVente.CREDIT, "tenant-vente-001");

        // En janvier 2025 pour tenant-001 (hors CREDIT) : ESPECES (10000) + WAVE (3000) = 2 lignes
        assertThat(result).hasSize(2);

        // Vérifie que CREDIT est absent des résultats
        boolean creditPresent = result.stream()
                .anyMatch(row -> ModePaiementVente.CREDIT.equals(row[0]));
        assertThat(creditPresent).isFalse();
    }

    @Test
    @DisplayName("sumDirectVentesParModeEtPeriode — vérifie les montants ESPECES")
    void sumDirectVentesParModeEtPeriode_VerifieMontantsEspeces() {
        LocalDateTime debut = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2025, 1, 31, 23, 59);

        List<Object[]> result = repo.sumDirectVentesParModeEtPeriode(
                debut, fin, ModePaiementVente.CREDIT, "tenant-vente-001");

        Object[] especes = result.stream()
                .filter(row -> ModePaiementVente.ESPECES.equals(row[0]))
                .findFirst()
                .orElseThrow();

        long countEspeces = ((Number) especes[1]).longValue();
        BigDecimal sumEspeces = (BigDecimal) especes[2];

        // 1 vente ESPECES en janvier pour tenant-001 (vente1 = 10000)
        assertThat(countEspeces).isEqualTo(1L);
        assertThat(sumEspeces).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("sumDirectVentesParModeEtPeriode — doit retourner liste vide hors période")
    void sumDirectVentesParModeEtPeriode_HorsPeriodeRetourneVide() {
        LocalDateTime debut = LocalDateTime.of(2020, 1, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2020, 12, 31, 23, 59);

        List<Object[]> result = repo.sumDirectVentesParModeEtPeriode(
                debut, fin, ModePaiementVente.CREDIT, "tenant-vente-001");

        assertThat(result).isEmpty();
    }

    // ==================== findByUtilisateurWithSearch ====================

    @Test
    @DisplayName("findByUtilisateurWithSearch — doit retourner uniquement les ventes de l'utilisateur")
    void findByUtilisateurWithSearch_DoitFiltrerParUtilisateur() {
        // user a 3 ventes (vente1, vente2, vente4), autreUser a 1 vente dans tenant-001
        Page<VenteEntity> page = repo.findByUtilisateurWithSearch(
                user, "tenant-vente-001", null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("findByUtilisateurWithSearch — filtre par produit en plus de l'utilisateur")
    void findByUtilisateurWithSearch_AvecFiltreProduit() {
        // user + "Collier" → vente1 (15 jan)
        Page<VenteEntity> page = repo.findByUtilisateurWithSearch(
                user, "tenant-vente-001", "collier", null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findByUtilisateurWithSearch — filtre par dates")
    void findByUtilisateurWithSearch_AvecFiltreDate() {
        // user + entre 14 jan et 16 jan → vente1 (15 jan) + vente4 (15 jan) = 2
        Page<VenteEntity> page = repo.findByUtilisateurWithSearch(
                user, "tenant-vente-001", null,
                LocalDate.of(2025, 1, 14),
                LocalDate.of(2025, 1, 16),
                PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("findByUtilisateurWithSearch — pagination fonctionne")
    void findByUtilisateurWithSearch_PaginationFonctionne() {
        Page<VenteEntity> page = repo.findByUtilisateurWithSearch(
                user, "tenant-vente-001", null, null, null, PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3L);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
}
