package com.example.dijasaliou.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Corps de requête pour enregistrer une production (POST /productions).
 * <p>
 * DTO dédié (pas l'AchatEntity brut comme AchatController.creer) car une
 * production doit créer atomiquement, en une seule requête : 1 lot achats
 * + 1 enregistrement production + N lignes d'ingrédients.
 * <p>
 * Le produit fabriqué est identifié par son NOM (saisie libre côté
 * frontend, comme pour un achat classique) — s'il n'existe pas encore
 * pour ce tenant, il est créé automatiquement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreerProductionRequest {

    @NotBlank(message = "Le nom du produit fabriqué est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit faire entre 2 et 100 caractères")
    private String nomProduit;

    @NotNull(message = "La quantité produite est obligatoire")
    @Positive(message = "La quantité produite doit être supérieure à 0")
    private Double quantiteProduite;

    private LocalDateTime dateProduction;

    @Size(max = 200, message = "Les notes ne peuvent dépasser 200 caractères")
    private String notes;

    @Size(max = 500, message = "L'URL de la photo ne peut dépasser 500 caractères")
    private String photoUrl;

    /**
     * Prix d'achat (coût unitaire) saisi/modifié par le commerçant. Si présent
     * et &gt; 0, PRIME sur le calcul automatique (total ingrédients / quantité)
     * — c'est ce prix qui devient le coût réel du lot de stock généré.
     */
    @DecimalMin(value = "0.01", message = "Le prix d'achat doit être supérieur à 0")
    private BigDecimal prixUnitaire;

    /** Prix de vente suggéré pour ce produit fabriqué, optionnel. */
    @DecimalMin(value = "0.01", message = "Le prix de vente suggéré doit être supérieur à 0")
    private BigDecimal prixVenteSuggere;

    @NotEmpty(message = "Au moins un ingrédient est requis")
    @Valid
    private List<IngredientLigne> ingredients;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientLigne {

        @NotBlank(message = "Le nom de l'ingrédient est obligatoire")
        @Size(max = 150, message = "Le nom de l'ingrédient ne peut dépasser 150 caractères")
        private String nomIngredient;

        @Size(max = 20, message = "L'unité ne peut dépasser 20 caractères")
        private String unite;

        @NotNull(message = "Le coût de l'ingrédient est obligatoire")
        @DecimalMin(value = "0.01", message = "Le coût doit être supérieur à 0")
        private BigDecimal cout;
    }
}
