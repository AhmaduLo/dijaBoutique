package com.example.dijasaliou.entity;

/**
 * Les comptes disponibles dans la caisse multi-comptes.
 *
 *   ESPECES        : argent physique dans la boîte
 *   WAVE           : solde sur Wave
 *   ORANGE_MONEY   : solde sur Orange Money
 *
 * Note : VIREMENT bancaire n'est pas un compte de caisse (l'argent est ailleurs),
 * mais peut être utilisé comme mode de paiement pour ne PAS impacter la caisse.
 */
public enum CompteCaisse {
    ESPECES,
    WAVE,
    ORANGE_MONEY
}
