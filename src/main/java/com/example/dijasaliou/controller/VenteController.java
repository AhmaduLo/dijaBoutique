package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.BeneficeStatistiquesDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.VenteDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.service.TenantService;
import com.example.dijasaliou.service.UserService;
import com.example.dijasaliou.service.VenteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller REST pour les ventes
 */
@RestController
@RequestMapping("/ventes")

public class VenteController {

    private final VenteService venteService;
    private final UserService userService;
    private final AchatRepository achatRepository;
    private final TenantService tenantService;

    public VenteController(VenteService venteService, UserService userService,
                           AchatRepository achatRepository, TenantService tenantService) {
        this.venteService = venteService;
        this.userService = userService;
        this.achatRepository = achatRepository;
        this.tenantService = tenantService;
    }

    /**
     * GET /api/ventes?page=0&size=20&search=xxx&dateDebut=2025-01-01&dateFin=2025-12-31
     */
    @GetMapping
    public ResponseEntity<PagedResponse<VenteDto>> obtenirTous(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        if (size > 100) size = 100;
        return ResponseEntity.ok(venteService.obtenirVentesPaginees(page, size, search, dateDebut, dateFin));
    }

    /**
     * GET /api/ventes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<VenteDto> obtenirParId(@PathVariable String id) {
        VenteEntity vente = venteService.obtenirVenteParId(id);
        return ResponseEntity.ok(VenteDto.fromEntity(vente));
    }

    /**
     * POST /api/ventes
     * SÉCURITÉ : L'utilisateur est extrait du token JWT (Authentication),
     * pas d'un paramètre fourni par le client (protection IDOR).
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> creer(
            @Valid @RequestBody VenteEntity vente,
            Authentication authentication) {

        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(authentication.getName());
        VenteEntity venteCree = venteService.creerVente(vente, utilisateur);

        Map<String, Object> response = new HashMap<>();
        response.put("vente", VenteDto.fromEntity(venteCree));

        // Vérifier si la date de vente est avant le premier achat du produit
        if (venteCree.getDateVente() != null) {
            try {
                TenantEntity tenant = tenantService.getCurrentTenant();
                List<AchatEntity> achats = achatRepository.findByNomProduitAndTenant(
                        venteCree.getNomProduit(), tenant);
                if (!achats.isEmpty()) {
                    AchatEntity premierAchat = achats.stream()
                            .min((a, b) -> a.getDateAchat().compareTo(b.getDateAchat()))
                            .orElse(null);
                    if (premierAchat != null && venteCree.getDateVente().isBefore(premierAchat.getDateAchat())) {
                        response.put("warning", "Attention : vous vendez ce produit avant sa date d'achat ("
                                + premierAchat.getDateAchat().toLocalDate() + "). Cela peut affecter vos rapports.");
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/ventes/{id}
     * SÉCURITÉ : L'utilisateur est extrait du token JWT (Authentication),
     * pas d'un paramètre fourni par le client (protection IDOR).
     */
    @PutMapping("/{id}")
    public ResponseEntity<VenteDto> modifier(
            @PathVariable String id,
            @Valid @RequestBody VenteEntity venteModifiee,
            Authentication authentication) {

        // Récupérer l'utilisateur authentifié (depuis le JWT, pas d'un paramètre client)
        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(authentication.getName());

        // Mettre à jour l'utilisateur de la vente
        venteModifiee.setUtilisateur(utilisateur);

        // Sauvegarder avec le nouvel utilisateur
        VenteEntity vente = venteService.modifierVente(id, venteModifiee);
        return ResponseEntity.ok(VenteDto.fromEntity(vente));
    }

    /**
     * GET /api/ventes/prochain-numero-facture
     * Retourne le prochain numéro de ticket/facture unique basé sur le compteur en BDD.
     */
    @GetMapping("/prochain-numero-facture")
    @PreAuthorize("hasAnyAuthority('USER', 'GERANT', 'ADMIN')")
    public ResponseEntity<Map<String, String>> getProchainNumeroFacture() {
        String numero = venteService.getProchainNumeroFacture();
        return ResponseEntity.ok(Map.of("numero", numero));
    }

    /**
     * DELETE /api/ventes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        venteService.supprimerVente(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/ventes/utilisateur/{utilisateurId}?page=0&size=20&search=xxx&dateDebut=...&dateFin=...
     */
    @GetMapping("/utilisateur/{utilisateurId}")
    @PreAuthorize("hasAnyAuthority('USER', 'GERANT', 'ADMIN')")
    public ResponseEntity<PagedResponse<VenteDto>> obtenirVentesParUtilisateur(
            @PathVariable Long utilisateurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        UserEntity utilisateur = userService.obtenirUtilisateurParId(utilisateurId);
        if (size > 100) size = 100;
        return ResponseEntity.ok(venteService.obtenirVentesParUtilisateurPaginees(utilisateur, page, size, search, dateDebut, dateFin));
    }

    /**
     * GET /api/ventes/chiffre-affaires?debut=2025-01-01&fin=2025-12-31
     * Calculer le chiffre d'affaires sur une période
     *
     * Exemple : GET /api/ventes/chiffre-affaires?debut=2025-10-01&fin=2025-10-31
     */
    @GetMapping("/chiffre-affaires")
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    public ResponseEntity<BigDecimal> calculerChiffreAffaires(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        BigDecimal ca = venteService.calculerChiffreAffaires(debut, fin);
        return ResponseEntity.ok(ca);
    }

    /**
     * GET /api/ventes/rapport-modes-paiement?debut=2025-01-01&fin=2025-12-31
     *
     * Retourne la répartition du CA par mode de paiement en tenant compte
     * des remboursements de crédits (table paiements_credit).
     *
     * - ESPECES / WAVE / ORANGE_MONEY = ventes directes + remboursements crédits du même mode
     * - CREDIT = somme des montants restants non soldés (ventes crédit créées dans la période)
     */
    @GetMapping("/rapport-modes-paiement")
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> rapportModePaiement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(venteService.calculerRapportModePaiement(debut, fin));
    }

    /**
     * GET /api/ventes/statistiques?debut=2025-01-01&fin=2025-12-31
     * Obtenir les statistiques de ventes sur une période
     * (nombre de ventes, CA total, liste des ventes)
     *
     * Exemple : GET /api/ventes/statistiques?debut=2025-10-01&fin=2025-10-31
     */
    @GetMapping("/statistiques")
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenirStatistiques(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        List<VenteEntity> ventes = venteService.obtenirVentesParPeriode(debut, fin);
        BigDecimal ca = venteService.calculerChiffreAffaires(debut, fin);

        // Convertir en DTOs
        List<VenteDto> ventesDto = ventes.stream()
                .map(VenteDto::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("dateDebut", debut);
        stats.put("dateFin", fin);
        stats.put("nombreVentes", ventesDto.size());
        stats.put("chiffreAffaires", ca);
        stats.put("ventes", ventesDto);

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/ventes/sorties?debut=2026-06-01&fin=2026-06-30
     *
     * Liste détaillée des sorties hors vente (perte, vol, casse, don, crédit impayé)
     * sur une période. Utilisée par la modale "Pertes" du frontend.
     */
    @GetMapping("/sorties")
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    public ResponseEntity<List<VenteDto>> obtenirSorties(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        List<VenteDto> sorties = venteService.obtenirSortiesPeriode(debut, fin)
                .stream()
                .map(VenteDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sorties);
    }

    /**
     * GET /api/ventes/benefice?debut=2026-06-01&fin=2026-06-07
     *
     * Statistiques de bénéfice net FIFO sur une période :
     *   - chiffreAffaires, totalCoutAchat, beneficeNet, margePourcentage
     *   - nbVentes, nbVentesAvecBenefice, nbVentesSansBenefice
     *
     * Utilisé par la carte "Bénéfice du jour / mois" dans l'UI Ventes.
     */
    @GetMapping("/benefice")
    @PreAuthorize("hasAnyAuthority('GERANT', 'ADMIN')")
    public ResponseEntity<BeneficeStatistiquesDto> obtenirBenefice(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        return ResponseEntity.ok(venteService.calculerStatistiquesBenefice(debut, fin));
    }
}
