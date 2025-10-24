package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST pour la gestion du stock
 *
 * Permet de consulter le stock disponible pour chaque produit
 * basé sur les achats et les ventes
 */
@RestController
@RequestMapping("/stock")
@CrossOrigin(origins = "http://localhost:4200")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * GET /api/stock
     * Obtenir le stock de tous les produits
     *
     * Calcule automatiquement :
     * - Quantité achetée
     * - Quantité vendue
     * - Stock disponible (achetée - vendue)
     * - Prix moyens
     * - Valeur du stock
     * - Marge unitaire
     *
     * Exemple : GET /api/stock
     */
    @GetMapping
    public ResponseEntity<List<StockDto>> obtenirTousLesStocks() {
        List<StockDto> stocks = stockService.obtenirTousLesStocks();
        return ResponseEntity.ok(stocks);
    }

    /**
     * GET /api/stock/produit/{nomProduit}
     * Obtenir le stock d'un produit spécifique
     *
     * @param nomProduit Nom du produit (insensible à la casse)
     *
     * Exemple : GET /api/stock/produit/Collier en or
     */
    @GetMapping("/produit/{nomProduit}")
    public ResponseEntity<StockDto> obtenirStockParProduit(@PathVariable String nomProduit) {
        StockDto stock = stockService.obtenirStockParNomProduit(nomProduit);
        return ResponseEntity.ok(stock);
    }

    /**
     * GET /api/stock/rupture
     * Obtenir les produits en rupture de stock
     *
     * Retourne tous les produits avec stock <= 0
     *
     * Exemple : GET /api/stock/rupture
     */
    @GetMapping("/rupture")
    public ResponseEntity<List<StockDto>> obtenirProduitsEnRupture() {
        List<StockDto> produits = stockService.obtenirProduitsEnRupture();
        return ResponseEntity.ok(produits);
    }

    /**
     * GET /api/stock/stock-bas
     * Obtenir les produits avec un stock bas
     *
     * Retourne tous les produits avec stock > 0 et < 10
     *
     * Exemple : GET /api/stock/stock-bas
     */
    @GetMapping("/stock-bas")
    public ResponseEntity<List<StockDto>> obtenirProduitsStockBas() {
        List<StockDto> produits = stockService.obtenirProduitsStockBas();
        return ResponseEntity.ok(produits);
    }

    /**
     * GET /api/stock/alertes
     * Obtenir toutes les alertes stock (rupture + stock bas)
     *
     * Combine les produits en rupture et à stock bas
     *
     * Exemple : GET /api/stock/alertes
     */
    @GetMapping("/alertes")
    public ResponseEntity<Map<String, Object>> obtenirAlertes() {
        List<StockDto> ruptures = stockService.obtenirProduitsEnRupture();
        List<StockDto> stocksBas = stockService.obtenirProduitsStockBas();

        Map<String, Object> alertes = new HashMap<>();
        alertes.put("ruptures", ruptures);
        alertes.put("nombreRuptures", ruptures.size());
        alertes.put("stocksBas", stocksBas);
        alertes.put("nombreStocksBas", stocksBas.size());
        alertes.put("nombreTotalAlertes", ruptures.size() + stocksBas.size());

        return ResponseEntity.ok(alertes);
    }

    /**
     * GET /api/stock/verifier?nomProduit=xxx&quantite=10
     * Vérifier si un produit a un stock suffisant
     *
     * @param nomProduit Nom du produit
     * @param quantite Quantité demandée
     *
     * Exemple : GET /api/stock/verifier?nomProduit=Collier&quantite=5
     */
    @GetMapping("/verifier")
    public ResponseEntity<Map<String, Object>> verifierStock(
            @RequestParam String nomProduit,
            @RequestParam Integer quantite) {

        boolean disponible = stockService.verifierStockDisponible(nomProduit, quantite);
        StockDto stock = null;

        try {
            stock = stockService.obtenirStockParNomProduit(nomProduit);
        } catch (RuntimeException e) {
            // Produit non trouvé
        }

        Map<String, Object> resultat = new HashMap<>();
        resultat.put("nomProduit", nomProduit);
        resultat.put("quantiteDemandee", quantite);
        resultat.put("stockDisponible", stock != null ? stock.getStockDisponible() : 0);
        resultat.put("disponible", disponible);
        resultat.put("message", disponible
                ? "Stock suffisant pour cette vente"
                : "Stock insuffisant ! Disponible : " + (stock != null ? stock.getStockDisponible() : 0));

        return ResponseEntity.ok(resultat);
    }

    /**
     * GET /api/stock/valeur-totale
     * Obtenir la valeur totale du stock
     *
     * Calcule la somme des valeurs de tous les stocks
     * (stock disponible × prix moyen d'achat)
     *
     * Exemple : GET /api/stock/valeur-totale
     */
    @GetMapping("/valeur-totale")
    public ResponseEntity<Map<String, Object>> obtenirValeurTotale() {
        BigDecimal valeurTotale = stockService.obtenirValeurTotaleStock();
        List<StockDto> stocks = stockService.obtenirTousLesStocks();

        Map<String, Object> resultat = new HashMap<>();
        resultat.put("valeurTotale", valeurTotale);
        resultat.put("nombreProduits", stocks.size());
        resultat.put("details", stocks);

        return ResponseEntity.ok(resultat);
    }

    /**
     * GET /api/stock/resume
     * Obtenir un résumé général du stock
     *
     * Statistiques globales :
     * - Nombre de produits
     * - Valeur totale
     * - Alertes
     * - Produits en stock
     *
     * Exemple : GET /api/stock/resume
     */
    @GetMapping("/resume")
    public ResponseEntity<Map<String, Object>> obtenirResume() {
        List<StockDto> stocks = stockService.obtenirTousLesStocks();
        BigDecimal valeurTotale = stockService.obtenirValeurTotaleStock();

        long produitsEnStock = stocks.stream()
                .filter(s -> s.getStockDisponible() > 0)
                .count();

        long produitsRupture = stocks.stream()
                .filter(s -> s.getStockDisponible() <= 0)
                .count();

        long produitsStockBas = stocks.stream()
                .filter(s -> s.getStockDisponible() > 0 && s.getStockDisponible() < 10)
                .count();

        Map<String, Object> resume = new HashMap<>();
        resume.put("nombreTotalProduits", stocks.size());
        resume.put("produitsEnStock", produitsEnStock);
        resume.put("produitsEnRupture", produitsRupture);
        resume.put("produitsStockBas", produitsStockBas);
        resume.put("valeurTotaleStock", valeurTotale);

        return ResponseEntity.ok(resume);
    }

    /**
     * GET /api/stock/produits-disponibles
     * Obtenir la liste des produits disponibles pour la vente
     *
     * Retourne uniquement les produits avec stock > 0
     * Trié par nom de produit (alphabétique)
     *
     * Utile pour :
     * - Liste déroulante dans le formulaire de vente
     * - Autocomplete dans Angular
     *
     * Exemple : GET /api/stock/produits-disponibles
     */
    @GetMapping("/produits-disponibles")
    public ResponseEntity<List<Map<String, Object>>> obtenirProduitsDisponibles() {
        List<StockDto> stocks = stockService.obtenirTousLesStocks();

        // Filtrer uniquement les produits avec stock > 0
        List<Map<String, Object>> produitsDisponibles = stocks.stream()
                .filter(s -> s.getStockDisponible() > 0)
                .sorted((s1, s2) -> s1.getNomProduit().compareToIgnoreCase(s2.getNomProduit()))
                .map(stock -> {
                    Map<String, Object> produit = new HashMap<>();
                    produit.put("nomProduit", stock.getNomProduit());
                    produit.put("stockDisponible", stock.getStockDisponible());
                    produit.put("prixMoyenVente", stock.getPrixMoyenVente());
                    produit.put("statut", stock.getStatut());
                    return produit;
                })
                .toList();

        return ResponseEntity.ok(produitsDisponibles);
    }

    /**
     * GET /api/stock/noms-produits
     * Obtenir uniquement la liste des noms de produits disponibles
     *
     * Version simplifiée pour une liste déroulante simple
     *
     * Exemple : GET /api/stock/noms-produits
     * Réponse : ["Collier en or", "Bracelet", "Bague"]
     */
    @GetMapping("/noms-produits")
    public ResponseEntity<List<String>> obtenirNomsProduits() {
        List<StockDto> stocks = stockService.obtenirTousLesStocks();

        List<String> nomsProduits = stocks.stream()
                .filter(s -> s.getStockDisponible() > 0)
                .map(StockDto::getNomProduit)
                .sorted(String::compareToIgnoreCase)
                .toList();

        return ResponseEntity.ok(nomsProduits);
    }
}
