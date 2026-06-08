package com.example.dijasaliou.entity;

/**
 * Les comptes disponibles dans la caisse multi-comptes.
 *
 *   ESPECES        : argent physique dans la boîte
 *   WAVE           : solde sur Wave
 *   ORANGE_MONEY   : solde sur Orange Money
 *   VIREMENT       : solde sur le compte bancaire (alimenté par virements)
 */
public enum CompteCaisse {
    ESPECES,
    WAVE,
    ORANGE_MONEY,
    VIREMENT
}
