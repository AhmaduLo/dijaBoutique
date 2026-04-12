package com.example.dijasaliou.controller;

import com.example.dijasaliou.annotation.RequiresPlan;
import com.example.dijasaliou.dto.CodeBarreLookupDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.ProduitReferenceEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.service.OpenFoodFactsService;
import com.example.dijasaliou.service.ProduitReferenceService;
import com.example.dijasaliou.service.StockService;
import com.example.dijasaliou.service.TenantService;
import com.example.dijasaliou.dto.StockDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour le scan de code-barre.
 *
 * Flux :
 * 1. Chercher dans la base locale HeasyStock (produits du tenant)
 * 2. Si pas trouvé → chercher sur Open Food Facts (API externe)
 * 3. Si pas trouvé → retourner { trouve: false, codeBarre: "xxx" }
 */
@RestController
@RequestMapping("/codebarre")
@RequiredArgsConstructor
public class CodeBarreController {

    private final AchatRepository achatRepository;
    private final TenantService tenantService;
    private final OpenFoodFactsService openFoodFactsService;
    private final ProduitReferenceService produitReferenceService;
    private final StockService stockService;

    /**
     * GET /api/codebarre/lookup/{code}
     *
     * Recherche complète : base locale → Open Food Facts → non trouvé.
     * Utilisé lors de l'ajout d'un nouvel achat (préremplissage).
     */
    @GetMapping("/lookup/{code}")
    @RequiresPlan(plans = {TenantEntity.Plan.PRO},
                  message = "Le scan code-barre est réservé au plan Pro. Passez au plan Pro pour débloquer cette fonctionnalité.")
    public ResponseEntity<CodeBarreLookupDto> lookup(@PathVariable String code) {
        String codeBarre = code.trim();

        // 1. Chercher en local (base du tenant)
        CodeBarreLookupDto local = rechercherEnLocal(codeBarre);
        if (local.isTrouve()) {
            return ResponseEntity.ok(local);
        }

        // 2. Chercher dans la base partagée (communauté HeasyStock)
        var refOpt = produitReferenceService.rechercherParCodeBarre(codeBarre);
        if (refOpt.isPresent()) {
            ProduitReferenceEntity ref = refOpt.get();
            return ResponseEntity.ok(CodeBarreLookupDto.builder()
                    .trouve(true)
                    .codeBarre(codeBarre)
                    .source("COMMUNAUTE")
                    .nomProduit(ref.getNomProduit())
                    .photoUrl(ref.getPhotoUrl())
                    .categorie(ref.getCategorie())
                    .build());
        }

        // 3. Chercher sur Open Food Facts
        CodeBarreLookupDto externe = openFoodFactsService.rechercherParCodeBarre(codeBarre);
        return ResponseEntity.ok(externe);
    }

    /**
     * GET /api/codebarre/recherche/{code}
     *
     * Recherche locale uniquement (rapide, sans appel réseau).
     * Utilisé lors d'une vente : on scanne → on retrouve le produit instantanément.
     */
    @GetMapping("/recherche/{code}")
    @RequiresPlan(plans = {TenantEntity.Plan.PRO},
                  message = "Le scan code-barre est réservé au plan Pro. Passez au plan Pro pour débloquer cette fonctionnalité.")
    public ResponseEntity<CodeBarreLookupDto> recherche(@PathVariable String code) {
        String codeBarre = code.trim();

        // 1. Chercher en local
        CodeBarreLookupDto result = rechercherEnLocal(codeBarre);
        if (result.isTrouve()) {
            return ResponseEntity.ok(result);
        }

        // 2. Chercher dans la base partagée (pas d'appel réseau)
        var refOpt = produitReferenceService.rechercherParCodeBarre(codeBarre);
        if (refOpt.isPresent()) {
            ProduitReferenceEntity ref = refOpt.get();
            return ResponseEntity.ok(CodeBarreLookupDto.builder()
                    .trouve(true)
                    .codeBarre(codeBarre)
                    .source("COMMUNAUTE")
                    .nomProduit(ref.getNomProduit())
                    .photoUrl(ref.getPhotoUrl())
                    .categorie(ref.getCategorie())
                    .build());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Recherche un produit par code-barre dans la base locale du tenant.
     * Retourne les infos du dernier achat correspondant + stock disponible.
     */
    private CodeBarreLookupDto rechercherEnLocal(String codeBarre) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        List<AchatEntity> achats = achatRepository.findByCodeBarreAndTenant(codeBarre, tenant);

        if (achats.isEmpty()) {
            return CodeBarreLookupDto.builder()
                    .trouve(false)
                    .codeBarre(codeBarre)
                    .build();
        }

        // Prendre le dernier achat (le plus récent — la liste est triée DESC)
        AchatEntity dernier = achats.get(0);

        // Récupérer le stock disponible pour ce produit
        Double stockDisponible = null;
        try {
            StockDto stock = stockService.obtenirStockParNomProduit(dernier.getNomProduit());
            stockDisponible = stock.getStockDisponible();
        } catch (RuntimeException ignored) {
            // Pas de stock trouvé — pas grave
        }

        return CodeBarreLookupDto.builder()
                .trouve(true)
                .codeBarre(codeBarre)
                .source("LOCAL")
                .nomProduit(dernier.getNomProduit())
                .photoUrl(dernier.getPhotoUrl())
                .unite(dernier.getUnite())
                .prixUnitaire(dernier.getPrixUnitaire())
                .prixVenteSuggere(dernier.getPrixVenteSuggere())
                .stockDisponible(stockDisponible)
                .build();
    }
}
