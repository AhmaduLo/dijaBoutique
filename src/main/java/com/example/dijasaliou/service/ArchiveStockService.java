package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.ProduitArchiveEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.ProduitArchiveRepository;
import com.example.dijasaliou.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArchiveStockService {

    private final ProduitArchiveRepository produitArchiveRepository;
    private final TenantRepository tenantRepository;
    private final StockService stockService;

    /**
     * Job planifié : vérifie les produits en rupture et les archive après 30 jours.
     * Exécuté tous les jours à 3h du matin.
     *
     * Logique en 2 étapes :
     * 1. Produit en rupture → enregistrer dateRupture (pas encore archivé, dateArchivage = null)
     * 2. dateRupture > 30 jours → archiver (dateArchivage = now)
     *
     * Si un produit revient en stock → supprimé de la table (via desarchiverSiNecessaire)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void archiverProduitsEnRupture() {
        log.info("[ARCHIVE] Début du job d'archivage automatique");
        int totalArchives = 0;

        List<TenantEntity> tenants = tenantRepository.findByDeletedFalse();

        for (TenantEntity tenant : tenants) {
            try {
                totalArchives += traiterPourTenant(tenant);
            } catch (Exception e) {
                log.warn("[ARCHIVE] Erreur pour le tenant {} : {}", tenant.getNomEntreprise(), e.getMessage());
            }
        }

        log.info("[ARCHIVE] Job terminé : {} produit(s) archivé(s)", totalArchives);
    }

    private int traiterPourTenant(TenantEntity tenant) {
        List<StockDto> stocks = stockService.obtenirTousLesStocksParTenant(tenant);
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite = maintenant.minusDays(30);
        int count = 0;

        for (StockDto stock : stocks) {
            String nomNormalise = stock.getNomProduit().toLowerCase().trim();
            boolean enRupture = stock.getStockDisponible() != null && stock.getStockDisponible() <= 0;

            Optional<ProduitArchiveEntity> existant = produitArchiveRepository.findByTenantAndNomProduit(tenant, nomNormalise);

            if (enRupture) {
                if (existant.isEmpty()) {
                    // Étape 1 : première détection de rupture → enregistrer dateRupture, pas encore archivé
                    ProduitArchiveEntity archive = ProduitArchiveEntity.builder()
                            .tenant(tenant)
                            .nomProduit(nomNormalise)
                            .dateRupture(maintenant)
                            .dateArchivage(null)
                            .build();
                    produitArchiveRepository.save(archive);
                    log.debug("[ARCHIVE] Rupture détectée : {} ({})", nomNormalise, tenant.getNomEntreprise());
                } else if (existant.get().getDateArchivage() == null
                        && existant.get().getDateRupture().isBefore(limite)) {
                    // Étape 2 : en rupture depuis 30+ jours → archiver
                    existant.get().setDateArchivage(maintenant);
                    produitArchiveRepository.save(existant.get());
                    count++;
                    log.info("[ARCHIVE] Archivé : {} ({}) — en rupture depuis {}",
                            nomNormalise, tenant.getNomEntreprise(), existant.get().getDateRupture());
                }
            } else if (existant.isPresent()) {
                // Produit de retour en stock → supprimer l'entrée
                produitArchiveRepository.delete(existant.get());
                log.info("[ARCHIVE] Retour en stock : {} ({})", nomNormalise, tenant.getNomEntreprise());
            }
        }

        return count;
    }

    /**
     * Désarchiver un produit (appelé quand un achat est créé pour un produit archivé).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void desarchiverSiNecessaire(TenantEntity tenant, String nomProduit) {
        try {
            String nomNormalise = nomProduit.toLowerCase().trim();
            if (produitArchiveRepository.existsByTenantAndNomProduit(tenant, nomNormalise)) {
                produitArchiveRepository.deleteByTenantAndNomProduit(tenant, nomNormalise);
                log.info("[ARCHIVE] Produit désarchivé : {} ({})", nomProduit, tenant.getNomEntreprise());
            }
        } catch (Exception e) {
            log.warn("[ARCHIVE] Erreur désarchivage {} : {}", nomProduit, e.getMessage());
        }
    }

    /**
     * Vérifier si un produit est archivé (dateArchivage non null).
     */
    @Transactional(readOnly = true)
    public boolean estArchive(TenantEntity tenant, String nomProduit) {
        return produitArchiveRepository.findByTenantAndNomProduit(tenant, nomProduit.toLowerCase().trim())
                .map(a -> a.getDateArchivage() != null)
                .orElse(false);
    }

    /**
     * Récupérer tous les produits archivés d'un tenant.
     */
    @Transactional(readOnly = true)
    public List<ProduitArchiveEntity> obtenirArchives(TenantEntity tenant) {
        return produitArchiveRepository.findByTenant(tenant);
    }
}
