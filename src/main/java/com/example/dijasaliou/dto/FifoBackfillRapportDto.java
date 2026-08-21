package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Rapport d'exécution du backfill FIFO (rétroactif).
 *
 * Indique combien de ventes/achats ont été retraités, le bénéfice total
 * reconstruit, et la liste des anomalies (ventes sans lot, stock négatif, etc.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FifoBackfillRapportDto {

    /** true = simulation sans écriture en base. */
    private boolean      dryRun;
    private String       tenantUuid;
    private String       tenantNom;

    private long         nbAchatsTraites;
    private long         nbVentesTraitees;
    private long         nbConsommationsCreees;
    private long         nbVentesEntierementTracees;
    private long         nbVentesPartiellementTracees;
    private long         nbVentesNonTracees;

    private BigDecimal   beneficeTotalReconstitue;

    @Builder.Default
    private List<String> anomalies = new ArrayList<>();

    private long         dureeMs;
}
