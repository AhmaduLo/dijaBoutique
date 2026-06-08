package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AchatRepository extends JpaRepository<AchatEntity, String> {

    /**Trouver tous les achats d'un produit (filtre tenant via Hibernate filter) */
    List<AchatEntity> findByNomProduit(String nomProduit);

    /**
     * Trouver les achats contenant un mot dans le nom (filtre tenant via Hibernate filter)
     */
    List<AchatEntity> findByNomProduitContaining(String keyword);

    /**
     * Trouver les achats d'un produit pour un tenant donné — filtre tenant EXPLICITE.
     * Utiliser à la place de findByNomProduit() dans les contextes sans filtre Hibernate (tâches planifiées, etc.)
     */
    @Query("SELECT a FROM AchatEntity a WHERE a.nomProduit = :nomProduit AND a.tenant = :tenant")
    List<AchatEntity> findByNomProduitAndTenant(@Param("nomProduit") String nomProduit, @Param("tenant") TenantEntity tenant);

    /**
     * Trouver les achats contenant un mot, pour un tenant donné — filtre tenant EXPLICITE.
     */
    @Query("SELECT a FROM AchatEntity a WHERE LOWER(a.nomProduit) LIKE LOWER(CONCAT('%', :keyword, '%')) AND a.tenant = :tenant")
    List<AchatEntity> findByNomProduitContainingAndTenant(@Param("keyword") String keyword, @Param("tenant") TenantEntity tenant);

    /**
     * Trouver tous les achats d'un utilisateur
     */
    List<AchatEntity> findByUtilisateur(UserEntity utilisateur);

    /**
     * Trouver les achats d'une date précise
     */
    List<AchatEntity> findByDateAchat(LocalDateTime date);

    /**
     * Trouver les achats entre deux dates
     */
    @Query("SELECT a FROM AchatEntity a JOIN FETCH a.utilisateur JOIN FETCH a.tenant WHERE a.dateAchat BETWEEN :debut AND :fin ORDER BY a.dateAchat DESC, a.id DESC")
    List<AchatEntity> findByDateAchatBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /**
     * Compter les achats d'un produit
     */
    long countByNomProduit(String nomProduit);

    /**
     * Supprimer tous les achats d'un utilisateur
     */
    void deleteByUtilisateur(UserEntity utilisateur);

    /**
     * Calculer le total des quantités achetées pour un produit et un tenant
     * Utilisé pour le calcul de stock
     */
    @Query("SELECT SUM(a.quantite) FROM AchatEntity a WHERE a.nomProduit = :nomProduit AND a.tenant = :tenant")
    Double sumQuantiteByNomProduitAndTenant(@Param("nomProduit") String nomProduit, @Param("tenant") TenantEntity tenant);

    /**
     * Récupère tous les achats d'un tenant (filtre explicite — évite findAll())
     */
    @Query("SELECT a FROM AchatEntity a WHERE a.tenant = :tenant ORDER BY a.nomProduit")
    List<AchatEntity> findAllByTenant(@Param("tenant") TenantEntity tenant);

    /**
     * Trouver les achats par code-barre pour un tenant donné
     */
    @Query("SELECT a FROM AchatEntity a WHERE a.codeBarre = :codeBarre AND a.tenant = :tenant ORDER BY a.dateAchat DESC, a.id DESC")
    List<AchatEntity> findByCodeBarreAndTenant(@Param("codeBarre") String codeBarre, @Param("tenant") TenantEntity tenant);

    /**
     * Recherche paginée avec filtre tenant EXPLICITE — permet l'utilisation de idx_achat_tenant_date.
     */
    @Query(value = "SELECT a FROM AchatEntity a JOIN FETCH a.utilisateur JOIN FETCH a.tenant WHERE " +
           "a.tenant.tenantUuid = :tenantUuid AND " +
           "(:search IS NULL OR LOWER(a.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.fournisseur) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR a.dateAchat >= :dateDebut) AND " +
           "(:dateFin IS NULL OR a.dateAchat <= :dateFin) " +
           "ORDER BY a.dateAchat DESC, a.id DESC",
           countQuery = "SELECT COUNT(a) FROM AchatEntity a WHERE " +
           "a.tenant.tenantUuid = :tenantUuid AND " +
           "(:search IS NULL OR LOWER(a.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.fournisseur) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR a.dateAchat >= :dateDebut) AND " +
           "(:dateFin IS NULL OR a.dateAchat <= :dateFin)")
    Page<AchatEntity> findAllWithSearch(@Param("tenantUuid") String tenantUuid,
                                        @Param("search") String search,
                                        @Param("dateDebut") LocalDateTime dateDebut,
                                        @Param("dateFin") LocalDateTime dateFin,
                                        Pageable pageable);

    /**
     * FIFO : lots d'achat disponibles pour un produit (quantité restante > 0),
     * triés par date d'achat croissante (le plus ancien d'abord).
     * Filtre tenant EXPLICITE pour sécurité multi-tenant.
     */
    @Query("""
            SELECT a FROM AchatEntity a
            WHERE a.tenant = :tenant
              AND a.nomProduit = :nomProduit
              AND a.quantiteRestante IS NOT NULL
              AND a.quantiteRestante > 0
            ORDER BY a.dateAchat ASC, a.id ASC
            """)
    List<AchatEntity> findLotsDisponiblesFifo(@Param("nomProduit") String nomProduit,
                                              @Param("tenant") TenantEntity tenant);

    /**
     * Tous les achats d'un tenant triés par date d'achat ASC (pour le backfill FIFO).
     */
    @Query("SELECT a FROM AchatEntity a WHERE a.tenant = :tenant ORDER BY a.dateAchat ASC, a.id ASC")
    List<AchatEntity> findAllByTenantOrderByDateAsc(@Param("tenant") TenantEntity tenant);

    /**
     * Somme des achats d'un tenant pour un mode de paiement, entre deux dates.
     * Utilisé par le module Caisse pour calculer les sorties par compte
     * (avec borne supérieure pour les snapshots).
     */
    @Query("""
            SELECT COALESCE(SUM(a.prixTotal), 0)
            FROM AchatEntity a
            WHERE a.tenant = :tenant
              AND a.modePaiement = :modePaiement
              AND a.dateAchat >= :debut
              AND a.dateAchat <= :fin
            """)
    java.math.BigDecimal sumByModePaiementBetween(
            @Param("tenant") TenantEntity tenant,
            @Param("modePaiement") com.example.dijasaliou.entity.ModePaiementCaisse modePaiement,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);
}