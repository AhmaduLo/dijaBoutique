package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires — TenantAdminDto.fromEntity() / calculerStatut()")
class TenantAdminDtoTest {

    private final UserEntity admin = UserEntity.builder()
            .nom("Diallo").prenom("Amadou")
            .email("admin@boutique.com")
            .motDePasse("encoded")
            .nomEntreprise("Boutique Test")
            .numeroTelephone("+221771234567")
            .build();

    // ==================== SUSPENDU ====================

    @Test
    @DisplayName("Tenant inactif (actif=false) → statut SUSPENDU (priorité absolue)")
    void calculerStatut_tenantInactif_SUSPENDU() {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("t1").nomEntreprise("B").numeroTelephone("0")
                .actif(false)
                .plan(TenantEntity.Plan.STARTER)
                .dateExpiration(LocalDateTime.now().plusDays(30))
                .build();

        TenantAdminDto dto = TenantAdminDto.fromEntity(tenant, 3L, 0L, admin, null);
        assertThat(dto.getStatut()).isEqualTo("SUSPENDU");
    }

    @Test
    @DisplayName("Tenant inactif même en période d'essai → SUSPENDU")
    void calculerStatut_tenantInactifAvecEssai_SUSPENDU() {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("t2").nomEntreprise("B").numeroTelephone("0")
                .actif(false)
                .plan(TenantEntity.Plan.GRATUIT)
                .dateDebutEssai(LocalDateTime.now())
                .essaiUtilise(false)
                .build();

        TenantAdminDto dto = TenantAdminDto.fromEntity(tenant, 1L, 0L, admin, null);
        assertThat(dto.getStatut()).isEqualTo("SUSPENDU");
    }

    // ==================== ESSAI ====================

    @Test
    @DisplayName("Essai valide (démarré il y a 1 jour, non utilisé) → ESSAI")
    void calculerStatut_essaiValide_ESSAI() {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("t3").nomEntreprise("B").numeroTelephone("0")
                .actif(true)
                .plan(TenantEntity.Plan.GRATUIT)
                .dateDebutEssai(LocalDateTime.now().minusDays(1))
                .essaiUtilise(false)
                .build();

        TenantAdminDto dto = TenantAdminDto.fromEntity(tenant, 1L, 0L, admin, null);
        assertThat(dto.getStatut()).isEqualTo("ESSAI");
    }

    // ==================== EXPIRE ====================

    @Test
    @DisplayName("Essai terminé + plan GRATUIT → EXPIRE")
    void calculerStatut_essaiTermineEtPlanGratuit_EXPIRE() {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("t4").nomEntreprise("B").numeroTelephone("0")
                .actif(true)
                .plan(TenantEntity.Plan.GRATUIT)
                .essaiUtilise(true)   // essai consommé
                .build();

        TenantAdminDto dto = TenantAdminDto.fromEntity(tenant, 1L, 0L, admin, null);
        assertThat(dto.getStatut()).isEqualTo("EXPIRE");
    }

    @Test
    @DisplayName("Plan BASIC avec dateExpiration hier → EXPIRE")
    void calculerStatut_planBasicExpire_EXPIRE() {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("t5").nomEntreprise("B").numeroTelephone("0")
                .actif(true)
                .plan(TenantEntity.Plan.STARTER)
                .essaiUtilise(true)
                .dateExpiration(LocalDateTime.now().minusDays(1))
                .build();

        TenantAdminDto dto = TenantAdminDto.fromEntity(tenant, 2L, 0L, admin, null);
        assertThat(dto.getStatut()).isEqualTo("EXPIRE");
    }

    // ==================== ACTIF ====================

    @Test
    @DisplayName("Plan BASIC sans dateExpiration → ACTIF (abonnement perpétuel)")
    void calculerStatut_planBasicSansExpiration_ACTIF() {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("t6").nomEntreprise("B").numeroTelephone("0")
                .actif(true)
                .plan(TenantEntity.Plan.STARTER)
                .essaiUtilise(true)
                .dateExpiration(null)
                .build();

        TenantAdminDto dto = TenantAdminDto.fromEntity(tenant, 3L, 0L, admin, null);
        assertThat(dto.getStatut()).isEqualTo("ACTIF");
    }

    @Test
    @DisplayName("Plan BASIC avec dateExpiration demain → ACTIF")
    void calculerStatut_planBasicExpirationFutur_ACTIF() {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("t7").nomEntreprise("B").numeroTelephone("0")
                .actif(true)
                .plan(TenantEntity.Plan.PRO)
                .essaiUtilise(true)
                .dateExpiration(LocalDateTime.now().plusDays(1))
                .build();

        TenantAdminDto dto = TenantAdminDto.fromEntity(tenant, 5L, 0L, admin, null);
        assertThat(dto.getStatut()).isEqualTo("ACTIF");
    }

    // ==================== Infos admin ====================

    @Test
    @DisplayName("admin=null → champs admin tous null dans le DTO")
    void fromEntity_adminNull_champsAdminNull() {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("t8").nomEntreprise("B").numeroTelephone("0")
                .actif(true).plan(TenantEntity.Plan.STARTER).build();

        TenantAdminDto dto = TenantAdminDto.fromEntity(tenant, 0L, 0L, null, null);

        assertThat(dto.getAdminEmail()).isNull();
        assertThat(dto.getAdminNom()).isNull();
        assertThat(dto.getAdminPrenom()).isNull();
        assertThat(dto.getAdminTelephone()).isNull();
    }

    @Test
    @DisplayName("Infos admin mappées correctement depuis UserEntity")
    void fromEntity_avecAdmin_infosAdminMappees() {
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("t9").nomEntreprise("Boutique Test").numeroTelephone("0")
                .actif(true).plan(TenantEntity.Plan.STARTER)
                .essaiUtilise(true).build();

        TenantAdminDto dto = TenantAdminDto.fromEntity(tenant, 2L, 0L, admin, null);

        assertThat(dto.getAdminEmail()).isEqualTo("admin@boutique.com");
        assertThat(dto.getAdminNom()).isEqualTo("Diallo");
        assertThat(dto.getAdminPrenom()).isEqualTo("Amadou");
        assertThat(dto.getNbUtilisateurs()).isEqualTo(2L);
        assertThat(dto.getNomEntreprise()).isEqualTo("Boutique Test");
    }
}
