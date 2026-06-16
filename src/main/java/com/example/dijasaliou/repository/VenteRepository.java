package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VenteRepository extends JpaRepository<VenteEntity, String> {

    // Recherche par produit (filtre tenant via Hibernate filter)
    List<VenteEntity> findByNomProduit(String nomProduit);

    List<VenteEntity> findByNomProduitContaining(String keyword);

    /**
     * Trouver les ventes d'un produit pour un tenant donné — filtre tenant EXPLICITE.
     * Utiliser à la place de findByNomProduit() dans les contextes sans filtre Hibernate.
     */
    @Query("SELECT v FROM VenteEntity v WHERE v.nomProduit = :nomProduit AND v.tenant = :tenant")
    List<VenteEntity> findByNomProduitAndTenant(@Param("nomProduit") String nomProduit, @Param("tenant") TenantEntity tenant);

    /**
     * Trouver les ventes contenant un mot, pour un tenant donné — filtre tenant EXPLICITE.
     */
    @Query("SELECT v FROM VenteEntity v WHERE LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :keyword, '%')) AND v.tenant = :tenant")
    List<VenteEntity> findByNomProduitContainingAndTenant(@Param("keyword") String keyword, @Param("tenant") TenantEntity tenant);

    // Recherche par utilisateur
    List<VenteEntity> findByUtilisateur(UserEntity utilisateur);

    // Recherche par date
    List<VenteEntity> findByDateVente(LocalDateTime date);

    @Query("SELECT v FROM VenteEntity v JOIN FETCH v.utilisateur JOIN FETCH v.tenant WHERE v.dateVente BETWEEN :debut AND :fin ORDER BY v.dateVente DESC, v.id DESC")
    List<VenteEntity> findByDateVenteBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    // Recherche par client
    List<VenteEntity> findByClient(String client);

    List<VenteEntity> findByClientIsNotNull();  // Ventes avec client

    List<VenteEntity> findByClientIsNull();  // Ventes sans client

    // Recherche par prix
    List<VenteEntity> findByPrixTotalGreaterThan(BigDecimal montant);

    // Comptage
    long countByNomProduit(String nomProduit);

    /**
     * Supprimer toutes les ventes d'un utilisateur
     */
    void deleteByUtilisateur(UserEntity utilisateur);

    /**
     * Calculer le total des quantités vendues pour un produit et un tenant
     * Utilisé pour le calcul de stock
     */
    @Query("SELECT SUM(v.quantite) FROM VenteEntity v WHERE v.nomProduit = :nomProduit AND v.tenant = :tenant")
    Double sumQuantiteByNomProduitAndTenant(@Param("nomProduit") String nomProduit, @Param("tenant") TenantEntity tenant);

    /**
     * Récupère toutes les ventes d'un tenant (filtre explicite — évite findAll())
     */
    @Query("SELECT v FROM VenteEntity v WHERE v.tenant = :tenant ORDER BY v.nomProduit")
    List<VenteEntity> findAllByTenant(@Param("tenant") TenantEntity tenant);

    /**
     * Toutes les ventes d'un tenant triées par date de vente ASC (pour le backfill FIFO).
     */
    @Query("SELECT v FROM VenteEntity v WHERE v.tenant = :tenant ORDER BY v.dateVente ASC, v.id ASC")
    List<VenteEntity> findAllByTenantOrderByDateAsc(@Param("tenant") TenantEntity tenant);

    /**
     * Somme des ventes d'un tenant pour un mode de paiement, entre deux dates.
     * Utilisé par le module Caisse pour calculer les entrées par compte
     * (avec borne supérieure pour les snapshots).
     */
    @Query("""
            SELECT COALESCE(SUM(v.prixTotal), 0)
            FROM VenteEntity v
            WHERE v.tenant = :tenant
              AND v.modePaiement = :modePaiement
              AND v.dateVente >= :debut
              AND v.dateVente <= :fin
            """)
    BigDecimal sumByModePaiementBetween(
            @Param("tenant") TenantEntity tenant,
            @Param("modePaiement") VenteEntity.ModePaiementVente modePaiement,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /**
     * Optimisation caisse : retourne le total des ventes GROUPÉ par mode
     * en une seule query. Résultat : List<[ModePaiementVente, BigDecimal]>.
     */
    @Query("""
            SELECT v.modePaiement, COALESCE(SUM(v.prixTotal), 0)
            FROM VenteEntity v
            WHERE v.tenant = :tenant
              AND v.dateVente >= :debut
              AND v.dateVente <= :fin
            GROUP BY v.modePaiement
            """)
    java.util.List<Object[]> sumByModePaiementGrouped(
            @Param("tenant") TenantEntity tenant,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /**
     * Calcule le chiffre d'affaires d'une période directement en SQL (évite le chargement en mémoire).
     * Exclut systématiquement les sorties hors vente (typeSortie != null).
     */
    @Query("SELECT COALESCE(SUM(v.prixTotal), 0) FROM VenteEntity v " +
           "WHERE v.dateVente BETWEEN :debut AND :fin " +
           "AND v.tenant.tenantUuid = :tenantUuid " +
           "AND v.typeSortie IS NULL")
    BigDecimal sumChiffreAffairesPeriode(@Param("debut") LocalDateTime debut,
                                         @Param("fin") LocalDateTime fin,
                                         @Param("tenantUuid") String tenantUuid);

    /**
     * CA des ventes payées immédiatement (mode != CREDIT) sur une période.
     * Sert la comptabilité de caisse : seules les ventes encaissées au moment de l'enregistrement
     * comptent ; les ventes crédit sont prises en compte via la table paiements_credit.
     * Exclut aussi les sorties hors vente.
     */
    @Query("SELECT COALESCE(SUM(v.prixTotal), 0) FROM VenteEntity v " +
           "WHERE v.dateVente BETWEEN :debut AND :fin " +
           "AND v.tenant.tenantUuid = :tenantUuid " +
           "AND v.modePaiement <> com.example.dijasaliou.entity.VenteEntity.ModePaiementVente.CREDIT " +
           "AND v.typeSortie IS NULL")
    BigDecimal sumChiffreAffairesNonCreditPeriode(@Param("debut") LocalDateTime debut,
                                                  @Param("fin") LocalDateTime fin,
                                                  @Param("tenantUuid") String tenantUuid);

    /**
     * Nombre de ventes non-crédit sur une période (pour le décompte "ventes encaissées").
     * Exclut les sorties hors vente.
     */
    @Query("SELECT COUNT(v) FROM VenteEntity v " +
           "WHERE v.dateVente BETWEEN :debut AND :fin " +
           "AND v.tenant.tenantUuid = :tenantUuid " +
           "AND v.modePaiement <> com.example.dijasaliou.entity.VenteEntity.ModePaiementVente.CREDIT " +
           "AND v.typeSortie IS NULL")
    long countVentesNonCreditPeriode(@Param("debut") LocalDateTime debut,
                                     @Param("fin") LocalDateTime fin,
                                     @Param("tenantUuid") String tenantUuid);

    /**
     * Groupe les sorties hors vente (perte, vol, casse, don, crédit impayé) par type sur une période.
     * Retourne List<Object[]> : [typeSortie, count, sumPrixTotal].
     * Utilisé par le widget "Pertes du mois" sur le dashboard ventes.
     */
    @Query("SELECT v.typeSortie, COUNT(v), COALESCE(SUM(v.prixTotal), 0) " +
           "FROM VenteEntity v " +
           "WHERE v.dateVente BETWEEN :debut AND :fin " +
           "AND v.tenant.tenantUuid = :tenantUuid " +
           "AND v.typeSortie IS NOT NULL " +
           "GROUP BY v.typeSortie")
    java.util.List<Object[]> sumSortiesParTypeEtPeriode(@Param("debut") LocalDateTime debut,
                                                        @Param("fin") LocalDateTime fin,
                                                        @Param("tenantUuid") String tenantUuid);

    /**
     * Liste détaillée des sorties hors vente d'une période — triée par date décroissante.
     * Utilisée par la modale "Pertes" pour afficher la liste filtrable par mois/année.
     * Charge l'utilisateur en fetch pour éviter les N+1 dans le DTO.
     */
    @Query("SELECT v FROM VenteEntity v " +
           "JOIN FETCH v.utilisateur " +
           "JOIN FETCH v.tenant " +
           "WHERE v.dateVente BETWEEN :debut AND :fin " +
           "AND v.tenant.tenantUuid = :tenantUuid " +
           "AND v.typeSortie IS NOT NULL " +
           "ORDER BY v.dateVente DESC, v.id DESC")
    java.util.List<VenteEntity> findSortiesBetween(@Param("debut") LocalDateTime debut,
                                                   @Param("fin") LocalDateTime fin,
                                                   @Param("tenantUuid") String tenantUuid);

    @Query("SELECT COUNT(v) FROM VenteEntity v WHERE v.tenant.tenantUuid = :tenantUuid")
    long countByTenantUuid(@Param("tenantUuid") String tenantUuid);

    @Query("SELECT COUNT(v) FROM VenteEntity v WHERE v.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Recherche paginée avec filtre tenant EXPLICITE — permet l'utilisation de idx_vente_tenant_date.
     */
    @Query(value = "SELECT v FROM VenteEntity v JOIN FETCH v.utilisateur JOIN FETCH v.tenant WHERE " +
           "v.tenant.tenantUuid = :tenantUuid AND " +
           "(:search IS NULL OR LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.client) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.telephoneClient) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR v.dateVente >= :dateDebut) AND " +
           "(:dateFin IS NULL OR v.dateVente <= :dateFin) " +
           "ORDER BY v.dateVente DESC, v.id DESC",
           countQuery = "SELECT COUNT(v) FROM VenteEntity v WHERE " +
           "v.tenant.tenantUuid = :tenantUuid AND " +
           "(:search IS NULL OR LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.client) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.telephoneClient) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR v.dateVente >= :dateDebut) AND " +
           "(:dateFin IS NULL OR v.dateVente <= :dateFin)")
    Page<VenteEntity> findAllWithSearch(@Param("tenantUuid") String tenantUuid,
                                        @Param("search") String search,
                                        @Param("dateDebut") LocalDateTime dateDebut,
                                        @Param("dateFin") LocalDateTime dateFin,
                                        Pageable pageable);

    /**
     * Recherche paginée par utilisateur avec filtre optionnel et plage de dates
     */
    /**
     * Agrège les ventes directes (hors CREDIT) par mode de paiement sur une période.
     * Retourne Object[] : [modePaiement, count, sum(prixTotal)]
     */
    @Query("SELECT v.modePaiement, COUNT(v), COALESCE(SUM(v.prixTotal), 0) " +
           "FROM VenteEntity v " +
           "WHERE v.dateVente BETWEEN :debut AND :fin " +
           "AND v.modePaiement <> :creditMode " +
           "AND v.tenant.tenantUuid = :tenantUuid " +
           "GROUP BY v.modePaiement")
    List<Object[]> sumDirectVentesParModeEtPeriode(@Param("debut") LocalDateTime debut,
                                                   @Param("fin") LocalDateTime fin,
                                                   @Param("creditMode") VenteEntity.ModePaiementVente creditMode,
                                                   @Param("tenantUuid") String tenantUuid);

    @Query(value = "SELECT v FROM VenteEntity v WHERE v.utilisateur = :utilisateur AND " +
           "v.tenant.tenantUuid = :tenantUuid AND " +
           "(:search IS NULL OR LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.client) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.telephoneClient) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR v.dateVente >= :dateDebut) AND " +
           "(:dateFin IS NULL OR v.dateVente <= :dateFin) " +
           "ORDER BY v.dateVente DESC, v.id DESC",
           countQuery = "SELECT COUNT(v) FROM VenteEntity v WHERE v.utilisateur = :utilisateur AND " +
           "v.tenant.tenantUuid = :tenantUuid AND " +
           "(:search IS NULL OR LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.client) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.telephoneClient) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR v.dateVente >= :dateDebut) AND " +
           "(:dateFin IS NULL OR v.dateVente <= :dateFin)")
    Page<VenteEntity> findByUtilisateurWithSearch(@Param("utilisateur") UserEntity utilisateur,
                                                  @Param("tenantUuid") String tenantUuid,
                                                  @Param("search") String search,
                                                  @Param("dateDebut") LocalDateTime dateDebut,
                                                  @Param("dateFin") LocalDateTime dateFin,
                                                  Pageable pageable);
}
