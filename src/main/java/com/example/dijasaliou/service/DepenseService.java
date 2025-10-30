package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.DepenseEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.DepenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service pour la logique métier des dépenses
 */
@Service
public class DepenseService {

    private final DepenseRepository depenseRepository;
    private final TenantService tenantService;

    public DepenseService(DepenseRepository depenseRepository, TenantService tenantService) {
        this.depenseRepository = depenseRepository;
        this.tenantService = tenantService;
    }


    /**
     * Récupérer toutes les dépenses
     */
    public List<DepenseEntity> obtenirToutesLesDepenses() {
        return depenseRepository.findAll();
    }

    /**
     * Récupérer une dépense par ID
     */
    public DepenseEntity obtenirDepenseParId(Long id) {
        return depenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée avec l'ID : " + id));
    }

    /**
     * Créer une nouvelle dépense
     */
    public DepenseEntity creerDepense(DepenseEntity depense, UserEntity utilisateur) {
        // Validation
        validerDepense(depense);

        // Associer l'utilisateur
        depense.setUtilisateur(utilisateur);

        // MULTI-TENANT : Assigner le tenant actuel (CRUCIAL!)
        depense.setTenant(tenantService.getCurrentTenant());

        return depenseRepository.save(depense);
    }

    /**
     * Modifier une dépense
     */
    public DepenseEntity modifierDepense(Long id, DepenseEntity depenseModifiee) {
        DepenseEntity depenseExistante = obtenirDepenseParId(id);

        // SÉCURITÉ : Vérifier que la dépense appartient au tenant actuel (double sécurité)
        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!depenseExistante.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        validerDepense(depenseModifiee);

        depenseExistante.setLibelle(depenseModifiee.getLibelle());
        depenseExistante.setMontant(depenseModifiee.getMontant());
        depenseExistante.setDateDepense(depenseModifiee.getDateDepense());
        depenseExistante.setCategorie(depenseModifiee.getCategorie());
        depenseExistante.setEstRecurrente(depenseModifiee.getEstRecurrente());
        depenseExistante.setNotes(depenseModifiee.getNotes());
        depenseExistante.setUtilisateur(depenseModifiee.getUtilisateur());

        return depenseRepository.save(depenseExistante);
    }

    /**
     * Supprimer une dépense
     */
    public void supprimerDepense(Long id) {
        // Récupérer la dépense existante
        DepenseEntity depenseExistante = obtenirDepenseParId(id);

        // SÉCURITÉ : Vérifier que la dépense appartient au tenant actuel (double sécurité)
        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!depenseExistante.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        depenseRepository.deleteById(id);
    }

    /**
     * Récupérer les dépenses d'un utilisateur
     */
    public List<DepenseEntity> obtenirDepensesParUtilisateur(UserEntity utilisateur) {
        return depenseRepository.findByUtilisateur(utilisateur);
    }

    /**
     * Récupérer les dépenses d'une période
     */
    public List<DepenseEntity> obtenirDepensesParPeriode(LocalDate debut, LocalDate fin) {
        return depenseRepository.findByDateDepenseBetween(debut, fin);
    }

    /**
     * Calculer le total des dépenses d'une période
     */
    public BigDecimal calculerTotalDepenses(LocalDate debut, LocalDate fin) {
        List<DepenseEntity> depenses = obtenirDepensesParPeriode(debut, fin);

        return depenses.stream()
                .map(DepenseEntity::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Récupérer les dépenses par catégorie
     */
    public List<DepenseEntity> obtenirDepensesParCategorie(DepenseEntity.CategorieDepense categorie) {
        return depenseRepository.findByCategorie(categorie);
    }

    /**
     * VALIDATION
     */
    private void validerDepense(DepenseEntity depense) {
        if (depense.getLibelle() == null || depense.getLibelle().trim().isEmpty()) {
            throw new IllegalArgumentException("Le libellé est obligatoire");
        }

        if (depense.getMontant() == null ||
                depense.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0");
        }

        if (depense.getCategorie() == null) {
            throw new IllegalArgumentException("La catégorie est obligatoire");
        }
    }
}
