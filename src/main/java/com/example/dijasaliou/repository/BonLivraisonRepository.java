package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.BonLivraisonEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BonLivraisonRepository extends JpaRepository<BonLivraisonEntity, String> {

    List<BonLivraisonEntity> findAllByOrderByCreatedDateDesc();

    List<BonLivraisonEntity> findByStatutOrderByCreatedDateDesc(BonLivraisonEntity.Statut statut);

    @Query("SELECT COUNT(b) FROM BonLivraisonEntity b WHERE b.numeroBL LIKE :prefix%")
    long countByNumeroBLStartingWith(String prefix);

    /**
     * Recherche paginée avec filtre optionnel sur clientNom, numeroBL, statut et plage de dates
     */
    @Query("SELECT b FROM BonLivraisonEntity b WHERE " +
           "(:statut IS NULL OR b.statut = :statut) AND " +
           "(:search IS NULL OR LOWER(b.clientNom) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.numeroBL) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.telephoneClient) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dateDebut IS NULL OR b.createdDate >= :dateDebut) AND " +
           "(:dateFin IS NULL OR b.createdDate <= :dateFin)")
    Page<BonLivraisonEntity> findAllWithSearch(@Param("statut") BonLivraisonEntity.Statut statut,
                                               @Param("search") String search,
                                               @Param("dateDebut") LocalDateTime dateDebut,
                                               @Param("dateFin") LocalDateTime dateFin,
                                               Pageable pageable);
}
