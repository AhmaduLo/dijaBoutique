package com.example.dijasaliou.dto;

import java.math.BigDecimal;

/**
 * Corps de la requête POST /superadmin/tenants/{id}/valider-paiement
 */
public record ValiderPaiementRequest(
        String plan,
        BigDecimal montant,
        String moisDebut,
        String modePaiement,
        String note
) {}
