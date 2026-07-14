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
     * Code-barre du produit (EAN-13, EAN-8, UPC-A, etc.)
     */
    private String codeBarre;

    /**
     * Unité de mesure du produit (pièce, kg, litre, mètre, etc.)
     */
    private String unite;

    /**
     * Catégorie du produit — remontée depuis le dernier achat non vide.
     * Utilisée par l'autocomplétion nom-produit en multi-achat pour
     * pré-remplir la catégorie du nouvel achat.
     */
    private String categorie;

    /**
     * Quantité totale achetée
     */
    private Double quantiteAchetee;

    /**
     * Quantité totale vendue
     */
    private Double quantiteVendue;

    /**
     * Stock disponible (achetée - vendue)
     */
    private Double stockDisponible;

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
     * Bénéfice net total réalisé sur ce produit (FIFO).
     * Somme des benefice_total_ligne des lignes vente_lot_consommation.
     * Inclut UNIQUEMENT les ventes qui ont eu un calcul FIFO valide.
     */
    private BigDecimal beneficeTotal;

    /**
     * Quantité vendue avec un bénéfice calculé (peut être < quantiteVendue
     * si certaines ventes n'ont pas trouvé de lot d'achat).
     */
    private Double quantiteVendueAvecBenefice;

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
    public static StatutStock determinerStatut(Double stockDisponible) {
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
    public boolean estSuffisant(Double quantiteDemandee) {
        return stockDisponible != null && stockDisponible >= quantiteDemandee;
    }

    /**
     * Calcule le pourcentage de stock restant
     */
    public Double getPourcentageStock() {
        if (quantiteAchetee == null || quantiteAchetee == 0) {
            return 0.0;
        }
        return (stockDisponible / quantiteAchetee) * 100;
    }
}
