package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.TenantEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires — AchatDto.fromEntity()")
class AchatDtoTest {

    private AchatEntity achatAvecPlan(TenantEntity.Plan plan) {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("uuid-achat")
                .nomEntreprise("Boutique Test")
                .numeroTelephone("+221771111111")
                .plan(plan)
                .build();
        return AchatEntity.builder()
                .nomProduit("Tissu wax")
                .quantite(10.0)
                .prixUnitaire(new BigDecimal("2000"))
                .prixTotal(new BigDecimal("20000"))
                .dateAchat(LocalDate.now())
                .photoUrl("/photos/tissu.jpg")
                .tenant(tenant)
                .build();
    }

    @Test
    @DisplayName("fromEntity(null) retourne null")
    void fromEntity_null_retourneNull() {
        assertThat(AchatDto.fromEntity(null)).isNull();
    }

    @Test
    @DisplayName("Plan GRATUIT — photoUrl masquée")
    void fromEntity_planGRATUIT_photoUrlNull() {
        assertThat(AchatDto.fromEntity(achatAvecPlan(TenantEntity.Plan.GRATUIT)).getPhotoUrl())
                .isNull();
    }

    @Test
    @DisplayName("Plan BASIC — photoUrl masquée")
    void fromEntity_planBASIC_photoUrlNull() {
        assertThat(AchatDto.fromEntity(achatAvecPlan(TenantEntity.Plan.STARTER)).getPhotoUrl())
                .isNull();
    }

    @Test
    @DisplayName("Plan PREMIUM — photoUrl visible")
    void fromEntity_planPREMIUM_photoUrlVisible() {
        assertThat(AchatDto.fromEntity(achatAvecPlan(TenantEntity.Plan.PRO)).getPhotoUrl())
                .isEqualTo("/photos/tissu.jpg");
    }

    @Test
    @DisplayName("Plan ENTREPRISE — photoUrl visible")
    void fromEntity_planENTREPRISE_photoUrlVisible() {
        assertThat(AchatDto.fromEntity(achatAvecPlan(TenantEntity.Plan.BUSINESS)).getPhotoUrl())
                .isEqualTo("/photos/tissu.jpg");
    }

    @Test
    @DisplayName("Tenant null — photoUrl masquée")
    void fromEntity_tenantNull_photoUrlNull() {
        AchatEntity achat = AchatEntity.builder()
                .nomProduit("Tissu wax")
                .quantite(5.0)
                .prixUnitaire(new BigDecimal("2000"))
                .prixTotal(new BigDecimal("10000"))
                .dateAchat(LocalDate.now())
                .photoUrl("/photos/tissu.jpg")
                .tenant(null)
                .build();

        assertThat(AchatDto.fromEntity(achat).getPhotoUrl()).isNull();
    }

    @Test
    @DisplayName("fromEntityWithoutUser — PREMIUM visible mais sans utilisateur")
    void fromEntityWithoutUser_planPREMIUM_photoVisibleSansUtilisateur() {
        AchatDto dto = AchatDto.fromEntityWithoutUser(achatAvecPlan(TenantEntity.Plan.PRO));

        assertThat(dto.getPhotoUrl()).isEqualTo("/photos/tissu.jpg");
        assertThat(dto.getUtilisateur()).isNull();
    }

    @Test
    @DisplayName("Achat d'aujourd'hui — estRecent=true, mois/annee corrects")
    void fromEntity_achatDuJour_champsCalculesCorrects() {
        LocalDate today = LocalDate.now();
        AchatDto dto = AchatDto.fromEntity(achatAvecPlan(TenantEntity.Plan.STARTER));

        assertThat(dto.getEstRecent()).isTrue();
        assertThat(dto.getMois()).isEqualTo(today.getMonthValue());
        assertThat(dto.getAnnee()).isEqualTo(today.getYear());
    }
}
