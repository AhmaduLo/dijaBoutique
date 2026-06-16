package com.example.dijasaliou.controller;

import com.example.dijasaliou.annotation.RequiresPlan;
import com.example.dijasaliou.dto.CreditClientDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.PaiementCreditDto;
import com.example.dijasaliou.dto.PaiementCreditRequest;
import com.example.dijasaliou.entity.PaiementCreditEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.CreditClientService;
import com.example.dijasaliou.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/credits")

@Slf4j
@RequiredArgsConstructor
public class CreditController {

    private final CreditClientService creditClientService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'GERANT', 'ADMIN')")
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS})
    public ResponseEntity<CreditClientDto> creerCredit(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        if (body.get("venteId") == null || body.get("clientId") == null) {
            throw new IllegalArgumentException("venteId et clientId sont obligatoires");
        }
        String venteId = body.get("venteId").toString();
        String clientId = body.get("clientId").toString();
        java.time.LocalDate dateEcheance = body.get("dateEcheance") != null
                ? java.time.LocalDate.parse(body.get("dateEcheance").toString())
                : null;
        com.example.dijasaliou.entity.UserEntity employe =
                userService.obtenirUtilisateurParEmail(auth.getName());
        com.example.dijasaliou.entity.CreditClientEntity credit =
                creditClientService.creerCredit(venteId, clientId, dateEcheance, employe);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(CreditClientDto.fromEntity(credit));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERANT')")
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS})
    public ResponseEntity<PagedResponse<CreditClientDto>> obtenirCredits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        if (size > 100) size = 100;
        return ResponseEntity.ok(creditClientService.obtenirCredits(page, size, search, statut, dateDebut, dateFin));
    }

    @GetMapping("/client/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERANT', 'USER')")
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS})
    public ResponseEntity<List<CreditClientDto>> obtenirHistoriqueClient(
            @PathVariable String id, Authentication auth) {
        log.info("Historique crédits client #{} demandé par {}", id, auth.getName());
        return ResponseEntity.ok(creditClientService.obtenirHistoriqueClient(id));
    }

    @PostMapping("/{id}/payer")
    @PreAuthorize("isAuthenticated()")
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS})
    public ResponseEntity<CreditClientDto> enregistrerPaiement(
            @PathVariable String id,
            @Valid @RequestBody PaiementCreditRequest request,
            Authentication auth) {
        UserEntity employe = userService.obtenirUtilisateurParEmail(auth.getName());
        log.info("Paiement de {} {} sur crédit #{} par {}",
                request.getMontant(), request.getModePaiement(), id, auth.getName());
        CreditClientDto result = creditClientService.enregistrerPaiement(
                id,
                request.getMontant(),
                request.getModePaiement(),
                request.getNote(),
                request.getDatePaiement(),
                employe);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/paiements")
    @PreAuthorize("isAuthenticated()")
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS})
    public ResponseEntity<List<PaiementCreditDto>> obtenirPaiements(@PathVariable String id) {
        return ResponseEntity.ok(creditClientService.obtenirPaiements(id));
    }

    /**
     * POST /api/credits/{id}/passer-en-perte
     *
     * Le commerçant indique qu'il abandonne ce crédit (le client ne paiera jamais).
     * Le reste dû devient une perte FIFO de catégorie CREDIT_IMPAYE dans les stats,
     * mais les paiements déjà reçus restent dans le CA / la caisse.
     */
    @PostMapping("/{id}/passer-en-perte")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERANT')")
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS})
    public ResponseEntity<CreditClientDto> passerEnPerte(@PathVariable String id, Authentication auth) {
        log.info("Crédit #{} passé en perte par {}", id, auth.getName());
        return ResponseEntity.ok(creditClientService.passerEnPerte(id));
    }

    /** GET /api/credits/{id} — détail d'un crédit (utilisé par certains refresh frontend). */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS})
    public ResponseEntity<CreditClientDto> obtenirCredit(@PathVariable String id) {
        return ResponseEntity.ok(creditClientService.obtenirCredit(id));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERANT')")
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS})
    public ResponseEntity<Map<String, Object>> obtenirStats() {
        return ResponseEntity.ok(creditClientService.obtenirStats());
    }
}
