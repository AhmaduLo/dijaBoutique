package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Solde actuel de la caisse (multi-comptes).
 *
 * Retourné par GET /api/caisse.
 *   - active = true  : la caisse a été activée, soldes calculés
 *   - active = false : pas encore activée (champ soldeXxx = null)
 *
 * Le calcul du solde par compte :
 *   solde_compte = solde_initial
 *                + ventes en mode_paiement = compte (depuis date_activation)
 *                - achats en mode_paiement = compte
 *                - dépenses en mode_paiement = compte
 *                + transferts entrants vers ce compte
 *                - transferts sortants depuis ce compte
 *                + mouvements manuels ENTREE sur ce compte
 *                - mouvements manuels SORTIE sur ce compte
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaisseSoldeDto {

    private boolean       active;
    private LocalDateTime dateActivation;

    private BigDecimal    soldeEspeces;
    private BigDecimal    soldeWave;
    private BigDecimal    soldeOm;
    private BigDecimal    soldeTotal;

    private BigDecimal    soldeInitialEspeces;
    private BigDecimal    soldeInitialWave;
    private BigDecimal    soldeInitialOm;
}
