package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.FifoBackfillRapportDto;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.service.FifoBackfillService;
import com.example.dijasaliou.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints admin pour la migration FIFO (rétroactive).
 *
 * Sécurité : réservé aux ADMIN du tenant courant.
 *
 *   GET  /api/admin/fifo/backfill/dry-run  → simulation, aucune écriture en base
 *   POST /api/admin/fifo/backfill          → exécution réelle, écrit les lignes
 */
@RestController
@RequestMapping("/admin/fifo")
@RequiredArgsConstructor
public class FifoMigrationController {

    private final FifoBackfillService fifoBackfillService;
    private final TenantService       tenantService;

    /**
     * GET /api/admin/fifo/backfill/dry-run
     *
     * Simule le backfill pour le tenant courant. Aucune écriture en base.
     * Retourne le rapport détaillé : nb ventes traitées, anomalies, bénéfice reconstitué.
     */
    @GetMapping("/backfill/dry-run")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<FifoBackfillRapportDto> dryRun() {
        TenantEntity tenant = tenantService.getCurrentTenant();
        return ResponseEntity.ok(fifoBackfillService.executerBackfill(tenant, true));
    }

    /**
     * POST /api/admin/fifo/backfill
     *
     * Exécute le backfill réel pour le tenant courant :
     *   - Reset des quantite_restante des achats.
     *   - Suppression des anciennes lignes de consommation.
     *   - Rejeu FIFO chronologique → création des nouvelles lignes.
     *
     * À utiliser une seule fois en production après déploiement.
     */
    @PostMapping("/backfill")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<FifoBackfillRapportDto> executerBackfill() {
        TenantEntity tenant = tenantService.getCurrentTenant();
        return ResponseEntity.ok(fifoBackfillService.executerBackfill(tenant, false));
    }
}
