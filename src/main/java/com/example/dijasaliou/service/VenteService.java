package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.repository.VenteRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service pour la logique métier des ventes
 */
@Service
public class VenteService {

    private final VenteRepository venteRepository;
    private final StockService stockService;
    private final TenantService tenantService;

    public VenteService(VenteRepository venteRepository, @Lazy StockService stockService, TenantService tenantService) {
        this.venteRepository = venteRepository;
        this.stockService = stockService;
        this.tenantService = tenantService;
    }

    /**
     * Récupérer toutes les ventes
     */
    public List<VenteEntity> obtenirToutesLesVentes() {
        return venteRepository.findAll();
    }

    /**
     * Récupérer une vente par ID
     */
    public VenteEntity obtenirVenteParId(Long id) {
        return venteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'ID : " + id));
    }

    /**
     * Créer une nouvelle vente
     */
    public VenteEntity creerVente(VenteEntity vente, UserEntity utilisateur) {
        // Validation
        validerVente(vente);

        // Vérifier le stock disponible
        verifierStockAvantVente(vente.getNomProduit(), vente.getQuantite());

        // Associer l'utilisateur
        vente.setUtilisateur(utilisateur);

        // MULTI-TENANT : Assigner le tenant actuel (CRUCIAL!)
        vente.setTenant(tenantService.getCurrentTenant());

        // Calculer le prix total
        if (vente.getPrixTotal() == null) {
            vente.calculerPrixTotal();
        }

        return venteRepository.save(vente);
    }

    /**
     * Modifier une vente
     */
    public VenteEntity modifierVente(Long id, VenteEntity venteModifiee) {
        VenteEntity venteExistante = obtenirVenteParId(id);

        // SÉCURITÉ : Vérifier que la vente appartient au tenant actuel (double sécurité)
        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!venteExistante.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        validerVente(venteModifiee);

        venteExistante.setQuantite(venteModifiee.getQuantite());
        venteExistante.setNomProduit(venteModifiee.getNomProduit());
        venteExistante.setPrixUnitaire(venteModifiee.getPrixUnitaire());
        venteExistante.setDateVente(venteModifiee.getDateVente());
        venteExistante.setClient(venteModifiee.getClient());
        venteExistante.setUtilisateur(venteModifiee.getUtilisateur());

        venteExistante.calculerPrixTotal();

        return venteRepository.save(venteExistante);
    }

    /**
     * Supprimer une vente
     */
    public void supprimerVente(Long id) {
        // Récupérer la vente existante
        VenteEntity venteExistante = obtenirVenteParId(id);

        // SÉCURITÉ : Vérifier que la vente appartient au tenant actuel (double sécurité)
        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!venteExistante.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        venteRepository.deleteById(id);
    }

    /**
     * Récupérer les ventes d'un utilisateur
     */
    public List<VenteEntity> obtenirVentesParUtilisateur(UserEntity utilisateur) {
        return venteRepository.findByUtilisateur(utilisateur);
    }

    /**
     * Récupérer les ventes d'une période
     */
    public List<VenteEntity> obtenirVentesParPeriode(LocalDate debut, LocalDate fin) {
        return venteRepository.findByDateVenteBetween(debut, fin);
    }

    /**
     * Calculer le chiffre d'affaires d'une période
     */
    public BigDecimal calculerChiffreAffaires(LocalDate debut, LocalDate fin) {
        List<VenteEntity> ventes = obtenirVentesParPeriode(debut, fin);

        return ventes.stream()
                .map(VenteEntity::getPrixTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * VALIDATION
     */
    private void validerVente(VenteEntity vente) {
        if (vente.getQuantite() == null || vente.getQuantite() <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à 0");
        }

        if (vente.getPrixUnitaire() == null ||
                vente.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix unitaire doit être supérieur à 0");
        }

        if (vente.getNomProduit() == null || vente.getNomProduit().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire");
        }
    }

    /**
     * Vérifier le stock avant une vente
     * Lance une exception si le stock est insuffisant
     */
    private void verifierStockAvantVente(String nomProduit, Integer quantite) {
        try {
            StockDto stock = stockService.obtenirStockParNomProduit(nomProduit);

            if (!stock.estSuffisant(quantite)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Stock insuffisant pour '%s' ! Disponible : %d, Demandé : %d",
                                nomProduit,
                                stock.getStockDisponible(),
                                quantite
                        )
                );
            }
        } catch (RuntimeException e) {
            // Si le produit n'existe pas dans les achats, on peut quand même vendre
            // (cas d'un produit jamais acheté mais qu'on souhaite vendre)
            // Commentez cette partie si vous voulez forcer l'achat avant la vente
            if (e.getMessage().contains("Produit non trouvé")) {
                // Permettre la vente même sans achat préalable
                return;
            }
            throw e;
        }
    }
}
