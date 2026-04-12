package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.ProduitReferenceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ProduitReferenceRepository extends JpaRepository<ProduitReferenceEntity, Long> {

    Optional<ProduitReferenceEntity> findByCodeBarre(String codeBarre);

    boolean existsByCodeBarre(String codeBarre);

    @Query("SELECT p FROM ProduitReferenceEntity p WHERE " +
           "(:search IS NULL OR LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR p.codeBarre LIKE CONCAT('%', :search, '%')) " +
           "ORDER BY p.nbUtilisations DESC, p.dateCreation DESC")
    Page<ProduitReferenceEntity> findAllWithSearch(@Param("search") String search, Pageable pageable);

    long countByPhotoUrlIsNotNull();

    long countByCategorieIsNull();

    long countByDateCreationAfter(LocalDateTime date);
}
