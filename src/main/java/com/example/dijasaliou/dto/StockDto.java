package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour le stock d'un produit
 *
 * Calcule automatiquement le stock disponible :
 * Stock = Quantité achetée - Quantité vendue
 *
 * Exemple :
 * - Collier en or : 50 achetés, 30 vendus = 20 en stock
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDto {
    /**
     * Nom du produit
     */
    private String nomProduit;

    /**
     * URL de la photo du produit (provenant du dernier achat)
     */
    private String photoUrl;

    /**
     * Quantité totale achetée
     */
    private Integer quantiteAchetee;

    /**
     * Quantité totale vendue
     */
    private Integer quantiteVendue;

    /**
     * Stock disponible (achetée - vendue)
     */
    private Integer stockDisponible;

    /**
     * Prix unitaire moyen d'achat
     */
    private BigDecimal prixMoyenAchat;

    /**
     * Prix unitaire moyen de vente
     */
    private BigDecimal prixMoyenVente;

    /**
     * Valeur totale du stock (stock × prix moyen d'achat)
     */
    private BigDecimal valeurStock;

    /**
     * Marge moyenne par unité (prix vente - prix achat)
     */
    private BigDecimal margeUnitaire;

    /**
     * Statut du stock
     */
    private StatutStock statut;

    /**
     * Enum pour le statut du stock
     */
    public enum StatutStock {
        EN_STOCK("En stock", "Stock disponible"),
        STOCK_BAS("Stock bas", "Moins de 10 unités"),
        RUPTURE("Rupture", "Stock épuisé"),
        NEGATIF("Négatif", "Ventes supérieures aux achats");

        private final String libelle;
        private final String description;

        StatutStock(String libelle, String description) {
            this.libelle = libelle;
            this.description = description;
        }

        public String getLibelle() {
            return libelle;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Détermine le statut du stock selon la quantité disponible
     */
    public static StatutStock determinerStatut(Integer stockDisponible) {
        if (stockDisponible == null) {
            return StatutStock.RUPTURE;
        }
        if (stockDisponible < 0) {
            return StatutStock.NEGATIF;
        }
        if (stockDisponible == 0) {
            return StatutStock.RUPTURE;
        }
        if (stockDisponible < 10) {
            return StatutStock.STOCK_BAS;
        }
        return StatutStock.EN_STOCK;
    }

    /**
     * Vérifie si le stock est suffisant pour une vente
     */
    public boolean estSuffisant(Integer quantiteDemandee) {
        return stockDisponible != null && stockDisponible >= quantiteDemandee;
    }

    /**
     * Calcule le pourcentage de stock restant
     */
    public Double getPourcentageStock() {
        if (quantiteAchetee == null || quantiteAchetee == 0) {
            return 0.0;
        }
        return (stockDisponible.doubleValue() / quantiteAchetee.doubleValue()) * 100;
    }
}
