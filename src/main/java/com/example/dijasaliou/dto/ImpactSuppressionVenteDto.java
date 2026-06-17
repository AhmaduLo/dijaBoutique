package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aperçu des conséquences d'une suppression de vente.
 *
 * Envoyé au frontend avant confirmation pour qu'il affiche
 * une modale claire avec les chiffres réels (stock à restaurer,
 * caisse à débiter, CA à recalculer par mois affecté).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactSuppressionVenteDto {

    /** id de la vente concernée. */
    private String venteId;

    /** Nom du produit (pour rappel dans la modale). */
    private String nomProduit;

    /** Quantité qui sera restaurée au stock. */
    private Double quantiteARestaurer;

    /** Mode de paiement de la vente (CREDIT / ESPECES / etc.). */
    private String modePaiement;

    /** true si une vente à crédit, false sinon. */
    private boolean estVenteCredit;

    /** Statut du crédit associé (EN_ATTENTE / PARTIEL / SOLDE / PERTE / null). */
    private String creditStatut;

    /** Détail des paiements à annuler. */
    @Builder.Default
    private List<PaiementAAnnuler> paiementsAAnnuler = java.util.Collections.emptyList();

    /** Total à débiter de la caisse (= somme des paiements à annuler, par mode). */
    @Builder.Default
    private List<MontantParMode> totalParMode = java.util.Collections.emptyList();

    /** Impact CA par mois affecté : "CA mai était 1 500 000, deviendra 1 420 000". */
    @Builder.Default
    private List<ImpactCaMois> impactCaParMois = java.util.Collections.emptyList();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaiementAAnnuler {
        private String paiementId;
        private LocalDate datePaiement;
        private String modePaiement;  // ESPECES / WAVE / ORANGE_MONEY / VIREMENT
        private BigDecimal montant;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MontantParMode {
        private String modePaiement;
        private BigDecimal montant;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImpactCaMois {
        private Integer annee;
        private Integer mois;       // 1-12
        private String moisLibelle; // "Mai 2026"
        private BigDecimal caActuel;
        private BigDecimal caApres;
        private BigDecimal diminution;
    }
}
