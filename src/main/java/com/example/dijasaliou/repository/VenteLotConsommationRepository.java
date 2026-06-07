package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.VenteLotConsommationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour les lignes de consommation FIFO (vente_lot_consommation).
 *
 * Toutes les requêtes d'agrégat utilisent date_vente_snapshot (dénormalisée)
 * pour éviter la jointure avec ventes et améliorer la performance.
 */
@Repository
public interface VenteLotConsommationRepository
        extends JpaRepository<VenteLotConsommationEntity, Long> {

    /** Lignes de consommation d'une vente donnée (filtre tenant via Hibernate filter). */
    List<VenteLotConsommationEntity> findByVenteId(String venteId);

    /** Lignes de consommation d'un achat (utile pour rollback ou audit). */
    List<VenteLotConsommationEntity> findByAchatId(String achatId);

    /**
     * Somme du bénéfice total sur une période, pour un tenant.
     * Filtre tenant EXPLICITE (utilisable dans tous les contextes).
     */
    @Query("""
            SELECT COALESCE(SUM(v.beneficeTotalLigne), 0)
            FROM VenteLotConsommationEntity v
            WHERE v.tenant = :tenant
              AND v.dateVenteSnapshot BETWEEN :debut AND :fin
            """)
    BigDecimal sumBeneficeBetween(@Param("tenant") TenantEntity tenant,
                                  @Param("debut") LocalDateTime debut,
                                  @Param("fin")   LocalDateTime fin);

    /**
     * Somme du coût d'achat (= prix_achat × quantité) sur une période, pour un tenant.
     * Permet de calculer la marge brute : marge = CA − coût.
     */
    @Query("""
            SELECT COALESCE(SUM(v.prixAchatUnitaireSnapshot * v.quantiteConsommee), 0)
            FROM VenteLotConsommationEntity v
            WHERE v.tenant = :tenant
              AND v.dateVenteSnapshot BETWEEN :debut AND :fin
            """)
    BigDecimal sumCoutAchatBetween(@Param("tenant") TenantEntity tenant,
                                   @Param("debut") LocalDateTime debut,
                                   @Param("fin")   LocalDateTime fin);

    /** Nombre de ventes distinctes ayant au moins une ligne de consommation sur la période. */
    @Query("""
            SELECT COUNT(DISTINCT v.vente.id)
            FROM VenteLotConsommationEntity v
            WHERE v.tenant = :tenant
              AND v.dateVenteSnapshot BETWEEN :debut AND :fin
            """)
    long countVentesAvecBeneficeBetween(@Param("tenant") TenantEntity tenant,
                                        @Param("debut") LocalDateTime debut,
                                        @Param("fin")   LocalDateTime fin);

    /**
     * Top N produits par bénéfice sur une période.
     * Retourne un Object[] : [nomProduit, totalBenefice, totalQuantite].
     */
    @Query("""
            SELECT v.vente.nomProduit,
                   SUM(v.beneficeTotalLigne)        AS totalBenefice,
                   SUM(v.quantiteConsommee)         AS totalQuantite
            FROM VenteLotConsommationEntity v
            WHERE v.tenant = :tenant
              AND v.dateVenteSnapshot BETWEEN :debut AND :fin
            GROUP BY v.vente.nomProduit
            ORDER BY totalBenefice DESC
            """)
    List<Object[]> findTopProduitsByBenefice(@Param("tenant") TenantEntity tenant,
                                             @Param("debut") LocalDateTime debut,
                                             @Param("fin")   LocalDateTime fin);

    /** Supprime toutes les lignes liées à une vente (utilisé lors d'une suppression de vente). */
    void deleteByVenteId(String venteId);

    /** Nombre de lignes pour un tenant donné (utile pour le diagnostic du backfill). */
    @Query("SELECT COUNT(v) FROM VenteLotConsommationEntity v WHERE v.tenant = :tenant")
    long countByTenant(@Param("tenant") TenantEntity tenant);

    /**
     * Agrège le bénéfice total par produit pour un tenant (toutes périodes confondues).
     * Retourne Object[] : [nomProduit (String), beneficeTotal (BigDecimal), quantiteTotaleVendue (Double)]
     *
     * Utilisé par StockService pour enrichir le DTO de stock avec le bénéfice réalisé sur chaque produit.
     */
    @Query("""
            SELECT v.vente.nomProduit,
                   SUM(v.beneficeTotalLigne),
                   SUM(v.quantiteConsommee)
            FROM VenteLotConsommationEntity v
            WHERE v.tenant = :tenant
            GROUP BY v.vente.nomProduit
            """)
    List<Object[]> sumBeneficeAndQuantiteByProduit(@Param("tenant") TenantEntity tenant);
}
