package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Mode de paiement utilisé pour un achat ou une dépense (impact caisse).
 *
 *   ESPECES        : sort de la caisse Espèces
 *   WAVE           : sort du compte Wave
 *   ORANGE_MONEY   : sort du compte Orange Money
 *   VIREMENT       : virement bancaire — N'IMPACTE PAS la caisse (l'argent est sur un compte bancaire)
 *
 * Pour les ventes, on utilise ModePaiementVente (existant) avec en plus CREDIT.
 */
public enum ModePaiementCaisse {
    ESPECES,
    WAVE,
    ORANGE_MONEY,
    VIREMENT;

    @JsonCreator
    public static ModePaiementCaisse fromString(String value) {
        if (value == null) return null;
        return ModePaiementCaisse.valueOf(value.toUpperCase());
    }

    /**
     * Retourne le compte de caisse correspondant, ou null si VIREMENT
     * (pas de compte caisse pour le virement).
     */
    public CompteCaisse toCompteCaisse() {
        return switch (this) {
            case ESPECES      -> CompteCaisse.ESPECES;
            case WAVE         -> CompteCaisse.WAVE;
            case ORANGE_MONEY -> CompteCaisse.ORANGE_MONEY;
            case VIREMENT     -> null;
        };
    }
}
