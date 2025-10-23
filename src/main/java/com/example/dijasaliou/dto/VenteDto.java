package com.example.dijasaliou.dto;

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

    // Informations sur l'utilisateur qui a créé la vente
    private UserDto utilisateur;

    // Champs calculés (optionnels)
    private Boolean estRecente;
    private Integer mois;
    private Integer annee;
    private Boolean aUnClient;

    /**
     * Convertit une entité Vente en DTO
     */
    public static VenteDto fromEntity(VenteEntity vente) {
        if (vente == null) {
            return null;
        }

        return VenteDto.builder()
                .id(vente.getId())
                .quantite(vente.getQuantite())
                .nomProduit(vente.getNomProduit())
                .prixUnitaire(vente.getPrixUnitaire())
                .prixTotal(vente.getPrixTotal())
                .dateVente(vente.getDateVente())
                .client(vente.getClient())
                .utilisateur(UserDto.fromEntityMinimal(vente.getUtilisateur()))
                .estRecente(vente.estRecente())
                .mois(vente.getMois())
                .annee(vente.getAnnee())
                .aUnClient(vente.aUnClient())
                .build();
    }

    /**
     * Version sans utilisateur (pour certains contextes)
     */
    public static VenteDto fromEntityWithoutUser(VenteEntity vente) {
        if (vente == null) {
            return null;
        }

        return VenteDto.builder()
                .id(vente.getId())
                .quantite(vente.getQuantite())
                .nomProduit(vente.getNomProduit())
                .prixUnitaire(vente.getPrixUnitaire())
                .prixTotal(vente.getPrixTotal())
                .dateVente(vente.getDateVente())
                .client(vente.getClient())
                .estRecente(vente.estRecente())
                .mois(vente.getMois())
                .annee(vente.getAnnee())
                .aUnClient(vente.aUnClient())
                .build();
    }
}
