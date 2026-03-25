package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Tests AchatRepository — requêtes @Query")
class AchatRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AchatRepository achatRepository;

    private TenantEntity tenant;
    private TenantEntity autreTenant;
    private UserEntity utilisateur;
    private UserEntity autreUtilisateur;

    @BeforeEach
    void setUp() {
        tenant = entityManager.persistAndFlush(TenantEntity.builder()
                .tenantUuid("achat-tenant-001")
                .nomEntreprise("Boutique Achat")
                .numeroTelephone("+221771111111")
                .plan(TenantEntity.Plan.STARTER)
                .build());

        autreTenant = entityManager.persistAndFlush(TenantEntity.builder()
                .tenantUuid("achat-tenant-002")
                .nomEntreprise("Autre Boutique Achat")
                .numeroTelephone("+221772222222")
                .plan(TenantEntity.Plan.PRO)
                .build());

        utilisateur = entityManager.persistAndFlush(UserEntity.builder()
                .nom("Diallo").prenom("Amadou")
                .email("amadou.achat@test.com")
                .motDePasse("encoded")
                .nomEntreprise("Boutique Achat")
                .numeroTelephone("+221771111111")
                .build());

        autreUtilisateur = entityManager.persistAndFlush(UserEntity.builder()
                .nom("Fall").prenom("Fatou")
                .email("fatou.achat@test.com")
                .motDePasse("encoded")
                .nomEntreprise("Autre Boutique Achat")
                .numeroTelephone("+221773333333")
                .build());
    }

    // ==================== sumQuantiteByNomProduitAndTenant ====================

    @Test
    @DisplayName("sumQuantiteByNomProduitAndTenant — aucun enregistrement → retourne null")
    void sumQuantiteByNomProduitAndTenant_AucunEnregistrement_RetourneNull() {
        // Act
        Integer result = achatRepository.sumQuantiteByNomProduitAndTenant("Produit Inexistant", tenant);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("sumQuantiteByNomProduitAndTenant — 2 achats (qte 10 + qte 5) → retourne 15")
    void sumQuantiteByNomProduitAndTenant_DeuxAchats_RetourneQuinze() {
        // Arrange
        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Collier en or")
                .quantite(10)
                .prixUnitaire(new BigDecimal("5000.00"))
                .prixTotal(new BigDecimal("50000.00"))
                .dateAchat(LocalDate.of(2025, 1, 10))
                .fournisseur("Fournisseur A")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Collier en or")
                .quantite(5)
                .prixUnitaire(new BigDecimal("5000.00"))
                .prixTotal(new BigDecimal("25000.00"))
                .dateAchat(LocalDate.of(2025, 2, 15))
                .fournisseur("Fournisseur B")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        // Act
        Integer result = achatRepository.sumQuantiteByNomProduitAndTenant("Collier en or", tenant);

        // Assert
        assertThat(result).isEqualTo(15);
    }

    @Test
    @DisplayName("sumQuantiteByNomProduitAndTenant — tenant différent → retourne null (isolation)")
    void sumQuantiteByNomProduitAndTenant_TenantDifferent_RetourneNull() {
        // Arrange — achat dans autreTenant
        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Bracelet argent")
                .quantite(8)
                .prixUnitaire(new BigDecimal("3000.00"))
                .prixTotal(new BigDecimal("24000.00"))
                .dateAchat(LocalDate.of(2025, 3, 1))
                .fournisseur("Fournisseur C")
                .utilisateur(autreUtilisateur)
                .tenant(autreTenant)
                .build());

        // Act — chercher dans tenant (qui n'a aucun achat de ce produit)
        Integer result = achatRepository.sumQuantiteByNomProduitAndTenant("Bracelet argent", tenant);

        // Assert
        assertThat(result).isNull();
    }

    // ==================== findAllWithSearch ====================

    @Test
    @DisplayName("findAllWithSearch — sans filtre, sans dates → retourne tout")
    void findAllWithSearch_SansFiltreNiDates_RetourneTout() {
        // Arrange
        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Montre dorée")
                .quantite(3)
                .prixUnitaire(new BigDecimal("15000.00"))
                .prixTotal(new BigDecimal("45000.00"))
                .dateAchat(LocalDate.of(2025, 1, 5))
                .fournisseur("Fournisseur X")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Bague diamant")
                .quantite(2)
                .prixUnitaire(new BigDecimal("25000.00"))
                .prixTotal(new BigDecimal("50000.00"))
                .dateAchat(LocalDate.of(2025, 2, 10))
                .fournisseur("Fournisseur Y")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        // Act
        Page<AchatEntity> page = achatRepository.findAllWithSearch(null, null, null, PageRequest.of(0, 20));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("findAllWithSearch — recherche par nomProduit → retourne correspondances")
    void findAllWithSearch_RechercheProduit_RetourneCorrespondances() {
        // Arrange
        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Collier perles")
                .quantite(4)
                .prixUnitaire(new BigDecimal("8000.00"))
                .prixTotal(new BigDecimal("32000.00"))
                .dateAchat(LocalDate.of(2025, 1, 20))
                .fournisseur("Fournisseur Bijoux")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Pendentif argent")
                .quantite(6)
                .prixUnitaire(new BigDecimal("4000.00"))
                .prixTotal(new BigDecimal("24000.00"))
                .dateAchat(LocalDate.of(2025, 1, 25))
                .fournisseur("Fournisseur Argent")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        // Act — recherche insensible à la casse
        Page<AchatEntity> page = achatRepository.findAllWithSearch("COLLIER", null, null, PageRequest.of(0, 20));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent().get(0).getNomProduit()).isEqualTo("Collier perles");
    }

    @Test
    @DisplayName("findAllWithSearch — recherche par fournisseur → retourne correspondances")
    void findAllWithSearch_RechercheFournisseur_RetourneCorrespondances() {
        // Arrange
        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Produit A")
                .quantite(5)
                .prixUnitaire(new BigDecimal("2000.00"))
                .prixTotal(new BigDecimal("10000.00"))
                .dateAchat(LocalDate.of(2025, 3, 10))
                .fournisseur("Dakar Import")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Produit B")
                .quantite(3)
                .prixUnitaire(new BigDecimal("3000.00"))
                .prixTotal(new BigDecimal("9000.00"))
                .dateAchat(LocalDate.of(2025, 3, 15))
                .fournisseur("Thiès Export")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        // Act — recherche par fournisseur
        Page<AchatEntity> page = achatRepository.findAllWithSearch("dakar", null, null, PageRequest.of(0, 20));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent().get(0).getFournisseur()).isEqualTo("Dakar Import");
    }

    @Test
    @DisplayName("findAllWithSearch — filtre dateDebut → retourne uniquement après la date")
    void findAllWithSearch_FiltreDateDebut_RetourneUniquementApresDate() {
        // Arrange
        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Ancien Produit")
                .quantite(2)
                .prixUnitaire(new BigDecimal("5000.00"))
                .prixTotal(new BigDecimal("10000.00"))
                .dateAchat(LocalDate.of(2025, 1, 5))
                .fournisseur("Fournisseur Ancien")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Nouveau Produit")
                .quantite(4)
                .prixUnitaire(new BigDecimal("7000.00"))
                .prixTotal(new BigDecimal("28000.00"))
                .dateAchat(LocalDate.of(2025, 6, 20))
                .fournisseur("Fournisseur Nouveau")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        // Act — dateDebut = 1er juin 2025
        Page<AchatEntity> page = achatRepository.findAllWithSearch(
                null, LocalDate.of(2025, 6, 1), null, PageRequest.of(0, 20));

        // Assert — seul "Nouveau Produit" (20 juin) est retourné
        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent().get(0).getNomProduit()).isEqualTo("Nouveau Produit");
    }

    @Test
    @DisplayName("findAllWithSearch — filtre dateFin → retourne uniquement avant la date")
    void findAllWithSearch_FiltreDateFin_RetourneUniquementAvantDate() {
        // Arrange
        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Produit Janvier")
                .quantite(3)
                .prixUnitaire(new BigDecimal("4000.00"))
                .prixTotal(new BigDecimal("12000.00"))
                .dateAchat(LocalDate.of(2025, 1, 15))
                .fournisseur("Fournisseur Jan")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        entityManager.persistAndFlush(AchatEntity.builder()
                .nomProduit("Produit Décembre")
                .quantite(1)
                .prixUnitaire(new BigDecimal("10000.00"))
                .prixTotal(new BigDecimal("10000.00"))
                .dateAchat(LocalDate.of(2025, 12, 1))
                .fournisseur("Fournisseur Dec")
                .utilisateur(utilisateur)
                .tenant(tenant)
                .build());

        // Act — dateFin = 28 février 2025
        Page<AchatEntity> page = achatRepository.findAllWithSearch(
                null, null, LocalDate.of(2025, 2, 28), PageRequest.of(0, 20));

        // Assert — seul "Produit Janvier" est retourné
        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent().get(0).getNomProduit()).isEqualTo("Produit Janvier");
    }
}
