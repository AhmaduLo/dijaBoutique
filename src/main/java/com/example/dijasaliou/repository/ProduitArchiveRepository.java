package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.ProduitArchiveEntity;
import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProduitArchiveRepository extends JpaRepository<ProduitArchiveEntity, Long> {

    List<ProduitArchiveEntity> findByTenant(TenantEntity tenant);

    Optional<ProduitArchiveEntity> findByTenantAndNomProduit(TenantEntity tenant, String nomProduit);

    boolean existsByTenantAndNomProduit(TenantEntity tenant, String nomProduit);

    @Modifying
    void deleteByTenantAndNomProduit(TenantEntity tenant, String nomProduit);

    /**
     * Retourne les noms de produits archivés pour un tenant (pour filtrage rapide)
     */
    default Set<String> findNomsArchivesParTenant(TenantEntity tenant) {
        List<ProduitArchiveEntity> archives = findByTenant(tenant);
        Set<String> noms = new java.util.HashSet<>();
        for (ProduitArchiveEntity a : archives) {
            // Seulement les produits réellement archivés (étape 2 : dateArchivage renseignée)
            if (a.getDateArchivage() != null) {
                noms.add(a.getNomProduit().toLowerCase().trim());
            }
        }
        return noms;
    }
}
