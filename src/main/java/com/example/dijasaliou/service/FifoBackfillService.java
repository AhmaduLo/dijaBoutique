package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.FifoBackfillRapportDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.entity.VenteLotConsommationEntity;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.repository.VenteLotConsommationRepository;
import com.example.dijasaliou.repository.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backfill rétroactif des données FIFO pour un tenant.
 *
 * Logique :
 *   1. Reset des quantite_restante (= quantite) de tous les achats du tenant.
 *   2. Suppression des anciennes lignes de consommation.
 *   3. Rejeu chronologique :
 *      - Pour chaque vente triée par date_vente ASC,
 *      - Application de la logique FIFO (lots triés par date_achat ASC),
 *      - Génération des lignes vente_lot_consommation et décrément des lots.
 *   4. Production d'un rapport détaillé.
 *
 * Mode dry-run : tout est calculé en mémoire, RIEN n'est écrit en base.
 * Mode réel    : les écritures sont effectuées dans la transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FifoBackfillService {

    private final AchatRepository                  achatRepository;
    private final VenteRepository                  venteRepository;
    private final VenteLotConsommationRepository   consommationRepository;

    /**
     * Lance le backfill pour un tenant donné.
     *
     * @param tenant Le tenant à traiter.
     * @param dryRun true = simulation, false = écriture réelle.
     * @return Rapport d'exécution.
     */
    @Transactional
    public FifoBackfillRapportDto executerBackfill(TenantEntity tenant, boolean dryRun) {
        long t0 = System.currentTimeMillis();
        List<String> anomalies = new ArrayList<>();

        log.info("FIFO Backfill : démarrage pour tenant={} ({}), dryRun={}",
                tenant.getTenantUuid(), tenant.getNomEntreprise(), dryRun);

        // 1. Récupérer tous les achats triés par date ASC
        List<AchatEntity> achats = achatRepository.findAllByTenantOrderByDateAsc(tenant);

        // Reset des quantites restantes (en mémoire pour dryRun, sinon sauvegardé en fin)
        for (AchatEntity a : achats) {
            a.setQuantiteRestante(a.getQuantite());
        }

        // 2. Supprimer les anciennes consommations (seulement en mode réel)
        if (!dryRun) {
            // findByVenteId par vente serait trop coûteux ; on supprime via le tenant
            List<VenteLotConsommationEntity> ancienne = consommationRepository.findAll()
                    .stream()
                    .filter(c -> c.getTenant() != null
                            && c.getTenant().getId().equals(tenant.getId()))
                    .toList();
            if (!ancienne.isEmpty()) {
                consommationRepository.deleteAll(ancienne);
                log.info("FIFO Backfill : suppression de {} ancienne(s) ligne(s) de consommation",
                        ancienne.size());
            }
        }

        // 3. Indexer les lots par nom de produit pour accès rapide
        Map<String, List<AchatEntity>> lotsParProduit = new HashMap<>();
        for (AchatEntity a : achats) {
            lotsParProduit
                    .computeIfAbsent(a.getNomProduit(), k -> new ArrayList<>())
                    .add(a);
        }

        // 4. Rejouer toutes les ventes dans l'ordre chronologique
        List<VenteEntity> ventes = venteRepository.findAllByTenantOrderByDateAsc(tenant);

        long nbConsommationsCreees       = 0;
        long nbVentesEntierementTracees  = 0;
        long nbVentesPartiellementTracees = 0;
        long nbVentesNonTracees          = 0;
        BigDecimal beneficeTotal         = BigDecimal.ZERO;
        List<VenteLotConsommationEntity> nouvellesConsommations = new ArrayList<>();

        for (VenteEntity vente : ventes) {
            String nomProduit = vente.getNomProduit();
            double quantiteAVendre = vente.getQuantite() != null ? vente.getQuantite() : 0.0;
            if (quantiteAVendre <= 0) {
                continue;
            }

            List<AchatEntity> lots = lotsParProduit.getOrDefault(nomProduit, List.of());
            if (lots.isEmpty()) {
                nbVentesNonTracees++;
                anomalies.add(String.format("Vente %s (%s, %s) : aucun lot d'achat pour le produit '%s'",
                        vente.getId(),
                        vente.getDateVente() != null ? vente.getDateVente().toLocalDate() : "?",
                        formatQuantite(quantiteAVendre),
                        nomProduit));
                continue;
            }

            double quantiteRestanteAVendre = quantiteAVendre;
            List<VenteLotConsommationEntity> consoVente = new ArrayList<>();

            for (AchatEntity lot : lots) {
                if (quantiteRestanteAVendre <= 0) break;
                if (lot.getQuantiteRestante() == null || lot.getQuantiteRestante() <= 0) continue;

                double aPuiser = Math.min(lot.getQuantiteRestante(), quantiteRestanteAVendre);

                VenteLotConsommationEntity c = VenteLotConsommationEntity.builder()
                        .vente(vente)
                        .achat(lot)
                        .tenant(tenant)
                        .quantiteConsommee(aPuiser)
                        .prixAchatUnitaireSnapshot(lot.getPrixUnitaire())
                        .prixVenteUnitaireSnapshot(vente.getPrixUnitaire())
                        .dateVenteSnapshot(vente.getDateVente())
                        .build();
                c.recalculerBenefice();
                consoVente.add(c);

                lot.setQuantiteRestante(lot.getQuantiteRestante() - aPuiser);
                quantiteRestanteAVendre -= aPuiser;
            }

            nouvellesConsommations.addAll(consoVente);
            nbConsommationsCreees += consoVente.size();
            beneficeTotal = beneficeTotal.add(
                    consoVente.stream()
                            .map(VenteLotConsommationEntity::getBeneficeTotalLigne)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));

            if (quantiteRestanteAVendre <= 0) {
                nbVentesEntierementTracees++;
            } else if (consoVente.isEmpty()) {
                nbVentesNonTracees++;
                anomalies.add(String.format("Vente %s (%s) : stock épuisé pour '%s' (manque %s)",
                        vente.getId(),
                        vente.getDateVente() != null ? vente.getDateVente().toLocalDate() : "?",
                        nomProduit,
                        formatQuantite(quantiteRestanteAVendre)));
            } else {
                nbVentesPartiellementTracees++;
                anomalies.add(String.format("Vente %s (%s) : %s sur %s tracées pour '%s' (manque %s)",
                        vente.getId(),
                        vente.getDateVente() != null ? vente.getDateVente().toLocalDate() : "?",
                        formatQuantite(quantiteAVendre - quantiteRestanteAVendre),
                        formatQuantite(quantiteAVendre),
                        nomProduit,
                        formatQuantite(quantiteRestanteAVendre)));
            }
        }

        // 5. Sauvegarde en mode réel
        if (!dryRun) {
            achatRepository.saveAll(achats);
            consommationRepository.saveAll(nouvellesConsommations);
            log.info("FIFO Backfill : {} ligne(s) de consommation créée(s), {} achat(s) mis à jour",
                    nouvellesConsommations.size(), achats.size());
        } else {
            log.info("FIFO Backfill (DRY-RUN) : {} ligne(s) qui auraient été créées, {} achat(s) qui auraient été mis à jour",
                    nouvellesConsommations.size(), achats.size());
        }

        long dureeMs = System.currentTimeMillis() - t0;
        log.info("FIFO Backfill : terminé en {} ms — {} ventes, {} entièrement / {} partiellement / {} non tracées, bénéfice total = {}",
                dureeMs, ventes.size(),
                nbVentesEntierementTracees, nbVentesPartiellementTracees, nbVentesNonTracees,
                beneficeTotal);

        return FifoBackfillRapportDto.builder()
                .dryRun(dryRun)
                .tenantUuid(tenant.getTenantUuid())
                .tenantNom(tenant.getNomEntreprise())
                .nbAchatsTraites(achats.size())
                .nbVentesTraitees(ventes.size())
                .nbConsommationsCreees(nbConsommationsCreees)
                .nbVentesEntierementTracees(nbVentesEntierementTracees)
                .nbVentesPartiellementTracees(nbVentesPartiellementTracees)
                .nbVentesNonTracees(nbVentesNonTracees)
                .beneficeTotalReconstitue(beneficeTotal)
                .anomalies(anomalies)
                .dureeMs(dureeMs)
                .build();
    }

    private static String formatQuantite(double q) {
        if (q == Math.floor(q)) {
            return String.valueOf((long) q);
        }
        return String.format("%.2f", q);
    }
}
