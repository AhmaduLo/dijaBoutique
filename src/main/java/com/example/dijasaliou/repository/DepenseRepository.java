package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.DepenseEntity;
import com.example.dijasaliou.entity.UserEntity;
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
public interface DepenseRepository extends JpaRepository<DepenseEntity, Long> {

    // Recherche par utilisateur
    List<DepenseEntity> findByUtilisateur(UserEntity utilisateur);

    // Recherche par catégorie
    List<DepenseEntity> findByCategorie(DepenseEntity.CategorieDepense categorie);

    // Recherche par date
    List<DepenseEntity> findByDateDepense(LocalDate date);

    List<DepenseEntity> findByDateDepenseBetween(LocalDate debut, LocalDate fin);

    // Recherche par récurrence
    List<DepenseEntity> findByEstRecurrente(Boolean estRecurrente);

    // Combinaison
    List<DepenseEntity> findByCategorieAndEstRecurrente(DepenseEntity.CategorieDepense categorie, Boolean recurrente);

    // Recherche par montant
    List<DepenseEntity> findByMontantGreaterThan(BigDecimal montant);

    // Comptage
    long countByCategorie(DepenseEntity.CategorieDepense categorie);

    /**
     * Supprimer toutes les dépenses d'un utilisateur
     */
    void deleteByUtilisateur(UserEntity utilisateur);

    /**
     * Recherche paginée avec filtre optionnel sur libelle et catégorie
     */
    @Query("SELECT d FROM DepenseEntity d WHERE " +
           "(:search IS NULL OR LOWER(d.libelle) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categorie IS NULL OR d.categorie = :categorie)")
    Page<DepenseEntity> findAllWithSearch(@Param("search") String search,
                                          @Param("categorie") DepenseEntity.CategorieDepense categorie,
                                          Pageable pageable);
}
