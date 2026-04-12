package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour le résultat d'un lookup de code-barre.
 *
 * source :
 * - "LOCAL"           → produit trouvé dans la base HeasyStock du tenant
 * - "OPEN_FOOD_FACTS" → produit trouvé via l'API Open Food Facts
 * - null              → produit non trouvé (trouve = false)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeBarreLookupDto {

    private boolean trouve;
    private String codeBarre;
    private String source;

    // Infos produit (préremplissage)
    private String nomProduit;
    private String photoUrl;
    private String categorie;
    private String marque;
    private String unite;

    // Infos prix (uniquement si source = LOCAL)
    private BigDecimal prixUnitaire;
    private BigDecimal prixVenteSuggere;
    private Double stockDisponible;
}
