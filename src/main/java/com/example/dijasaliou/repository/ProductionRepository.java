package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.ProductionEntity;
import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionRepository extends JpaRepository<ProductionEntity, String> {

    List<ProductionEntity> findAllByTenantOrderByDateProductionDesc(TenantEntity tenant);

    /** Retrouve la production associée à un lot d'achat — alimente le détail "Ingrédients" dans la page Achats. */
    Optional<ProductionEntity> findByAchat_IdAndTenant(String achatId, TenantEntity tenant);

    /** Sécurité : vérifie que la production appartient bien au tenant avant modification. */
    Optional<ProductionEntity> findByIdAndTenant(String id, TenantEntity tenant);

    /**
     * Dernière production enregistrée pour un nom de produit fabriqué donné —
     * alimente le pré-remplissage des ingrédients dans le formulaire
     * "Nouvelle production" quand le commerçant retape un nom déjà utilisé.
     * <p>
     * Tri par dateProduction PUIS createdDate : dateProduction vient d'un simple
     * sélecteur de date (granularité jour) côté frontend — deux productions du même
     * jour ont exactement la même valeur, donc un tri sur ce seul champ ne départage
     * pas de façon fiable laquelle est réellement la plus récente. createdDate (horodatage
     * précis, posé automatiquement à la création) sert de critère de départage.
     */
    Optional<ProductionEntity> findFirstByTenantAndProduitFabrique_NomProduitIgnoreCaseOrderByDateProductionDescCreatedDateDesc(
            TenantEntity tenant, String nomProduit);
}
