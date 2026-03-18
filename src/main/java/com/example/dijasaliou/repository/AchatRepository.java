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

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AchatRepository extends JpaRepository<AchatEntity, Long> {

    /**Trouver tous les achats d'un produit*/
    List<AchatEntity> findByNomProduit(String nomProduit);

    /**
     * Trouver les achats contenant un mot dans le nom
     */
    List<AchatEntity> findByNomProduitContaining(String keyword);

    /**
     * Trouver tous les achats d'un utilisateur
     */
    List<AchatEntity> findByUtilisateur(UserEntity utilisateur);

    /**
     * Trouver les achats d'une date précise
     */
    List<AchatEntity> findByDateAchat(LocalDate date);

    /**
     * Trouver les achats entre deux dates
     */
    List<AchatEntity> findByDateAchatBetween(LocalDate debut, LocalDate fin);

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
    Integer sumQuantiteByNomProduitAndTenant(@Param("nomProduit") String nomProduit, @Param("tenant") TenantEntity tenant);

    /**
     * Recherche paginée avec filtre optionnel sur nomProduit, fournisseur et plage de dates
     */
    @Query("SELECT a FROM AchatEntity a WHERE " +
           "(:search IS NULL OR LOWER(a.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.fournisseur) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR a.dateAchat >= :dateDebut) AND " +
           "(:dateFin IS NULL OR a.dateAchat <= :dateFin)")
    Page<AchatEntity> findAllWithSearch(@Param("search") String search,
                                        @Param("dateDebut") LocalDate dateDebut,
                                        @Param("dateFin") LocalDate dateFin,
                                        Pageable pageable);
}