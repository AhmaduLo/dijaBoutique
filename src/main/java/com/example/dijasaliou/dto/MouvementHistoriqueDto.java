package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.CompteCaisse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO unifié pour l'historique des mouvements de caisse manuels :
 *   - ENTREE  : entrée d'argent sur un compte (motif libre)
 *   - SORTIE  : sortie d'argent d'un compte (motif libre)
 *   - TRANSFERT : déplacement entre 2 comptes (compteSource → compteDestination)
 *
 * Format unifié pour faciliter l'affichage chronologique côté UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MouvementHistoriqueDto {

    public enum TypeHistorique {
        ENTREE,
        SORTIE,
        TRANSFERT
    }

    private String          id;
    private TypeHistorique  type;

    /** Pour ENTREE / SORTIE — le compte impacté. NULL pour TRANSFERT. */
    private CompteCaisse    compte;

    /** Pour TRANSFERT — compte source. NULL pour ENTREE/SORTIE. */
    private CompteCaisse    compteSource;

    /** Pour TRANSFERT — compte destination. NULL pour ENTREE/SORTIE. */
    private CompteCaisse    compteDestination;

    private BigDecimal      montant;
    private String          motif;
    private LocalDateTime   date;
    private String          faitPar;
}
