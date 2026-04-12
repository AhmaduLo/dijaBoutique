package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.CategorieReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorieReferenceRepository extends JpaRepository<CategorieReferenceEntity, Long> {

    List<CategorieReferenceEntity> findAllByOrderByOrdreAsc();

    boolean existsByNom(String nom);
}
