package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.ProduitReferenceEntity;
import com.example.dijasaliou.service.ProduitReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints Super Admin pour gérer la base partagée de codes-barres.
 */
@RestController
@RequestMapping("/superadmin/produits-reference")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class ProduitReferenceController {

    private final ProduitReferenceService produitReferenceService;

    /**
     * GET /api/superadmin/produits-reference/stats
     * Dashboard : nombre total, avec photo, sans catégorie, ajoutés cette semaine.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(produitReferenceService.obtenirStats());
    }

    /**
     * GET /api/superadmin/produits-reference?page=0&size=20&search=café
     * Liste paginée avec recherche par nom ou code-barre.
     */
    @GetMapping
    public ResponseEntity<Page<ProduitReferenceEntity>> lister(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        if (size > 100) size = 100;
        return ResponseEntity.ok(produitReferenceService.lister(page, size, search));
    }

    /**
     * PUT /api/superadmin/produits-reference/{id}
     * Modifier nom, photo ou catégorie d'un produit référencé.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProduitReferenceEntity> modifier(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ProduitReferenceEntity updated = produitReferenceService.modifier(
                id,
                body.get("nomProduit"),
                body.get("photoUrl"),
                body.get("categorie")
        );
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/superadmin/produits-reference/{id}
     * Supprimer un produit référencé (doublon, erreur).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        produitReferenceService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
