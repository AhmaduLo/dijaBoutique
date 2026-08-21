package com.example.dijasaliou.controller;

import com.example.dijasaliou.annotation.RequiresPlan;
import com.example.dijasaliou.dto.CreerProductionRequest;
import com.example.dijasaliou.dto.ProductionDto;
import com.example.dijasaliou.entity.ProductionEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.ProductionService;
import com.example.dijasaliou.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Enregistrement des productions (produits fabriqués à partir d'ingrédients).
 */
@RestController
@RequestMapping("/productions")
public class ProductionController {

    private final ProductionService productionService;
    private final UserService userService;

    public ProductionController(ProductionService productionService, UserService userService) {
        this.productionService = productionService;
        this.userService = userService;
    }

    /**
     * GET /productions
     */
    @GetMapping
    public ResponseEntity<List<ProductionDto>> obtenirToutes() {
        List<ProductionDto> productions = productionService.listerProductions().stream()
                .map(ProductionDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(productions);
    }

    /**
     * GET /productions/par-achat/{achatId}
     * Retrouve la production liée à un lot d'achat — alimente le détail
     * "Ingrédients" affiché quand on ouvre un achat issu d'une production.
     * 404 si ce lot n'est pas issu d'une production (achat classique).
     */
    @GetMapping("/par-achat/{achatId}")
    public ResponseEntity<ProductionDto> obtenirParAchatId(@PathVariable String achatId) {
        return productionService.trouverParAchatId(achatId)
                .map(ProductionDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /productions/dernieres-ingredients?nomProduit=Thieboudienne
     * Ingrédients de la dernière production de ce nom, pour pré-remplir le
     * formulaire quand le commerçant retape un produit déjà fabriqué.
     * Retourne une liste vide (200) si aucune production antérieure n'existe.
     */
    @GetMapping("/dernieres-ingredients")
    public ResponseEntity<List<ProductionDto.IngredientLigneDto>> obtenirDernieresIngredients(
            @RequestParam String nomProduit) {
        List<ProductionDto.IngredientLigneDto> ingredients = productionService.trouverDerniereProduction(nomProduit)
                .map(ProductionDto::fromEntity)
                .map(ProductionDto::getIngredients)
                .orElse(List.of());
        return ResponseEntity.ok(ingredients);
    }

    /**
     * POST /productions
     * SÉCURITÉ : L'utilisateur est extrait du token JWT (Authentication),
     * pas d'un paramètre fourni par le client (protection IDOR).
     */
    @PostMapping
    @RequiresPlan(
            plans = {TenantEntity.Plan.STARTER},
            message = "La gestion de production est en test — pour l'instant réservée au plan Starter"
    )
    public ResponseEntity<ProductionDto> creer(
            @Valid @RequestBody CreerProductionRequest request,
            Authentication authentication) {

        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(authentication.getName());
        ProductionEntity production = productionService.creerProduction(request, utilisateur);

        return ResponseEntity.status(HttpStatus.CREATED).body(ProductionDto.fromEntity(production));
    }

    /**
     * PUT /productions/{id}
     * SÉCURITÉ : L'utilisateur est extrait du token JWT (Authentication),
     * pas d'un paramètre fourni par le client (protection IDOR).
     */
    @PutMapping("/{id}")
    @RequiresPlan(
            plans = {TenantEntity.Plan.STARTER},
            message = "La gestion de production est en test — pour l'instant réservée au plan Starter"
    )
    public ResponseEntity<ProductionDto> modifier(
            @PathVariable String id,
            @Valid @RequestBody CreerProductionRequest request,
            Authentication authentication) {

        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(authentication.getName());
        ProductionEntity production = productionService.modifierProduction(id, request, utilisateur);

        return ResponseEntity.ok(ProductionDto.fromEntity(production));
    }
}
