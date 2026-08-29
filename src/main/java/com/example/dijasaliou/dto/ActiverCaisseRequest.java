package com.example.dijasaliou.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Requête pour activer la caisse (1ère fois) ou corriger son solde actuel
 * (ex: après un comptage physique).
 *
 * Les champs soldeInitial* représentent le TOTAL ACTUEL VOULU par compte,
 * exprimé dans {@link #devise} — pas nécessairement un "solde initial" figé.
 * Voir {@link com.example.dijasaliou.service.CaisseService#activerCaisse}.
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
     * Code de la devise dans laquelle les montants ci-dessus sont exprimés
     * (ex: "EUR", "XOF"). Optionnel — XOF par défaut si absent.
     */
    private String devise;

    /**
     * Date d'activation (optionnelle). Si fournie, c'est l'heure locale du
     * navigateur — évite tout problème de fuseau quand le serveur est dans
     * un autre pays, et force une réinitialisation volontaire du suivi. Si
     * null, le backend conserve la date déjà active (ou l'initialise à
     * maintenant lors de la toute première activation).
     */
    private java.time.LocalDateTime dateActivation;
}
