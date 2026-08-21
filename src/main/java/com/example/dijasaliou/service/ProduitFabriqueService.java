package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.ProduitFabriqueEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.ProduitFabriqueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gère le catalogue des produits fabriqués (issus de production).
 * <p>
 * Ce catalogue ne représente aucun stock : il sert uniquement à alimenter
 * l'autocomplétion du champ "nom du produit" dans le formulaire "Nouvelle
 * production", et à retrouver la dernière production d'un produit pour
 * pré-remplir ses ingrédients. Il n'y a pas d'étape de création dédiée côté
 * utilisateur — un nom de produit fabriqué est créé automatiquement dès sa
 * première utilisation dans une production (voir {@link #trouverOuCreerProduitFabrique}),
 * exactement comme un nouveau produit se crée implicitement via le formulaire
 * d'achat classique.
 */
@Service
public class ProduitFabriqueService {

    private final ProduitFabriqueRepository produitFabriqueRepository;
    private final TenantService tenantService;

    public ProduitFabriqueService(ProduitFabriqueRepository produitFabriqueRepository, TenantService tenantService) {
        this.produitFabriqueRepository = produitFabriqueRepository;
        this.tenantService = tenantService;
    }

    @Transactional(readOnly = true)
    public List<ProduitFabriqueEntity> listerProduitsFabriquesActifs() {
        return produitFabriqueRepository.findAllByTenantAndActifTrueOrderByNomProduitAsc(tenantService.getCurrentTenant());
    }

    /**
     * Retrouve le produit fabriqué par son nom (insensible à la casse) pour
     * le tenant courant, ou le crée s'il n'existe pas encore.
     */
    @Transactional
    public ProduitFabriqueEntity trouverOuCreerProduitFabrique(String nomProduit) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        String nom = nomProduit != null ? nomProduit.trim() : null;
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du produit fabriqué est obligatoire");
        }

        return produitFabriqueRepository.findByTenantAndNomProduitIgnoreCase(tenant, nom)
                .orElseGet(() -> produitFabriqueRepository.save(
                        ProduitFabriqueEntity.builder()
                                .nomProduit(nom)
                                .unite("pièce")
                                .actif(true)
                                .tenant(tenant)
                                .build()
                ));
    }
}
