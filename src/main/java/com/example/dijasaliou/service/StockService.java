package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.repository.VenteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service pour gérer le stock des produits
 *
 * Calcule le stock disponible pour chaque produit en comparant les achats et les ventes
 */
@Service
public class StockService {

    private final AchatRepository achatRepository;
    private final VenteRepository venteRepository;
    private final TenantService tenantService;

    public StockService(AchatRepository achatRepository, VenteRepository venteRepository, TenantService tenantService) {
        this.achatRepository = achatRepository;
        this.venteRepository = venteRepository;
        this.tenantService = tenantService;
    }

    /**
     * Obtenir le stock de tous les produits — mis en cache 2 min par tenant.
     * Le cache est invalidé après chaque achat ou vente (voir invalidateStockCache).
     *
     * @return Liste des stocks par produit
     */
    @Cacheable(value = "stocks", key = "#root.target.getCurrentTenantKey()")
    @Transactional(readOnly = true)
    public List<StockDto> obtenirTousLesStocks() {
        // 1. Récupérer les achats et ventes du tenant courant uniquement
        TenantEntity tenant = tenantService.getCurrentTenant();
        List<AchatEntity> achats = achatRepository.findAllByTenant(tenant);
        List<VenteEntity> ventes = venteRepository.findAllByTenant(tenant);

        // 2. Grouper les achats par nom de produit et calculer les totaux
        Map<String, List<AchatEntity>> achatsParProduit = achats.stream()
                .collect(Collectors.groupingBy(
                        achat -> achat.getNomProduit().toLowerCase().trim()
                ));

        // 3. Grouper les ventes par nom de produit et calculer les totaux
        Map<String, List<VenteEntity>> ventesParProduit = ventes.stream()
                .collect(Collectors.groupingBy(
                        vente -> vente.getNomProduit().toLowerCase().trim()
                ));

        // 4. Créer la liste des stocks
        List<StockDto> stocks = new ArrayList<>();

        // 5. Pour chaque produit acheté, calculer son stock
        for (Map.Entry<String, List<AchatEntity>> entry : achatsParProduit.entrySet()) {
            String nomProduit = entry.getKey();
            List<AchatEntity> achatsProduitsListe = entry.getValue();
            List<VenteEntity> ventesProduitsListe = ventesParProduit.getOrDefault(nomProduit, new ArrayList<>());

            stocks.add(calculerStock(nomProduit, achatsProduitsListe, ventesProduitsListe));
        }

        // 6. Trier par stock disponible (du plus faible au plus élevé pour voir les alertes)
        stocks.sort((s1, s2) -> Double.compare(s1.getStockDisponible(), s2.getStockDisponible()));

        return stocks;
    }

    /**
     * Obtenir le stock d'un produit spécifique par son nom
     *
     * @param nomProduit Nom du produit (insensible à la casse)
     * @return Stock du produit
     */
    @Transactional(readOnly = true)
    public StockDto obtenirStockParNomProduit(String nomProduit) {
        TenantEntity tenant = tenantService.getCurrentTenant();

        // Requêtes ciblées avec filtre tenant EXPLICITE (pas de dépendance au filtre Hibernate)
        List<AchatEntity> achats = achatRepository.findByNomProduitAndTenant(nomProduit, tenant);
        List<VenteEntity> ventes = venteRepository.findByNomProduitAndTenant(nomProduit, tenant);

        // Fallback insensible à la casse si rien trouvé avec le nom exact
        if (achats.isEmpty() && ventes.isEmpty()) {
            String nomNormalise = nomProduit.toLowerCase().trim();
            achats = achatRepository.findByNomProduitContainingAndTenant(nomProduit, tenant).stream()
                    .filter(a -> a.getNomProduit().toLowerCase().trim().equals(nomNormalise))
                    .collect(Collectors.toList());
            ventes = venteRepository.findByNomProduitContainingAndTenant(nomProduit, tenant).stream()
                    .filter(v -> v.getNomProduit().toLowerCase().trim().equals(nomNormalise))
                    .collect(Collectors.toList());
        }

        if (achats.isEmpty() && ventes.isEmpty()) {
            throw new RuntimeException("Produit non trouvé : " + nomProduit);
        }

        return calculerStock(nomProduit, achats, ventes);
    }

    /**
     * Obtenir les produits en rupture de stock
     *
     * @return Liste des produits avec stock = 0
     */
    @Transactional(readOnly = true)
    public List<StockDto> obtenirProduitsEnRupture() {
        return obtenirTousLesStocks().stream()
                .filter(stock -> stock.getStockDisponible() <= 0)
                .collect(Collectors.toList());
    }

    /**
     * Obtenir les produits avec stock bas (< 10 unités)
     *
     * @return Liste des produits avec stock faible
     */
    @Transactional(readOnly = true)
    public List<StockDto> obtenirProduitsStockBas() {
        return obtenirTousLesStocks().stream()
                .filter(stock -> stock.getStockDisponible() > 0 && stock.getStockDisponible() < 10)
                .collect(Collectors.toList());
    }

    /**
     * Vérifier si un produit a un stock suffisant pour une vente
     *
     * @param nomProduit Nom du produit
     * @param quantite Quantité demandée
     * @return true si le stock est suffisant
     */
    @Transactional(readOnly = true)
    public boolean verifierStockDisponible(String nomProduit, Double quantite) {
        try {
            StockDto stock = obtenirStockParNomProduit(nomProduit);
            return stock.estSuffisant(quantite);
        } catch (RuntimeException e) {
            // Produit non trouvé = pas de stock
            return false;
        }
    }

    /**
     * Obtenir la valeur totale du stock
     *
     * @return Somme des valeurs de tous les stocks
     */
    @Transactional(readOnly = true)
    public BigDecimal obtenirValeurTotaleStock() {
        return obtenirTousLesStocks().stream()
                .map(StockDto::getValeurStock)
                .filter(valeur -> valeur != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Clé de cache = UUID du tenant courant (isolé par entreprise).
     * Appelée par @Cacheable(key = "#root.target.getCurrentTenantKey()").
     */
    public String getCurrentTenantKey() {
        return tenantService.isTenantDefined()
                ? tenantService.getCurrentTenant().getTenantUuid()
                : "no-tenant";
    }

    /**
     * Invalide le cache des stocks pour le tenant courant.
     * À appeler après chaque achat ou vente.
     */
    @CacheEvict(value = "stocks", key = "#tenantUuid")
    public void invalidateStockCache(String tenantUuid) {
        // méthode vide — l'annotation fait le travail
    }

    /**
     * Méthode privée pour calculer le stock d'un produit
     *
     * @param nomProduit Nom du produit
     * @param achats Liste des achats
     * @param ventes Liste des ventes
     * @return StockDto calculé
     */
    private StockDto calculerStock(String nomProduit, List<AchatEntity> achats, List<VenteEntity> ventes) {
        // Calculer la quantité totale achetée
        Double quantiteAchetee = achats.stream()
                .mapToDouble(AchatEntity::getQuantite)
                .sum();

        // Calculer la quantité totale vendue
        Double quantiteVendue = ventes.stream()
                .mapToDouble(VenteEntity::getQuantite)
                .sum();

        // Calculer le stock disponible
        Double stockDisponible = quantiteAchetee - quantiteVendue;

        // Calculer le prix moyen pondéré d'achat (somme(prix × qté) / totalQté)
        BigDecimal prixMoyenAchat = BigDecimal.ZERO;
        if (quantiteAchetee > 0) {
            BigDecimal totalValeurAchats = achats.stream()
                    .map(a -> a.getPrixUnitaire().multiply(BigDecimal.valueOf(a.getQuantite())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            prixMoyenAchat = totalValeurAchats.divide(
                    BigDecimal.valueOf(quantiteAchetee),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        // Calculer le prix moyen pondéré de vente (somme(prix × qté) / totalQté)
        BigDecimal prixMoyenVente = BigDecimal.ZERO;
        if (quantiteVendue > 0) {
            BigDecimal totalValeurVentes = ventes.stream()
                    .map(v -> v.getPrixUnitaire().multiply(BigDecimal.valueOf(v.getQuantite())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            prixMoyenVente = totalValeurVentes.divide(
                    BigDecimal.valueOf(quantiteVendue),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        // Calculer la valeur du stock
        BigDecimal valeurStock = prixMoyenAchat.multiply(BigDecimal.valueOf(stockDisponible));

        // Calculer la marge unitaire
        BigDecimal margeUnitaire = prixMoyenVente.subtract(prixMoyenAchat);

        // Déterminer le statut
        StockDto.StatutStock statut = StockDto.determinerStatut(stockDisponible);

        // Vérifier si le plan ENTERPRISE est actif pour afficher les photos
        TenantEntity currentTenant = tenantService.isTenantDefined() ? tenantService.getCurrentTenant() : null;
        boolean canViewPhotos = currentTenant != null &&
                                currentTenant.getPlan() == TenantEntity.Plan.BUSINESS;

        // Récupérer le dernier achat (le plus récent) pour la photo et l'unité
        AchatEntity dernierAchat = achats.stream()
                .sorted(Comparator.comparing(AchatEntity::getDateAchat).reversed())
                .findFirst()
                .orElse(null);

        // Récupérer la photo du dernier achat (le plus récent avec une photo)
        // RESTRICTION : null si le plan n'est pas ENTERPRISE
        String photoUrl = null;
        if (canViewPhotos) {
            photoUrl = achats.stream()
                    .filter(achat -> achat.getPhotoUrl() != null && !achat.getPhotoUrl().isEmpty())
                    .sorted(Comparator.comparing(AchatEntity::getDateAchat).reversed())
                    .findFirst()
                    .map(AchatEntity::getPhotoUrl)
                    .orElse(null);
        }

        // Récupérer l'unité depuis le dernier achat
        String unite = dernierAchat != null ? dernierAchat.getUnite() : "pièce";

        return StockDto.builder()
                .nomProduit(nomProduit)
                .photoUrl(photoUrl)
                .unite(unite)
                .quantiteAchetee(quantiteAchetee)
                .quantiteVendue(quantiteVendue)
                .stockDisponible(stockDisponible)
                .prixMoyenAchat(prixMoyenAchat)
                .prixMoyenVente(prixMoyenVente)
                .valeurStock(valeurStock)
                .margeUnitaire(margeUnitaire)
                .statut(statut)
                .build();
    }
}
