package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.ProduitFabriqueDto;
import com.example.dijasaliou.service.ProduitFabriqueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Catalogue des produits fabriqués (issus de production) — alimente
 * l'autocomplétion du champ "nom du produit" dans le formulaire
 * "Nouvelle production". La création se fait implicitement lors de
 * l'enregistrement d'une production (voir ProductionController), il n'y a
 * pas d'endpoint de création dédié.
 */
@RestController
@RequestMapping("/produits-fabriques")
public class ProduitFabriqueController {

    private final ProduitFabriqueService produitFabriqueService;

    public ProduitFabriqueController(ProduitFabriqueService produitFabriqueService) {
        this.produitFabriqueService = produitFabriqueService;
    }

    /**
     * GET /produits-fabriques
     */
    @GetMapping
    public ResponseEntity<List<ProduitFabriqueDto>> obtenirTous() {
        List<ProduitFabriqueDto> produits = produitFabriqueService.listerProduitsFabriquesActifs().stream()
                .map(ProduitFabriqueDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(produits);
    }
}
