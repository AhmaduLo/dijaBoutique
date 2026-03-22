package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.ClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import com.example.dijasaliou.entity.TenantEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires — CreditClientDto.fromEntity()")
class CreditClientDtoTest {

    private final TenantEntity tenant = TenantEntity.builder()
            .tenantUuid("uuid-credit")
            .nomEntreprise("Boutique Test")
            .numeroTelephone("+221771111111")
            .build();

    private final ClientEntity client = ClientEntity.builder()
            .nom("Aminata Diallo")
            .telephone("771234567")
            .tenant(tenant)
            .build();

    private CreditClientEntity credit(BigDecimal initial, BigDecimal restant) {
        return CreditClientEntity.builder()
                .client(client)
                .montantInitial(initial)
                .montantRestant(restant)
                .statut(StatutCredit.EN_ATTENTE)
                .tenant(tenant)
                .build();
    }

    // ==================== montantPaye ====================

    @Test
    @DisplayName("montantPaye = montantInitial − montantRestant")
    void fromEntity_montantPaye_calculeCorrectement() {
        CreditClientDto dto = CreditClientDto.fromEntity(
                credit(new BigDecimal("10000"), new BigDecimal("3000")));

        assertThat(dto.getMontantPaye()).isEqualByComparingTo(new BigDecimal("7000"));
    }

    @Test
    @DisplayName("Crédit non commencé : montantPaye=0")
    void fromEntity_creditNonCommence_montantPayeZero() {
        CreditClientDto dto = CreditClientDto.fromEntity(
                credit(new BigDecimal("5000"), new BigDecimal("5000")));

        assertThat(dto.getMontantPaye()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== pourcentageRembourse ====================

    @Test
    @DisplayName("70% remboursé — arrondi correct (7000 payé sur 10000)")
    void fromEntity_pourcentage70_calculeCorrectement() {
        CreditClientDto dto = CreditClientDto.fromEntity(
                credit(new BigDecimal("10000"), new BigDecimal("3000")));

        assertThat(dto.getPourcentageRembourse()).isEqualTo(70);
    }

    @Test
    @DisplayName("100% remboursé — pourcentage=100")
    void fromEntity_creditSolde_pourcentage100() {
        CreditClientDto dto = CreditClientDto.fromEntity(
                credit(new BigDecimal("10000"), new BigDecimal("0")));

        assertThat(dto.getPourcentageRembourse()).isEqualTo(100);
    }

    @Test
    @DisplayName("0% remboursé — pourcentage=0")
    void fromEntity_creditNonPaye_pourcentage0() {
        CreditClientDto dto = CreditClientDto.fromEntity(
                credit(new BigDecimal("10000"), new BigDecimal("10000")));

        assertThat(dto.getPourcentageRembourse()).isEqualTo(0);
    }

    @Test
    @DisplayName("montantInitial=0 — division par zéro protégée, pourcentage=0")
    void fromEntity_montantInitialZero_pourcentageZero() {
        CreditClientDto dto = CreditClientDto.fromEntity(
                credit(BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(dto.getPourcentageRembourse()).isEqualTo(0);
    }

    @Test
    @DisplayName("Arrondi HALF_UP — 6667 payé sur 10000 → 67% (pas 66)")
    void fromEntity_arrondiHalfUp() {
        // 6667 / 10000 = 66.67% → arrondi à 67
        CreditClientDto dto = CreditClientDto.fromEntity(
                credit(new BigDecimal("10000"), new BigDecimal("3333")));

        assertThat(dto.getPourcentageRembourse()).isEqualTo(67);
    }

    // ==================== Champs clientNom / statut ====================

    @Test
    @DisplayName("clientNom et clientTelephone mappés depuis l'entité client")
    void fromEntity_clientNomEtTelephone_mappes() {
        CreditClientDto dto = CreditClientDto.fromEntity(
                credit(new BigDecimal("5000"), new BigDecimal("2000")));

        assertThat(dto.getClientNom()).isEqualTo("Aminata Diallo");
        assertThat(dto.getClientTelephone()).isEqualTo("771234567");
    }

    @Test
    @DisplayName("statut mappé en String depuis l'enum")
    void fromEntity_statutString_mappéDepuisEnum() {
        CreditClientEntity creditSolde = CreditClientEntity.builder()
                .client(client)
                .montantInitial(new BigDecimal("5000"))
                .montantRestant(BigDecimal.ZERO)
                .statut(StatutCredit.SOLDE)
                .tenant(tenant)
                .build();

        CreditClientDto dto = CreditClientDto.fromEntity(creditSolde);
        assertThat(dto.getStatut()).isEqualTo("SOLDE");
    }

    @Test
    @DisplayName("paiements vide par défaut → liste vide dans le DTO")
    void fromEntity_paiementsVide_listVideDansDtos() {
        CreditClientDto dto = CreditClientDto.fromEntity(
                credit(new BigDecimal("5000"), new BigDecimal("5000")));

        assertThat(dto.getPaiements()).isNotNull().isEmpty();
    }
}
