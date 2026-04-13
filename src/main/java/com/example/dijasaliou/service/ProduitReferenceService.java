package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.ProduitReferenceEntity;
import com.example.dijasaliou.repository.ProduitReferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProduitReferenceService {

    private final ProduitReferenceRepository produitReferenceRepository;

    /**
     * Cherche un produit par code-barre dans la base partagée.
     */
    @Transactional(readOnly = true)
    public Optional<ProduitReferenceEntity> rechercherParCodeBarre(String codeBarre) {
        return produitReferenceRepository.findByCodeBarre(codeBarre);
    }

    /**
     * Contribue un produit à la base partagée.
     * Appelé automatiquement quand un commerçant renseigne un nouveau produit avec code-barre.
     * Si le code-barre existe déjà, incrémente le compteur d'utilisations.
     */
    @Transactional
    public void contribuer(String codeBarre, String nomProduit, String photoUrl, String categorie, String tenantNom) {
        if (codeBarre == null || codeBarre.isBlank() || nomProduit == null || nomProduit.isBlank()) {
            return;
        }

        Optional<ProduitReferenceEntity> existant = produitReferenceRepository.findByCodeBarre(codeBarre.trim());

        if (existant.isPresent()) {
            // Produit déjà connu — incrémenter le compteur
            ProduitReferenceEntity ref = existant.get();
            ref.setNbUtilisations(ref.getNbUtilisations() + 1);
            // Mettre à jour la photo si elle était absente
            if ((ref.getPhotoUrl() == null || ref.getPhotoUrl().isBlank()) && photoUrl != null && !photoUrl.isBlank()) {
                ref.setPhotoUrl(photoUrl);
            }
            // Mettre à jour la catégorie si elle était absente
            if ((ref.getCategorie() == null || ref.getCategorie().isBlank()) && categorie != null && !categorie.isBlank()) {
                ref.setCategorie(categorie.trim());
            }
            produitReferenceRepository.save(ref);
        } else {
            // Nouveau produit — créer l'entrée partagée
            ProduitReferenceEntity ref = ProduitReferenceEntity.builder()
                    .codeBarre(codeBarre.trim())
                    .nomProduit(nomProduit.trim())
                    .photoUrl(photoUrl)
                    .categorie(categorie != null && !categorie.isBlank() ? categorie.trim() : null)
                    .contribueParTenantNom(tenantNom)
                    .build();
            produitReferenceRepository.save(ref);
            log.info("Nouveau produit référencé : {} ({}) catégorie: {}", nomProduit, codeBarre, categorie);
        }
    }

    /**
     * Liste paginée avec recherche — pour le super admin.
     */
    @Transactional(readOnly = true)
    public Page<ProduitReferenceEntity> lister(int page, int size, String search) {
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        return produitReferenceRepository.findAllWithSearch(searchParam, PageRequest.of(page, size));
    }

    /**
     * Stats pour le dashboard super admin.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenirStats() {
        long total = produitReferenceRepository.count();
        long avecPhoto = produitReferenceRepository.countByPhotoUrlIsNotNull();
        long sansCategorie = produitReferenceRepository.countByCategorieIsNull();
        long ajoutesCetteSemaine = produitReferenceRepository.countByDateCreationAfter(
                LocalDateTime.now().minusDays(7));

        return Map.of(
                "totalProduits", total,
                "produitsAvecPhoto", avecPhoto,
                "produitsSansCategorie", sansCategorie,
                "ajoutesCetteSemaine", ajoutesCetteSemaine
        );
    }

    /**
     * Modifier un produit — super admin uniquement.
     */
    @Transactional
    public ProduitReferenceEntity modifier(Long id, String nomProduit, String photoUrl, String categorie) {
        ProduitReferenceEntity ref = produitReferenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit référencé non trouvé avec l'ID : " + id));

        if (nomProduit != null && !nomProduit.isBlank()) {
            ref.setNomProduit(nomProduit.trim());
        }
        if (photoUrl != null) {
            ref.setPhotoUrl(photoUrl.isBlank() ? null : photoUrl);
        }
        if (categorie != null) {
            ref.setCategorie(categorie.isBlank() ? null : categorie);
        }

        return produitReferenceRepository.save(ref);
    }

    /**
     * Supprimer un produit — super admin uniquement.
     */
    @Transactional
    public void supprimer(Long id) {
        if (!produitReferenceRepository.existsById(id)) {
            throw new RuntimeException("Produit référencé non trouvé avec l'ID : " + id);
        }
        produitReferenceRepository.deleteById(id);
    }
}
