package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
