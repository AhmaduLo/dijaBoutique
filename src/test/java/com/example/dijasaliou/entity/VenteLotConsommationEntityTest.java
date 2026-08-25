package com.example.dijasaliou.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires — VenteLotConsommationEntity.recalculerBenefice")
class VenteLotConsommationEntityTest {

    @Test
    @DisplayName("Achat et vente dans la même devise — comportement inchangé (soustraction directe)")
    void recalculerBenefice_memeDevise_soustractionDirecte() {
        AchatEntity achat = AchatEntity.builder()
                .prixUnitaire(new BigDecimal("100000"))
                .tauxChangeApplique(1.0)
                .build();
        VenteEntity vente = VenteEntity.builder()
                .prixUnitaire(new BigDecimal("150000"))
                .tauxChangeApplique(1.0)
                .build();

        VenteLotConsommationEntity conso = VenteLotConsommationEntity.builder()
                .vente(vente)
                .achat(achat)
                .quantiteConsommee(1.0)
                .prixAchatUnitaireSnapshot(achat.getPrixUnitaire())
                .prixVenteUnitaireSnapshot(vente.getPrixUnitaire())
                .dateVenteSnapshot(LocalDateTime.now())
                .build();

        conso.recalculerBenefice();

        assertThat(conso.getBeneficeUnitaire()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(conso.getBeneficeTotalLigne()).isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    @DisplayName("Achat en XOF, vente en EUR — le bénéfice reste positif et cohérent, pas une soustraction brute")
    void recalculerBenefice_devisesDifferentes_convertitAvantSoustraction() {
        // Achat : 100 000 XOF (taux 1.0, XOF = référence)
        AchatEntity achat = AchatEntity.builder()
                .prixUnitaire(new BigDecimal("100000"))
                .tauxChangeApplique(1.0)
                .build();
        // Vente : 150 EUR (taux EUR = 655.957, càd 1 EUR = 655.957 XOF)
        VenteEntity vente = VenteEntity.builder()
                .prixUnitaire(new BigDecimal("150"))
                .tauxChangeApplique(655.957)
                .build();

        VenteLotConsommationEntity conso = VenteLotConsommationEntity.builder()
                .vente(vente)
                .achat(achat)
                .quantiteConsommee(1.0)
                .prixAchatUnitaireSnapshot(achat.getPrixUnitaire())
                .prixVenteUnitaireSnapshot(vente.getPrixUnitaire())
                .dateVenteSnapshot(LocalDateTime.now())
                .build();

        conso.recalculerBenefice();

        // AVANT le fix : 150 - 100000 = -99850 (faux, bug reproduit par l'utilisateur)
        // APRES le fix : achat converti dans la devise de la vente : 100000 XOF / 655.957 ≈ 152.45 EUR
        // benefice = 150 - 152.45 ≈ -2.45 EUR (une petite perte réelle, pas -99850)
        assertThat(conso.getBeneficeUnitaire()).isEqualByComparingTo(new BigDecimal("-2.45"));
        assertThat(conso.getBeneficeUnitaire()).isGreaterThan(new BigDecimal("-100"));
    }
}
