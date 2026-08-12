package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.ProduitFabriqueEntity;
import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitFabriqueRepository extends JpaRepository<ProduitFabriqueEntity, Long> {

    List<ProduitFabriqueEntity> findAllByTenantAndActifTrueOrderByNomProduitAsc(TenantEntity tenant);

    Optional<ProduitFabriqueEntity> findByTenantAndNomProduitIgnoreCase(TenantEntity tenant, String nomProduit);
}
