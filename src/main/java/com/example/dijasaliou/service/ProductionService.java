package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.CreerProductionRequest;
import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.ProduitFabriqueEntity;
import com.example.dijasaliou.entity.ProductionEntity;
import com.example.dijasaliou.entity.ProductionIngredientEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.ProductionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enregistre une production : le commerçant a combiné des ingrédients pour
 * fabriquer N unités d'un produit fabriqué.
 * <p>
 * PRINCIPE : ne réinvente aucun moteur de stock/FIFO/bénéfice. Une production
 * crée simplement un lot {@link AchatEntity} normal pour le produit fabriqué
 * (via {@link AchatService#creerAchat}) — c'est ce lot que
 * {@code FifoCalculService} consommera automatiquement à la vente, exactement
 * comme n'importe quel autre achat. Le bénéfice se retrouve donc rattaché
 * aux unités VENDUES et pas aux unités produites, sans code supplémentaire.
 * <p>
 * Les lignes d'ingrédients ne sont JAMAIS insérées dans {@code achats} —
 * ça compterait le coût de production deux fois. Elles sont sauvegardées à
 * part, uniquement pour la traçabilité.
 */
@Service
public class ProductionService {

    private final ProductionRepository productionRepository;
    private final ProduitFabriqueService produitFabriqueService;
    private final AchatService achatService;
    private final TenantService tenantService;

    public ProductionService(ProductionRepository productionRepository,
                              ProduitFabriqueService produitFabriqueService,
                              AchatService achatService,
                              TenantService tenantService) {
        this.productionRepository = productionRepository;
        this.produitFabriqueService = produitFabriqueService;
        this.achatService = achatService;
        this.tenantService = tenantService;
    }

    @Transactional(readOnly = true)
    public List<ProductionEntity> listerProductions() {
        return productionRepository.findAllByTenantOrderByDateProductionDesc(tenantService.getCurrentTenant());
    }

    /** Retrouve la production liée à un lot d'achat — alimente le détail "Ingrédients" dans la page Achats. */
    @Transactional(readOnly = true)
    public Optional<ProductionEntity> trouverParAchatId(String achatId) {
        return productionRepository.findByAchat_IdAndTenant(achatId, tenantService.getCurrentTenant());
    }

    /**
     * Ingrédients de la dernière production enregistrée pour ce nom de
     * produit fabriqué — utilisé pour pré-remplir le formulaire quand le
     * commerçant retape un nom déjà utilisé. Liste vide si aucune production
     * antérieure n'existe pour ce nom.
     */
    @Transactional(readOnly = true)
    public Optional<ProductionEntity> trouverDerniereProduction(String nomProduit) {
        if (nomProduit == null || nomProduit.isBlank()) {
            return Optional.empty();
        }
        return productionRepository.findFirstByTenantAndProduitFabrique_NomProduitIgnoreCaseOrderByDateProductionDescCreatedDateDesc(
                tenantService.getCurrentTenant(), nomProduit.trim());
    }

    @Transactional
    public ProductionEntity creerProduction(CreerProductionRequest request, UserEntity utilisateur) {
        validerRequete(request);

        TenantEntity tenant = tenantService.getCurrentTenant();

        // Le produit fabriqué est identifié par son nom — créé automatiquement
        // à la première utilisation, comme un nouveau produit dans le formulaire d'achat.
        ProduitFabriqueEntity produitFabrique = produitFabriqueService.trouverOuCreerProduitFabrique(request.getNomProduit());
        Couts couts = calculerCouts(request);
        LocalDateTime dateProduction = request.getDateProduction() != null
                ? request.getDateProduction() : LocalDateTime.now();

        // Créer le lot de stock via le chemin normal des achats — aucune logique
        // stock/cache/FIFO dupliquée ici. prixTotal n'est pas fourni : AchatService
        // le recalcule automatiquement (prixUnitaire × quantite).
        AchatEntity lotProduction = AchatEntity.builder()
                .nomProduit(produitFabrique.getNomProduit())
                .quantite(request.getQuantiteProduite())
                .prixUnitaire(couts.unitaireFinal())
                .dateAchat(dateProduction)
                .categorie("Production")
                .unite(produitFabrique.getUnite())
                .prixVenteSuggere(request.getPrixVenteSuggere())
                .photoUrl(request.getPhotoUrl())
                .modePaiement(request.getModePaiement())
                .build();

        AchatEntity achatCree = achatService.creerAchat(lotProduction, utilisateur);

        // Enregistrer la production + le détail des ingrédients (traçabilité uniquement)
        ProductionEntity production = ProductionEntity.builder()
                .produitFabrique(produitFabrique)
                .achat(achatCree)
                .utilisateur(utilisateur)
                .tenant(tenant)
                .quantiteProduite(request.getQuantiteProduite())
                .coutIngredientsTotal(couts.ingredientsTotal())
                .coutUnitaire(couts.unitaireFinal())
                .dateProduction(dateProduction)
                .notes(request.getNotes())
                .build();

        production.setIngredients(construireIngredients(production, tenant, request.getIngredients()));

        return productionRepository.save(production);
    }

    /**
     * Modifie une production existante : met à jour le lot de stock lié via
     * {@link AchatService#modifierAchat} (mêmes garde-fous que la modification
     * d'un achat classique — notamment le blocage si la réduction de quantité
     * rendrait le stock négatif), puis remplace les lignes d'ingrédients.
     */
    @Transactional
    public ProductionEntity modifierProduction(String id, CreerProductionRequest request, UserEntity utilisateur) {
        validerRequete(request);

        TenantEntity tenant = tenantService.getCurrentTenant();
        ProductionEntity production = productionRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new RuntimeException("Production non trouvée"));

        ProduitFabriqueEntity produitFabrique = produitFabriqueService.trouverOuCreerProduitFabrique(request.getNomProduit());
        Couts couts = calculerCouts(request);
        LocalDateTime dateProduction = request.getDateProduction() != null
                ? request.getDateProduction() : production.getDateProduction();

        // Met à jour le lot d'achat via le chemin normal de modification des achats —
        // réutilise les mêmes garde-fous (stock négatif, recalcul du prix total) sans
        // dupliquer cette logique ici.
        AchatEntity achatModifie = AchatEntity.builder()
                .nomProduit(produitFabrique.getNomProduit())
                .quantite(request.getQuantiteProduite())
                .prixUnitaire(couts.unitaireFinal())
                .dateAchat(dateProduction)
                .categorie("Production")
                .unite(produitFabrique.getUnite())
                .prixVenteSuggere(request.getPrixVenteSuggere())
                .photoUrl(request.getPhotoUrl())
                .modePaiement(request.getModePaiement())
                .utilisateur(utilisateur)
                .build();
        achatService.modifierAchat(production.getAchat().getId(), achatModifie);

        production.setProduitFabrique(produitFabrique);
        production.setQuantiteProduite(request.getQuantiteProduite());
        production.setCoutIngredientsTotal(couts.ingredientsTotal());
        production.setCoutUnitaire(couts.unitaireFinal());
        production.setDateProduction(dateProduction);
        production.setNotes(request.getNotes());

        // Remplace entièrement les lignes d'ingrédients (orphanRemoval supprime les anciennes).
        production.getIngredients().clear();
        production.getIngredients().addAll(construireIngredients(production, tenant, request.getIngredients()));

        return productionRepository.save(production);
    }

    /** Coût total des ingrédients et coût unitaire — tous deux calculés, jamais saisis directement. */
    private record Couts(BigDecimal ingredientsTotal, BigDecimal unitaireFinal) {}

    /**
     * Le prix d'achat (coût par unité) est TOUJOURS calculé à partir des ingrédients — un
     * éventuel {@code request.getPrixUnitaire()} envoyé par le client est ignoré. Ce n'est
     * plus un champ modifiable côté formulaire ; l'ignorer ici empêche aussi de le contourner
     * via un appel direct à l'API.
     */
    private Couts calculerCouts(CreerProductionRequest request) {
        BigDecimal ingredientsTotal = request.getIngredients().stream()
                .map(CreerProductionRequest.IngredientLigne::getCout)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unitaireCalcule = ingredientsTotal.divide(
                BigDecimal.valueOf(request.getQuantiteProduite()), 2, RoundingMode.HALF_UP);

        return new Couts(ingredientsTotal, unitaireCalcule);
    }

    private List<ProductionIngredientEntity> construireIngredients(ProductionEntity production, TenantEntity tenant,
                                                                     List<CreerProductionRequest.IngredientLigne> lignes) {
        List<ProductionIngredientEntity> ingredients = new ArrayList<>();
        int ordre = 0;
        for (CreerProductionRequest.IngredientLigne ligne : lignes) {
            ingredients.add(ProductionIngredientEntity.builder()
                    .production(production)
                    .tenant(tenant)
                    .nomIngredient(ligne.getNomIngredient())
                    .unite(ligne.getUnite())
                    .cout(ligne.getCout())
                    .ordre(ordre++)
                    .build());
        }
        return ingredients;
    }

    private void validerRequete(CreerProductionRequest request) {
        if (request.getQuantiteProduite() == null || request.getQuantiteProduite() <= 0) {
            throw new IllegalArgumentException("La quantité produite doit être supérieure à 0");
        }
        if (request.getIngredients() == null || request.getIngredients().isEmpty()) {
            throw new IllegalArgumentException("Au moins un ingrédient est requis");
        }
        for (CreerProductionRequest.IngredientLigne ligne : request.getIngredients()) {
            if (ligne.getNomIngredient() == null || ligne.getNomIngredient().isBlank()) {
                throw new IllegalArgumentException("Le nom de chaque ingrédient est obligatoire");
            }
            if (ligne.getCout() == null || ligne.getCout().compareTo(BigDecimal.valueOf(0.01)) < 0) {
                throw new IllegalArgumentException("Le coût de chaque ingrédient doit être supérieur à 0");
            }
        }
    }
}
