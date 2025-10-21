package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.service.UserService;
import com.example.dijasaliou.service.VenteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour les ventes
 */
@RestController
@RequestMapping("/ventes")
@CrossOrigin(origins = "http://localhost:4200")
public class VenteController {

    private final VenteService venteService;
    private final UserService userService;

    public VenteController(VenteService venteService, UserService userService) {
        this.venteService = venteService;
        this.userService = userService;
    }

    /**
     * GET /api/ventes
     */
    @GetMapping
    public ResponseEntity<List<VenteEntity>> obtenirTous() {
        List<VenteEntity> ventes = venteService.obtenirToutesLesVentes();
        return ResponseEntity.ok(ventes);
    }

    /**
     * GET /api/ventes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<VenteEntity> obtenirParId(@PathVariable Long id) {
        VenteEntity vente = venteService.obtenirVenteParId(id);
        return ResponseEntity.ok(vente);
    }

    /**
     * POST /api/ventes
     */
    @PostMapping
    public ResponseEntity<VenteEntity> creer(
            @RequestBody VenteEntity vente,
            @RequestParam Long utilisateurId) {

        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);
        VenteEntity venteCree = venteService.creerVente(vente, utilisateur);

        return ResponseEntity.status(HttpStatus.CREATED).body(venteCree);
    }

    /**
     * PUT /api/ventes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<VenteEntity> modifier(
            @PathVariable Long id,
            @RequestBody VenteEntity venteModifiee) {

        VenteEntity vente = venteService.modifierVente(id, venteModifiee);
        return ResponseEntity.ok(vente);
    }

    /**
     * DELETE /api/ventes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        venteService.supprimerVente(id);
        return ResponseEntity.noContent().build();
    }
}
