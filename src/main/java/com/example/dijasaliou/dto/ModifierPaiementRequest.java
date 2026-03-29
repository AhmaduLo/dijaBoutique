package com.example.dijasaliou.dto;

import java.math.BigDecimal;

/**
 * Corps de la requête PUT /superadmin/paiements/{id}
 */
public record ModifierPaiementRequest(
        String plan,
        BigDecimal montant,
        String modePaiement,
        String note
) {}
