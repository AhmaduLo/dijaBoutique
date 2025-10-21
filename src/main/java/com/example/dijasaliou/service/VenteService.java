package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.repository.VenteRepository;
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

    public VenteService(VenteRepository venteRepository) {
        this.venteRepository = venteRepository;
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

        // Associer l'utilisateur
        vente.setUtilisateur(utilisateur);

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

        validerVente(venteModifiee);

        venteExistante.setQuantite(venteModifiee.getQuantite());
        venteExistante.setNomProduit(venteModifiee.getNomProduit());
        venteExistante.setPrixUnitaire(venteModifiee.getPrixUnitaire());
        venteExistante.setDateVente(venteModifiee.getDateVente());
        venteExistante.setClient(venteModifiee.getClient());

        venteExistante.calculerPrixTotal();

        return venteRepository.save(venteExistante);
    }

    /**
     * Supprimer une vente
     */
    public void supprimerVente(Long id) {
        if (!venteRepository.existsById(id)) {
            throw new RuntimeException("Vente non trouvée avec l'ID : " + id);
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
}
