package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.AchatRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service pour la logique métier des achats
 *
 * @Service : Dit à Spring que c'est un service
 * Spring va créer automatiquement une instance (injection de dépendances)
 */
@Service
public class AchatService {

    // Le repository pour accéder à la base
    private final AchatRepository achatRepository;

    /**
     * Constructeur
     * Spring injecte automatiquement achatRepository
     */
    public AchatService(AchatRepository achatRepository) {
        this.achatRepository = achatRepository;
    }

    /**
     * MÉTHODE 1 : Récupérer tous les achats
     *
     * Simple : Juste appeler le repository
     * Pas de logique métier ici
     */
    public List<AchatEntity> obtenirTousLesAchats() {
        return achatRepository.findAll();
    }

    /**
     * Récupérer un achat par son ID
     */
    public AchatEntity obtenirAchatParId(Long id) {
        return achatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Achat non trouvé avec l'ID : " + id));
    }

    /**
     * MÉTHODE 2 : Créer un nouvel achat
     * AVEC LOGIQUE MÉTIER :
     * - Validation des données
     * - Calcul du prix total
     * - Vérifications métier
     */
    public AchatEntity creerAchat(AchatEntity achat, UserEntity utilisateur) {

        // 1. VALIDATION : Vérifier que les données sont correctes
        validerAchat(achat);
        // 2. LOGIQUE : Associer l'utilisateur
        achat.setUtilisateur(utilisateur);

        // 3. LOGIQUE : Calculer le prix total (si pas fait)
        if (achat.getPrixTotal() == null) {
            achat.calculerPrixTotal();
        }

        // 4. SAUVEGARDE : Enregistrer en base
        return achatRepository.save(achat);
    }

    /**
     * MÉTHODE PRIVÉE : Validation métier
     *
     * Cette méthode vérifie les règles métier
     */
    private void validerAchat(AchatEntity achat) {
        // Règle 1 : Quantité doit être > 0
        if (achat.getQuantite() == null || achat.getQuantite() <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à 0");
        }
        // Règle 2 : Prix unitaire doit être > 0
        if (achat.getPrixUnitaire() == null ||
                achat.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix unitaire doit être supérieur à 0");
        }

        // Règle 3 : Nom du produit obligatoire
        if (achat.getNomProduit() == null || achat.getNomProduit().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire");
        }
    }

    /**
     * Modifier un achat existant
     */
    public AchatEntity modifierAchat(Long id, AchatEntity achatModifie) {
        // 1. Vérifier que l'achat existe
        AchatEntity achatExistant = obtenirAchatParId(id);

        // 2. Valider les nouvelles données
        validerAchat(achatModifie);

        // 3. Mettre à jour les champs
        achatExistant.setQuantite(achatModifie.getQuantite());
        achatExistant.setNomProduit(achatModifie.getNomProduit());
        achatExistant.setPrixUnitaire(achatModifie.getPrixUnitaire());
        achatExistant.setDateAchat(achatModifie.getDateAchat());
        achatExistant.setFournisseur(achatModifie.getFournisseur());

        // 4. Recalculer le prix total
        achatExistant.calculerPrixTotal();

        // 5. Sauvegarder
        return achatRepository.save(achatExistant);
    }

    /**
     * Supprimer un achat
     */
    public void supprimerAchat(Long id) {
        // 1. Vérifier que l'achat existe
        if (!achatRepository.existsById(id)) {
            throw new RuntimeException("Achat non trouvé avec l'ID : " + id);
        }

        // 2. Supprimer
        achatRepository.deleteById(id);
    }

    /**
     * Récupérer les achats d'un utilisateur
     */
    public List<AchatEntity> obtenirAchatsParUtilisateur(UserEntity utilisateur) {
        return achatRepository.findByUtilisateur(utilisateur);
    }


    /**
     * Récupérer les achats d'une période
     */
    public List<AchatEntity> obtenirAchatsParPeriode(LocalDate debut, LocalDate fin) {
        return achatRepository.findByDateAchatBetween(debut, fin);
    }

    /**
     * Calculer le total des achats d'une période
     *
     * LOGIQUE MÉTIER : Parcourir tous les achats et additionner
     */
    public BigDecimal calculerTotalAchats(LocalDate debut, LocalDate fin) {
        List<AchatEntity> achats = obtenirAchatsParPeriode(debut, fin);

        // Additionner tous les prix totaux
        return achats.stream()
                .map(AchatEntity::getPrixTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }



}
