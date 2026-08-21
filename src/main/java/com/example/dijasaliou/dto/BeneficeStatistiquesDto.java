package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Statistiques de bénéfice net sur une période.
 *
 * Calcul :
 *   - chiffreAffaires  : somme des prix_total des ventes de la période
 *   - totalCoutAchat   : somme des (prix_achat × quantité) des lignes FIFO de la période
 *   - beneficeNet      : chiffreAffaires − totalCoutAchat
 *                        (équivaut à SUM(benefice_total_ligne) de vente_lot_consommation)
 *   - margePourcentage : (beneficeNet / chiffreAffaires) × 100
 *
 * Le champ nbVentesSansBenefice compte les ventes de la période qui n'ont
 * AUCUNE ligne de consommation (produit jamais acheté ou stock initial sans achat).
 *
 * Les pertes (sorties hors vente : vol, casse, don, crédit impayé) sont
 * comptabilisées séparément dans totalPertes / pertesParType pour ne pas
 * polluer le bénéfice commercial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficeStatistiquesDto {

    private LocalDate   dateDebut;
    private LocalDate   dateFin;

    private BigDecimal  chiffreAffaires;
    private BigDecimal  totalCoutAchat;
    private BigDecimal  beneficeNet;
    private BigDecimal  margePourcentage;

    private long        nbVentes;
    private long        nbVentesAvecBenefice;
    private long        nbVentesSansBenefice;

    /**
     * Total des pertes (coût FIFO) sur la période, toutes catégories confondues.
     * = ce que tu as payé pour des marchandises qui sont parties sans contrepartie.
     */
    private BigDecimal  totalPertes;

    /**
     * Détail des pertes par type : clé = "PERTE_CASSE" / "VOL" / "OFFERT" / "CREDIT_IMPAYE" / "AUTRE",
     * valeur = coût FIFO de cette catégorie.
     */
    private Map<String, BigDecimal> pertesParType;

    /**
     * Nombre de sorties hors vente sur la période.
     */
    private long        nbSorties;
}
