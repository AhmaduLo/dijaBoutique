package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.BonLivraisonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BonLivraisonRepository extends JpaRepository<BonLivraisonEntity, Long> {

    List<BonLivraisonEntity> findAllByOrderByCreatedDateDesc();

    List<BonLivraisonEntity> findByStatutOrderByCreatedDateDesc(BonLivraisonEntity.Statut statut);

    @Query("SELECT COUNT(b) FROM BonLivraisonEntity b WHERE b.numeroBL LIKE :prefix%")
    long countByNumeroBLStartingWith(String prefix);
}
