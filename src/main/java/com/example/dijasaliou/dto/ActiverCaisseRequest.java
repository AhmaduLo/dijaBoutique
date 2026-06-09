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

    /**
     * Date d'activation (optionnelle). Si fournie, c'est l'heure locale du
     * navigateur — évite tout problème de fuseau quand le serveur est dans
     * un autre pays. Si null, le backend utilise son heure locale.
     */
    private java.time.LocalDateTime dateActivation;
}
