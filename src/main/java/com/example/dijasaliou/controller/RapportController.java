package com.example.dijasaliou.controller;

import com.example.dijasaliou.annotation.RequiresPlan;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.service.RapportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/rapports")
@RequiredArgsConstructor
public class RapportController {

    private final RapportService rapportService;

    /**
     * GET /api/rapports/pdf?debut=2026-01-01&fin=2026-01-31
     *
     * Génère un rapport PDF complet pour la période donnée :
     * résumé exécutif, top produits, modes paiement, stock, crédits,
     * dépenses, bons de livraison, relevé chronologique, certification.
     *
     * Accès : ADMIN ou GERANT uniquement.
     * Plan requis : BUSINESS.
     */
    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERANT')")
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS})
    public ResponseEntity<byte[]> genererRapportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        if (fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début");
        }
        if (ChronoUnit.DAYS.between(debut, fin) > 366) {
            throw new IllegalArgumentException("La période ne peut pas dépasser 366 jours");
        }

        byte[] pdfBytes = rapportService.genererRapportPdf(debut, fin);

        String filename = String.format("rapport-%s-%s.pdf", debut, fin);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
