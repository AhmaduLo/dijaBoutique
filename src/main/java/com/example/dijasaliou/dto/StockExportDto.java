package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO enrichi pour l'export de stock : combine l'état actuel du stock
 * (StockDto) avec l'activité sur une période choisie.
 *
 *  - Le stock affiché reste celui d'AUJOURD'HUI (réalité physique).
 *  - quantiteAcheteePeriode / quantiteVenduePeriode = activité dans la période choisie.
 *  - Un produit n'apparaît dans l'export que s'il a eu de l'activité dans la période.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockExportDto {

    private String nomProduit;
    private String codeBarre;
    private String unite;

    private Double      quantiteAchetee;          // total historique
    private Double      quantiteVendue;           // total historique
    private Double      stockDisponible;          // actuel
    private BigDecimal  prixMoyenAchat;
    private BigDecimal  prixMoyenVente;
    private BigDecimal  valeurStock;              // stock × prixMoyenAchat
    private BigDecimal  margeUnitaire;
    private BigDecimal  beneficeTotal;            // FIFO

    private StockDto.StatutStock statut;

    /** Quantité achetée pendant la période sélectionnée (peut être 0). */
    private Double quantiteAcheteePeriode;

    /** Quantité vendue pendant la période sélectionnée (peut être 0). */
    private Double quantiteVenduePeriode;
}
