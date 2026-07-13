package com.example.dijasaliou.dto;

import java.math.BigDecimal;

/**
 * Config utilisateur "seuil montant" — réutilisée pour VENTE_EMPLOYE,
 * SORTIE_CAISSE_IMPORTANTE, NOUVEAU_CREDIT.
 *
 * Schéma JSON : { "seuilMontant": 50000 }
 *
 * Le montant est stocké en BigDecimal pour rester cohérent avec les colonnes
 * de la base (prix, montants). Seuil = 0 → toutes les occurrences notifient
 * (pas de filtre par montant).
 */
public record SeuilMontantConfig(BigDecimal seuilMontant) {

    private static final BigDecimal DEFAULT_SEUIL = BigDecimal.ZERO;

    public SeuilMontantConfig {
        if (seuilMontant == null) seuilMontant = BigDecimal.ZERO;
        if (seuilMontant.signum() < 0) {
            throw new IllegalArgumentException("Le seuil doit être positif ou nul (reçu : " + seuilMontant + ")");
        }
    }

    public static SeuilMontantConfig defaults() {
        return new SeuilMontantConfig(DEFAULT_SEUIL);
    }
}
