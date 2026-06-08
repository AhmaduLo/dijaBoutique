package com.example.dijasaliou.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Requête pour activer la caisse pour la 1ère fois.
 *
 * Le commerçant saisit les soldes qu'il a physiquement à l'instant t.
 * Tous les achats/ventes/dépenses antérieurs sont ignorés.
 */
@Data
public class ActiverCaisseRequest {

    @NotNull
    @PositiveOrZero
    private BigDecimal soldeInitialEspeces;

    @NotNull
    @PositiveOrZero
    private BigDecimal soldeInitialWave;

    @NotNull
    @PositiveOrZero
    private BigDecimal soldeInitialOm;

    @NotNull
    @PositiveOrZero
    private BigDecimal soldeInitialVirement;
}
