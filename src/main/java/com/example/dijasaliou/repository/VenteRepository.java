package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
