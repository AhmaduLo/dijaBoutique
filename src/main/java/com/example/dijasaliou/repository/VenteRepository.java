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
public interface VenteRepository extends JpaRepository<VenteEntity, Long> {

    // Recherche par produit
    List<VenteEntity> findByNomProduit(String nomProduit);

    List<VenteEntity> findByNomProduitContaining(String keyword);

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
    Integer sumQuantiteByNomProduitAndTenant(@Param("nomProduit") String nomProduit, @Param("tenant") TenantEntity tenant);

    /**
     * Recherche paginée avec filtre optionnel sur nomProduit, client et plage de dates
     */
    @Query("SELECT v FROM VenteEntity v WHERE " +
           "(:search IS NULL OR LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.client) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR v.dateVente >= :dateDebut) AND " +
           "(:dateFin IS NULL OR v.dateVente <= :dateFin)")
    Page<VenteEntity> findAllWithSearch(@Param("search") String search,
                                        @Param("dateDebut") LocalDate dateDebut,
                                        @Param("dateFin") LocalDate dateFin,
                                        Pageable pageable);

    /**
     * Recherche paginée par utilisateur avec filtre optionnel et plage de dates
     */
    @Query("SELECT v FROM VenteEntity v WHERE v.utilisateur = :utilisateur AND " +
           "(:search IS NULL OR LOWER(v.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.client) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR v.dateVente >= :dateDebut) AND " +
           "(:dateFin IS NULL OR v.dateVente <= :dateFin)")
    Page<VenteEntity> findByUtilisateurWithSearch(@Param("utilisateur") UserEntity utilisateur,
                                                  @Param("search") String search,
                                                  @Param("dateDebut") LocalDate dateDebut,
                                                  @Param("dateFin") LocalDate dateFin,
                                                  Pageable pageable);
}
