package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.DeviseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour la gestion des devises
 */
@Repository
public interface DeviseRepository extends JpaRepository<DeviseEntity, Long> {

    /**
     * Vérifie si une devise avec ce code existe déjà
     *
     * @param code Code de la devise (ex: USD, EUR)
     * @return true si le code existe
     */
    boolean existsByCode(String code);

    /**
     * Trouve une devise par son code
     *
     * @param code Code de la devise
     * @return Optional contenant la devise si trouvée
     */
    Optional<DeviseEntity> findByCode(String code);

    /**
     * Trouve la devise par défaut
     *
     * @return Optional contenant la devise par défaut si elle existe
     */
    Optional<DeviseEntity> findByIsDefaultTrue();

    /**
     * Vérifie si une devise est la devise par défaut
     *
     * @return true si une devise par défaut existe
     */
    boolean existsByIsDefaultTrue();
}
