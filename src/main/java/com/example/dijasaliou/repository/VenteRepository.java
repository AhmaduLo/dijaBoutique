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
import java.time.LocalDate;
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
    List<VenteEntity> findByDateVente(LocalDate date);

    List<VenteEntity> findByDateVenteBetween(LocalDate debut, LocalDate fin);

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
     * Calcule le chiffre d'affaires d'une période directement en SQL (évite le chargement en mémoire)
     */
    @Query("SELECT COALESCE(SUM(v.prixTotal), 0) FROM VenteEntity v " +
           "WHERE v.dateVente BETWEEN :debut AND :fin " +
           "AND v.tenant.tenantUuid = :tenantUuid")
    BigDecimal sumChiffreAffairesPeriode(@Param("debut") LocalDate debut,
                                         @Param("fin") LocalDate fin,
                                         @Param("tenantUuid") String tenantUuid);

    @Query("SELECT COUNT(v) FROM VenteEntity v WHERE v.tenant.tenantUuid = :tenantUuid")
    long countByTenantUuid(@Param("tenantUuid") String tenantUuid);

    @Query("SELECT COUNT(v) FROM VenteEntity v WHERE v.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Recherche paginée avec filtre optionnel sur nomProduit, client et plage de dates
     */
    @Query(value = "SELECT v FROM VenteEntity v WHERE " +
           "(:search IS NULL OR LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.client) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.telephoneClient) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR v.dateVente >= :dateDebut) AND " +
           "(:dateFin IS NULL OR v.dateVente <= :dateFin) " +
           "ORDER BY v.dateVente DESC, v.id DESC",
           countQuery = "SELECT COUNT(v) FROM VenteEntity v WHERE " +
           "(:search IS NULL OR LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.client) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.telephoneClient) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR v.dateVente >= :dateDebut) AND " +
           "(:dateFin IS NULL OR v.dateVente <= :dateFin)")
    Page<VenteEntity> findAllWithSearch(@Param("search") String search,
                                        @Param("dateDebut") LocalDate dateDebut,
                                        @Param("dateFin") LocalDate dateFin,
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
    List<Object[]> sumDirectVentesParModeEtPeriode(@Param("debut") LocalDate debut,
                                                   @Param("fin") LocalDate fin,
                                                   @Param("creditMode") VenteEntity.ModePaiementVente creditMode,
                                                   @Param("tenantUuid") String tenantUuid);

    @Query(value = "SELECT v FROM VenteEntity v WHERE v.utilisateur = :utilisateur AND " +
           "(:search IS NULL OR LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.client) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.telephoneClient) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR v.dateVente >= :dateDebut) AND " +
           "(:dateFin IS NULL OR v.dateVente <= :dateFin) " +
           "ORDER BY v.dateVente DESC, v.id DESC",
           countQuery = "SELECT COUNT(v) FROM VenteEntity v WHERE v.utilisateur = :utilisateur AND " +
           "(:search IS NULL OR LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.client) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.telephoneClient) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR v.dateVente >= :dateDebut) AND " +
           "(:dateFin IS NULL OR v.dateVente <= :dateFin)")
    Page<VenteEntity> findByUtilisateurWithSearch(@Param("utilisateur") UserEntity utilisateur,
                                                  @Param("search") String search,
                                                  @Param("dateDebut") LocalDate dateDebut,
                                                  @Param("dateFin") LocalDate dateFin,
                                                  Pageable pageable);
}
