package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.AchatDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.AchatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    // MULTI-TENANT : Service pour récupérer le tenant actuel
    private final TenantService tenantService;

    private final StockService stockService;

    /**
     * Constructeur
     * Spring injecte automatiquement achatRepository, tenantService et stockService
     */
    public AchatService(AchatRepository achatRepository, TenantService tenantService,
                        StockService stockService) {
        this.achatRepository = achatRepository;
        this.tenantService = tenantService;
        this.stockService = stockService;
    }

    /**
     * Récupérer tous les achats du tenant courant
     */
    public List<AchatEntity> obtenirTousLesAchats() {
        return achatRepository.findAllByTenant(tenantService.getCurrentTenant());
    }

    /**
     * Récupérer les achats paginés avec recherche optionnelle et filtre de dates
     */
    public PagedResponse<AchatDto> obtenirAchatsPagines(int page, int size, String search, LocalDate dateDebut, LocalDate dateFin) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateAchat"));
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        Page<AchatEntity> achatsPage = achatRepository.findAllWithSearch(searchParam, dateDebut, dateFin, pageable);
        Page<AchatDto> dtoPage = achatsPage.map(AchatDto::fromEntity);
        return PagedResponse.from(dtoPage);
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
     * - MULTI-TENANT : Assignation automatique du tenant
     */
    public AchatEntity creerAchat(AchatEntity achat, UserEntity utilisateur) {

        // 1. VALIDATION : Vérifier que les données sont correctes
        validerAchat(achat);

        // 2. LOGIQUE : Associer l'utilisateur
        achat.setUtilisateur(utilisateur);

        // 3. MULTI-TENANT : Assigner le tenant actuel (CRUCIAL!)
        achat.setTenant(tenantService.getCurrentTenant());

        // 4. LOGIQUE : Calculer le prix total (si pas fait)
        if (achat.getPrixTotal() == null) {
            achat.calculerPrixTotal();
        }

        // 5. SAUVEGARDE : Enregistrer en base
        AchatEntity saved = achatRepository.save(achat);
        stockService.invalidateStockCache(saved.getTenant().getTenantUuid());
        return saved;
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

        // 2. SÉCURITÉ : Vérifier que l'achat appartient au tenant actuel (double sécurité)
        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!achatExistant.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        // 3. Valider les nouvelles données
        validerAchat(achatModifie);

        // 4. Vérifier que la réduction de quantité ne rend pas le stock négatif
        boolean memeProduit = achatExistant.getNomProduit().equalsIgnoreCase(achatModifie.getNomProduit());
        if (memeProduit && achatModifie.getQuantite() < achatExistant.getQuantite()) {
            int reduction = achatExistant.getQuantite() - achatModifie.getQuantite();
            try {
                StockDto stock = stockService.obtenirStockParNomProduit(achatExistant.getNomProduit());
                if (stock.getStockDisponible() - reduction < 0) {
                    throw new IllegalArgumentException(
                            String.format("Impossible de réduire la quantité : stock disponible %d, réduction demandée %d",
                                    stock.getStockDisponible(), reduction));
                }
            } catch (RuntimeException e) {
                if (e instanceof IllegalArgumentException) throw e;
                // Produit non trouvé → pas de ventes → réduction toujours safe
            }
        }

        // 5. Mettre à jour les champs
        achatExistant.setQuantite(achatModifie.getQuantite());
        achatExistant.setNomProduit(achatModifie.getNomProduit());
        achatExistant.setPrixUnitaire(achatModifie.getPrixUnitaire());
        achatExistant.setDateAchat(achatModifie.getDateAchat());
        achatExistant.setFournisseur(achatModifie.getFournisseur());
        achatExistant.setUtilisateur(achatModifie.getUtilisateur());
        achatExistant.setPhotoUrl(achatModifie.getPhotoUrl());
        achatExistant.setUnite(achatModifie.getUnite());

        // NOTE : On ne modifie PAS le tenant pour des raisons de sécurité
        // Le tenant est défini à la création et ne change jamais

        // 5. Recalculer le prix total
        achatExistant.calculerPrixTotal();

        // 6. Sauvegarder + invalider le cache tenant
        AchatEntity saved = achatRepository.save(achatExistant);
        stockService.invalidateStockCache(saved.getTenant().getTenantUuid());
        return saved;
    }

    /**
     * Supprimer un achat
     */
    public void supprimerAchat(Long id) {
        // 1. Récupérer l'achat existant
        AchatEntity achatExistant = obtenirAchatParId(id);

        // 2. SÉCURITÉ : Vérifier que l'achat appartient au tenant actuel (double sécurité)
        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!achatExistant.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        // 3. Supprimer + invalider le cache tenant
        achatRepository.deleteById(id);
        stockService.invalidateStockCache(tenantActuel.getTenantUuid());
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

    /**
     * Récupérer tous les produits avec leurs prix de vente suggérés
     * Cette méthode est utilisée pour permettre aux utilisateurs de type USER
     * de créer des ventes avec des prix pré-remplis, sans avoir accès aux
     * informations sensibles des achats (prix d'achat, fournisseur, etc.)
     *
     * @return Liste des achats avec uniquement les infos nécessaires pour la vente
     */
    public List<AchatEntity> obtenirProduitsAvecPrixVente() {
        return achatRepository.findAllByTenant(tenantService.getCurrentTenant());
    }

}
