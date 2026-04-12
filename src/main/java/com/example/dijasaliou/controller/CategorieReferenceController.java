package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.CategorieReferenceEntity;
import com.example.dijasaliou.repository.CategorieReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CategorieReferenceController {

    private final CategorieReferenceRepository categorieRepository;

    /**
     * GET /api/categories-reference
     * Liste des catégories — accessible à tous les utilisateurs authentifiés.
     * Le frontend affiche cette liste dans un menu déroulant.
     */
    @GetMapping("/categories-reference")
    public ResponseEntity<List<CategorieReferenceEntity>> lister() {
        return ResponseEntity.ok(categorieRepository.findAllByOrderByOrdreAsc());
    }

    /**
     * POST /api/superadmin/categories-reference
     * Ajouter une catégorie — super admin uniquement.
     */
    @PostMapping("/superadmin/categories-reference")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> ajouter(@RequestBody Map<String, Object> body) {
        String nom = (String) body.get("nom");
        if (nom == null || nom.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le nom est obligatoire"));
        }
        if (categorieRepository.existsByNom(nom.trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Cette catégorie existe déjà"));
        }

        Integer ordre = body.get("ordre") != null ? ((Number) body.get("ordre")).intValue() : 0;

        CategorieReferenceEntity categorie = CategorieReferenceEntity.builder()
                .nom(nom.trim())
                .ordre(ordre)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(categorieRepository.save(categorie));
    }

    /**
     * PUT /api/superadmin/categories-reference/{id}
     * Modifier une catégorie — super admin uniquement.
     */
    @PutMapping("/superadmin/categories-reference/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> modifier(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        CategorieReferenceEntity categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));

        String nom = (String) body.get("nom");
        if (nom != null && !nom.isBlank()) {
            categorie.setNom(nom.trim());
        }
        if (body.get("ordre") != null) {
            categorie.setOrdre(((Number) body.get("ordre")).intValue());
        }
        return ResponseEntity.ok(categorieRepository.save(categorie));
    }

    /**
     * DELETE /api/superadmin/categories-reference/{id}
     * Supprimer une catégorie — super admin uniquement.
     */
    @DeleteMapping("/superadmin/categories-reference/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        if (!categorieRepository.existsById(id)) {
            throw new RuntimeException("Catégorie non trouvée");
        }
        categorieRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
