package com.example.dijasaliou.dto;

import java.math.BigDecimal;

/**
 * Corps de la requête POST /superadmin/tenants/{id}/valider-paiement
 * periode : "MENSUEL" (défaut) ou "ANNUEL"
 */
public record ValiderPaiementRequest(
        String plan,
        BigDecimal montant,
        String moisDebut,
        String modePaiement,
        String periode,
        String note
) {}
