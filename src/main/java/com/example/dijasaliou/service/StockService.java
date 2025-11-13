package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.repository.VenteRepository;
import org.springframework.stereotype.Service;

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

    public StockService(AchatRepository achatRepository, VenteRepository venteRepository) {
        this.achatRepository = achatRepository;
        this.venteRepository = venteRepository;
    }

    /**
     * Obtenir le stock de tous les produits
     *
     * @return Liste des stocks par produit
     */
    public List<StockDto> obtenirTousLesStocks() {
        // 1. Récupérer tous les achats et ventes
        List<AchatEntity> achats = achatRepository.findAll();
        List<VenteEntity> ventes = venteRepository.findAll();

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
        stocks.sort((s1, s2) -> Integer.compare(s1.getStockDisponible(), s2.getStockDisponible()));

        return stocks;
    }

    /**
     * Obtenir le stock d'un produit spécifique par son nom
     *
     * @param nomProduit Nom du produit (insensible à la casse)
     * @return Stock du produit
     */
    public StockDto obtenirStockParNomProduit(String nomProduit) {
        String nomProduitNormalise = nomProduit.toLowerCase().trim();

        // Récupérer les achats et ventes pour ce produit
        List<AchatEntity> achats = achatRepository.findAll().stream()
                .filter(a -> a.getNomProduit().toLowerCase().trim().equals(nomProduitNormalise))
                .collect(Collectors.toList());

        List<VenteEntity> ventes = venteRepository.findAll().stream()
                .filter(v -> v.getNomProduit().toLowerCase().trim().equals(nomProduitNormalise))
                .collect(Collectors.toList());

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
    public boolean verifierStockDisponible(String nomProduit, Integer quantite) {
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
    public BigDecimal obtenirValeurTotaleStock() {
        return obtenirTousLesStocks().stream()
                .map(StockDto::getValeurStock)
                .filter(valeur -> valeur != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
        Integer quantiteAchetee = achats.stream()
                .mapToInt(AchatEntity::getQuantite)
                .sum();

        // Calculer la quantité totale vendue
        Integer quantiteVendue = ventes.stream()
                .mapToInt(VenteEntity::getQuantite)
                .sum();

        // Calculer le stock disponible
        Integer stockDisponible = quantiteAchetee - quantiteVendue;

        // Calculer le prix moyen d'achat
        BigDecimal prixMoyenAchat = BigDecimal.ZERO;
        if (!achats.isEmpty()) {
            BigDecimal totalAchats = achats.stream()
                    .map(AchatEntity::getPrixUnitaire)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            prixMoyenAchat = totalAchats.divide(
                    new BigDecimal(achats.size()),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        // Calculer le prix moyen de vente
        BigDecimal prixMoyenVente = BigDecimal.ZERO;
        if (!ventes.isEmpty()) {
            BigDecimal totalVentes = ventes.stream()
                    .map(VenteEntity::getPrixUnitaire)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            prixMoyenVente = totalVentes.divide(
                    new BigDecimal(ventes.size()),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        // Calculer la valeur du stock
        BigDecimal valeurStock = prixMoyenAchat.multiply(new BigDecimal(stockDisponible));

        // Calculer la marge unitaire
        BigDecimal margeUnitaire = prixMoyenVente.subtract(prixMoyenAchat);

        // Déterminer le statut
        StockDto.StatutStock statut = StockDto.determinerStatut(stockDisponible);

        // Récupérer la photo du dernier achat (le plus récent avec une photo)
        String photoUrl = achats.stream()
                .filter(achat -> achat.getPhotoUrl() != null && !achat.getPhotoUrl().isEmpty())
                .sorted(Comparator.comparing(AchatEntity::getDateAchat).reversed())
                .findFirst()
                .map(AchatEntity::getPhotoUrl)
                .orElse(null);

        return StockDto.builder()
                .nomProduit(nomProduit)
                .photoUrl(photoUrl)
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
