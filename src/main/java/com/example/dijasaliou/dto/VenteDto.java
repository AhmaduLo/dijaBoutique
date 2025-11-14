package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.VenteEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour les ventes
 *
 * Avantages :
 * - Pas de boucle de sérialisation avec l'utilisateur
 * - Contrôle total sur les données exposées
 * - Peut inclure des champs calculés (marge, taux de marge, etc.)
 * - Sépare la couche persistance de la couche API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenteDto {
    private Long id;
    private Integer quantite;
    private String nomProduit;
    private BigDecimal prixUnitaire;
    private BigDecimal prixTotal;
    private LocalDate dateVente;
    private String client;
    private String photoUrl;

    // Informations sur l'utilisateur qui a créé la vente
    private UserDto utilisateur;

    // Champs calculés (optionnels)
    private Boolean estRecente;
    private Integer mois;
    private Integer annee;
    private Boolean aUnClient;

    /**
     * Convertit une entité Vente en DTO
     *
     * RESTRICTION : photoUrl n'est retourné que pour le plan ENTERPRISE
     */
    public static VenteDto fromEntity(VenteEntity vente) {
        if (vente == null) {
            return null;
        }

        // Vérifier si le plan ENTERPRISE est actif pour afficher les photos
        boolean canViewPhotos = vente.getTenant() != null &&
                                vente.getTenant().getPlan() == TenantEntity.Plan.ENTREPRISE;

        return VenteDto.builder()
                .id(vente.getId())
                .quantite(vente.getQuantite())
                .nomProduit(vente.getNomProduit())
                .prixUnitaire(vente.getPrixUnitaire())
                .prixTotal(vente.getPrixTotal())
                .dateVente(vente.getDateVente())
                .client(vente.getClient())
                .photoUrl(canViewPhotos ? vente.getPhotoUrl() : null)
                .utilisateur(UserDto.fromEntityMinimal(vente.getUtilisateur()))
                .estRecente(vente.estRecente())
                .mois(vente.getMois())
                .annee(vente.getAnnee())
                .aUnClient(vente.aUnClient())
                .build();
    }

    /**
     * Version sans utilisateur (pour certains contextes)
     *
     * RESTRICTION : photoUrl n'est retourné que pour le plan ENTERPRISE
     */
    public static VenteDto fromEntityWithoutUser(VenteEntity vente) {
        if (vente == null) {
            return null;
        }

        // Vérifier si le plan ENTERPRISE est actif pour afficher les photos
        boolean canViewPhotos = vente.getTenant() != null &&
                                vente.getTenant().getPlan() == TenantEntity.Plan.ENTREPRISE;

        return VenteDto.builder()
                .id(vente.getId())
                .quantite(vente.getQuantite())
                .nomProduit(vente.getNomProduit())
                .prixUnitaire(vente.getPrixUnitaire())
                .prixTotal(vente.getPrixTotal())
                .dateVente(vente.getDateVente())
                .client(vente.getClient())
                .photoUrl(canViewPhotos ? vente.getPhotoUrl() : null)
                .estRecente(vente.estRecente())
                .mois(vente.getMois())
                .annee(vente.getAnnee())
                .aUnClient(vente.aUnClient())
                .build();
    }
}
