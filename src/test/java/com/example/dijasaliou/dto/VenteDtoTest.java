package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.VenteEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires — VenteDto.fromEntity()")
class VenteDtoTest {

    /** Construit une vente avec photo et le plan donné. */
    private VenteEntity venteAvecPlan(TenantEntity.Plan plan) {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("uuid-test")
                .nomEntreprise("Boutique Test")
                .numeroTelephone("+221771111111")
                .plan(plan)
                .build();
        return VenteEntity.builder()
                .nomProduit("Collier en or")
                .quantite(2)
                .prixUnitaire(new BigDecimal("5000"))
                .prixTotal(new BigDecimal("10000"))
                .dateVente(LocalDate.now())
                .photoUrl("/photos/collier.jpg")
                .modePaiement(VenteEntity.ModePaiementVente.ESPECES)
                .tenant(tenant)
                .build();
    }

    // ==================== null guard ====================

    @Test
    @DisplayName("fromEntity(null) retourne null")
    void fromEntity_null_retourneNull() {
        assertThat(VenteDto.fromEntity(null)).isNull();
    }

    @Test
    @DisplayName("fromEntityWithoutUser(null) retourne null")
    void fromEntityWithoutUser_null_retourneNull() {
        assertThat(VenteDto.fromEntityWithoutUser(null)).isNull();
    }

    // ==================== Filtrage photo selon le plan ====================

    @Test
    @DisplayName("Plan GRATUIT — photoUrl masquée (null) même si entité a une photo")
    void fromEntity_planGRATUIT_photoUrlNull() {
        VenteDto dto = VenteDto.fromEntity(venteAvecPlan(TenantEntity.Plan.GRATUIT));
        assertThat(dto.getPhotoUrl()).isNull();
    }

    @Test
    @DisplayName("Plan BASIC — photoUrl masquée (null) même si entité a une photo")
    void fromEntity_planBASIC_photoUrlNull() {
        VenteDto dto = VenteDto.fromEntity(venteAvecPlan(TenantEntity.Plan.BASIC));
        assertThat(dto.getPhotoUrl()).isNull();
    }

    @Test
    @DisplayName("Plan PREMIUM — photoUrl visible")
    void fromEntity_planPREMIUM_photoUrlVisible() {
        VenteDto dto = VenteDto.fromEntity(venteAvecPlan(TenantEntity.Plan.PREMIUM));
        assertThat(dto.getPhotoUrl()).isEqualTo("/photos/collier.jpg");
    }

    @Test
    @DisplayName("Plan ENTREPRISE — photoUrl visible")
    void fromEntity_planENTREPRISE_photoUrlVisible() {
        VenteDto dto = VenteDto.fromEntity(venteAvecPlan(TenantEntity.Plan.ENTREPRISE));
        assertThat(dto.getPhotoUrl()).isEqualTo("/photos/collier.jpg");
    }

    @Test
    @DisplayName("Tenant null — photoUrl masquée (null)")
    void fromEntity_tenantNull_photoUrlNull() {
        VenteEntity vente = VenteEntity.builder()
                .nomProduit("Bague")
                .quantite(1)
                .prixUnitaire(new BigDecimal("8000"))
                .prixTotal(new BigDecimal("8000"))
                .dateVente(LocalDate.now())
                .photoUrl("/photos/bague.jpg")
                .tenant(null) // tenant absent
                .build();

        assertThat(VenteDto.fromEntity(vente).getPhotoUrl()).isNull();
    }

    // ==================== fromEntityWithoutUser ====================

    @Test
    @DisplayName("fromEntityWithoutUser — PREMIUM retourne photo mais sans champ utilisateur")
    void fromEntityWithoutUser_planPREMIUM_photoVisibleUtilisateurAbsent() {
        VenteDto dto = VenteDto.fromEntityWithoutUser(venteAvecPlan(TenantEntity.Plan.PREMIUM));

        assertThat(dto.getPhotoUrl()).isEqualTo("/photos/collier.jpg");
        assertThat(dto.getUtilisateur()).isNull();
    }

    @Test
    @DisplayName("fromEntityWithoutUser — GRATUIT masque la photo")
    void fromEntityWithoutUser_planGRATUIT_photoNull() {
        VenteDto dto = VenteDto.fromEntityWithoutUser(venteAvecPlan(TenantEntity.Plan.GRATUIT));
        assertThat(dto.getPhotoUrl()).isNull();
    }

    // ==================== Champs calculés ====================

    @Test
    @DisplayName("Vente d'aujourd'hui — estRecente=true, mois et annee corrects")
    void fromEntity_venteDuJour_champsCalculesCorrects() {
        LocalDate today = LocalDate.now();
        VenteDto dto = VenteDto.fromEntity(venteAvecPlan(TenantEntity.Plan.BASIC));

        assertThat(dto.getEstRecente()).isTrue();
        assertThat(dto.getMois()).isEqualTo(today.getMonthValue());
        assertThat(dto.getAnnee()).isEqualTo(today.getYear());
    }

    @Test
    @DisplayName("Vente vieille de 60 jours — estRecente=false")
    void fromEntity_vente60JoursAvant_pasRecente() {
        VenteEntity vente = VenteEntity.builder()
                .nomProduit("Bracelet")
                .quantite(1)
                .prixUnitaire(new BigDecimal("3000"))
                .prixTotal(new BigDecimal("3000"))
                .dateVente(LocalDate.now().minusDays(60))
                .tenant(TenantEntity.builder()
                        .plan(TenantEntity.Plan.BASIC)
                        .nomEntreprise("B").numeroTelephone("0").tenantUuid("u").build())
                .build();

        assertThat(VenteDto.fromEntity(vente).getEstRecente()).isFalse();
    }

    // ==================== modePaiement ====================

    @Test
    @DisplayName("modePaiement null sur entité → 'ESPECES' dans le DTO (valeur par défaut)")
    void fromEntity_modePaiementNull_defaultEspeces() {
        VenteEntity vente = venteAvecPlan(TenantEntity.Plan.BASIC);
        vente.setModePaiement(null); // force null post-build

        assertThat(VenteDto.fromEntity(vente).getModePaiement()).isEqualTo("ESPECES");
    }

    @Test
    @DisplayName("modePaiement WAVE → 'WAVE' dans le DTO")
    void fromEntity_modePaiementWAVE_retourneWAVE() {
        VenteEntity vente = VenteEntity.builder()
                .nomProduit("Collier")
                .quantite(1)
                .prixUnitaire(new BigDecimal("5000"))
                .prixTotal(new BigDecimal("5000"))
                .dateVente(LocalDate.now())
                .modePaiement(VenteEntity.ModePaiementVente.WAVE)
                .tenant(TenantEntity.builder()
                        .plan(TenantEntity.Plan.BASIC)
                        .nomEntreprise("B").numeroTelephone("0").tenantUuid("u").build())
                .build();

        assertThat(VenteDto.fromEntity(vente).getModePaiement()).isEqualTo("WAVE");
    }

    // ==================== aUnClient ====================

    @Test
    @DisplayName("client non vide → aUnClient=true")
    void fromEntity_avecClient_aUnClientTrue() {
        VenteEntity vente = VenteEntity.builder()
                .nomProduit("Anneau")
                .quantite(1)
                .prixUnitaire(new BigDecimal("2000"))
                .prixTotal(new BigDecimal("2000"))
                .dateVente(LocalDate.now())
                .client("Aminata Diallo")
                .tenant(TenantEntity.builder()
                        .plan(TenantEntity.Plan.BASIC)
                        .nomEntreprise("B").numeroTelephone("0").tenantUuid("u").build())
                .build();

        assertThat(VenteDto.fromEntity(vente).getAUnClient()).isTrue();
    }

    @Test
    @DisplayName("client null → aUnClient=false")
    void fromEntity_clientNull_aUnClientFalse() {
        VenteEntity vente = VenteEntity.builder()
                .nomProduit("Anneau")
                .quantite(1)
                .prixUnitaire(new BigDecimal("2000"))
                .prixTotal(new BigDecimal("2000"))
                .dateVente(LocalDate.now())
                .client(null)
                .tenant(TenantEntity.builder()
                        .plan(TenantEntity.Plan.BASIC)
                        .nomEntreprise("B").numeroTelephone("0").tenantUuid("u").build())
                .build();

        assertThat(VenteDto.fromEntity(vente).getAUnClient()).isFalse();
    }
}
