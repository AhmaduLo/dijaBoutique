package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.AchatDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.AchatService;
import com.example.dijasaliou.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@CrossOrigin(origins = "http://localhost:4200")
public class AchatController {

    private final AchatService achatService;
    private final UserService userService;

    public AchatController(AchatService achatService, UserService userService) {
        this.achatService = achatService;
        this.userService = userService;
    }

    /**
     * ROUTE 1 : Récupérer tous les achats
     *
     * URL : GET http://localhost:8080/api/achats
     *
     * @GetMapping : Dit que c'est une route GET (lecture)
     * @return Liste de tous les achats en JSON avec informations utilisateur
     */
    @GetMapping
    public ResponseEntity<List<AchatDto>> obtenirTous() {
        List<AchatEntity> achats = achatService.obtenirTousLesAchats();
        List<AchatDto> achatsDto = achats.stream()
                .map(AchatDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(achatsDto);
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
    public ResponseEntity<AchatDto> obtenirParId(@PathVariable Long id) {
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
     * Créer un nouvel achat
     *
     * @RequestBody : Les données viennent du corps de la requête (JSON)
     * @RequestParam : L'ID utilisateur vient de l'URL (?utilisateurId=1)
     *
     * Exemple : POST /api/achats?utilisateurId=1
     *           Body : { "quantite": 10, "nomProduit": "Collier", ... }
     */
    @PostMapping
    public ResponseEntity<AchatDto> creer(
            @RequestBody AchatEntity achat,
            @RequestParam Long utilisateurId) {

        // Récupérer l'utilisateur
        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);

        // Créer l'achat
        AchatEntity achatCree = achatService.creerAchat(achat, utilisateur);

        // Retourner avec code 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).body(AchatDto.fromEntity(achatCree));
    }

    /**
     * PUT /api/achats/{id}
     * Modifier un achat existant
     *
     * Exemple : PUT /api/achats/5
     *           Body : { "quantite": 20, "nomProduit": "Collier doré", ... }
     */
    @PutMapping("/{id}")
    public ResponseEntity<AchatDto> modifier(
            @PathVariable Long id,
            @RequestBody AchatEntity achatModifie) {

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
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        achatService.supprimerAchat(id);

        // Retourner 204 No Content (succès sans contenu)
        return ResponseEntity.noContent().build();
    }
}
