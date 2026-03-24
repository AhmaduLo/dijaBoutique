package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.AchatDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.ProduitPourVenteDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.AchatService;
import com.example.dijasaliou.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller REST pour les achats
 *
 * @RestController : Dit à Spring que c'est un controller REST (retourne du JSON)
 * @RequestMapping : Préfixe pour toutes les routes de ce controller
 * @CrossOrigin : Permet à Angular (localhost:4200) d'appeler cette API
 */
@RestController
@RequestMapping("/achats")

public class AchatController {

    private final AchatService achatService;
    private final UserService userService;

    public AchatController(AchatService achatService, UserService userService) {
        this.achatService = achatService;
        this.userService = userService;
    }

    /**
     * GET /api/achats?page=0&size=20&search=xxx&dateDebut=2025-01-01&dateFin=2025-12-31
     */
    @GetMapping
    public ResponseEntity<PagedResponse<AchatDto>> obtenirTous(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(achatService.obtenirAchatsPagines(page, size, search, dateDebut, dateFin));
    }

    /**
     * GET /api/achats/{id}
     * Récupérer un achat par son ID
     *
     * @PathVariable : Récupère {id} de l'URL
     *
     * Exemple : GET /api/achats/5
     *           → id = 5
     */
    @GetMapping("/{id}")
    public ResponseEntity<AchatDto> obtenirParId(@PathVariable String id) {
        AchatEntity achat = achatService.obtenirAchatParId(id);
        return ResponseEntity.ok(AchatDto.fromEntity(achat));
    }

    /**
     * GET /api/achats/utilisateur/{utilisateurId}
     * Récupérer tous les achats d'un utilisateur spécifique
     *
     * Exemple : GET /api/achats/utilisateur/1
     */
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<AchatDto>> obtenirAchatsParUtilisateur(@PathVariable Long utilisateurId) {
        // 1. Récupérer l'utilisateur depuis la base
        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);

        // 2. Récupérer ses achats
        List<AchatEntity> achats = achatService.obtenirAchatsParUtilisateur(utilisateur);

        // 3. Convertir en DTOs et retourner
        List<AchatDto> achatsDto = achats.stream()
                .map(AchatDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(achatsDto);
    }

    /**
     * POST /api/achats
     * SÉCURITÉ : L'utilisateur est extrait du token JWT (Authentication),
     * pas d'un paramètre fourni par le client (protection IDOR).
     */
    @PostMapping
    public ResponseEntity<AchatDto> creer(
            @Valid @RequestBody AchatEntity achat,
            Authentication authentication) {

        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(authentication.getName());
        AchatEntity achatCree = achatService.creerAchat(achat, utilisateur);

        return ResponseEntity.status(HttpStatus.CREATED).body(AchatDto.fromEntity(achatCree));
    }

    /**
     * PUT /api/achats/{id}
     * SÉCURITÉ : L'utilisateur est extrait du token JWT (Authentication),
     * pas d'un paramètre fourni par le client (protection IDOR).
     */
    @PutMapping("/{id}")
    public ResponseEntity<AchatDto> modifier(
            @PathVariable String id,
            @Valid @RequestBody AchatEntity achatModifie,
            Authentication authentication) {

        // Récupérer l'utilisateur authentifié (depuis le JWT, pas d'un paramètre client)
        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(authentication.getName());

        // Mettre à jour l'utilisateur de l'achat
        achatModifie.setUtilisateur(utilisateur);

        // Sauvegarder avec le nouvel utilisateur
        AchatEntity achat = achatService.modifierAchat(id, achatModifie);
        return ResponseEntity.ok(AchatDto.fromEntity(achat));
    }

    /**
     * GET /api/achats/statistiques?debut=2025-01-01&fin=2025-12-31
     */
    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> obtenirStatistiques(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        List<AchatEntity> achats = achatService.obtenirAchatsParPeriode(debut, fin);
        BigDecimal total = achatService.calculerTotalAchats(debut, fin);

        // Convertir les achats en DTOs
        List<AchatDto> achatsDto = achats.stream()
                .map(AchatDto::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("dateDebut", debut);
        stats.put("dateFin", fin);
        stats.put("nombreAchats", achatsDto.size());
        stats.put("montantTotal", total);
        stats.put("achats", achatsDto);

        return ResponseEntity.ok(stats);
    }

    /**
     * DELETE /api/achats/{id}
     * Supprimer un achat
     *
     * Exemple : DELETE /api/achats/5
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        achatService.supprimerAchat(id);

        // Retourner 204 No Content (succès sans contenu)
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/achats/produits-pour-vente
     * Récupérer la liste des produits avec leurs prix de vente suggérés
     *
     * Cette route est accessible à TOUS les utilisateurs (y compris USER)
     * Elle retourne uniquement les informations nécessaires pour créer une vente :
     * - Nom du produit
     * - Prix de vente suggéré
     *
     * Les informations sensibles (prix d'achat, fournisseur, etc.) ne sont PAS exposées
     *
     * Exemple : GET /api/achats/produits-pour-vente
     * Retourne : [
     *   { "nomProduit": "Bracelet", "prixVenteSuggere": 25.00 },
     *   { "nomProduit": "Collier", "prixVenteSuggere": 50.00 }
     * ]
     */
    @GetMapping("/produits-pour-vente")
    public ResponseEntity<List<ProduitPourVenteDto>> obtenirProduitsAvecPrixVente() {
        List<AchatEntity> achats = achatService.obtenirProduitsAvecPrixVente();

        // Convertir en DTO simplifié (uniquement nom produit + prix vente suggéré)
        List<ProduitPourVenteDto> produits = achats.stream()
                .map(ProduitPourVenteDto::fromAchat)
                .toList();

        return ResponseEntity.ok(produits);
    }
}
