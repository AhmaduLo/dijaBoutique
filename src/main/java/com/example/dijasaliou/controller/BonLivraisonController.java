package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.BonLivraisonDto;
import com.example.dijasaliou.dto.CreateBonLivraisonRequest;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.service.BonLivraisonService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller Bons de livraison
 *
 * Routes :
 * GET    /bons-de-livraison          → liste tous les BL du tenant
 * GET    /bons-de-livraison/{id}     → détail d'un BL
 * POST   /bons-de-livraison          → créer un BL
 * PUT    /bons-de-livraison/{id}/livrer  → marquer comme livré
 * PUT    /bons-de-livraison/{id}/annuler → annuler
 * DELETE /bons-de-livraison/{id}     → supprimer
 */
@RestController
@RequestMapping("/bons-de-livraison")
@Slf4j
public class BonLivraisonController {

    private final BonLivraisonService bonLivraisonService;

    public BonLivraisonController(BonLivraisonService bonLivraisonService) {
        this.bonLivraisonService = bonLivraisonService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<BonLivraisonDto>> getTous(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "TOUS") String statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            Authentication auth) {
        log.info("[BL] {} consulte les bons de livraison", auth.getName());
        return ResponseEntity.ok(bonLivraisonService.getTousPagines(page, size, search, statut, dateDebut, dateFin));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BonLivraisonDto> getParId(@PathVariable Long id, Authentication auth) {
        log.info("[BL] {} consulte le BL {}", auth.getName(), id);
        return ResponseEntity.ok(bonLivraisonService.getParId(id));
    }

    @PostMapping
    public ResponseEntity<BonLivraisonDto> creer(
            @Valid @RequestBody CreateBonLivraisonRequest request,
            Authentication auth) {
        log.info("[BL] {} crée un BL pour le client '{}'", auth.getName(), request.getClientNom());
        BonLivraisonDto created = bonLivraisonService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/livrer")
    public ResponseEntity<BonLivraisonDto> marquerLivre(@PathVariable Long id, Authentication auth) {
        log.info("[BL] {} marque le BL {} comme livré", auth.getName(), id);
        return ResponseEntity.ok(bonLivraisonService.marquerLivre(id));
    }

    @PutMapping("/{id}/annuler")
    public ResponseEntity<BonLivraisonDto> annuler(@PathVariable Long id, Authentication auth) {
        log.info("[BL] {} annule le BL {}", auth.getName(), id);
        return ResponseEntity.ok(bonLivraisonService.annuler(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id, Authentication auth) {
        log.info("[BL] {} supprime le BL {}", auth.getName(), id);
        bonLivraisonService.supprimer(id);
        return ResponseEntity.ok(Map.of("message", "Bon de livraison supprimé"));
    }
}
