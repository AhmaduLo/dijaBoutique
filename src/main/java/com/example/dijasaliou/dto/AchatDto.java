package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.AchatEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour les achats
 *
 * Avantages :
 * - Pas de boucle de sérialisation avec l'utilisateur
 * - Contrôle total sur les données exposées
 * - Peut inclure des champs calculés
 * - Sépare la couche persistance de la couche API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchatDto {
    private Long id;
    private Integer quantite;
    private String nomProduit;
    private BigDecimal prixUnitaire;
    private BigDecimal prixTotal;
    private LocalDate dateAchat;
    private String fournisseur;
    private BigDecimal prixVenteSuggere;

    // Informations sur l'utilisateur qui a créé l'achat
    private UserDto utilisateur;

    // Champs calculés (optionnels)
    private Boolean estRecent;
    private Integer mois;
    private Integer annee;

    /**
     * Convertit une entité Achat en DTO
     */
    public static AchatDto fromEntity(AchatEntity achat) {
        if (achat == null) {
            return null;
        }

        return AchatDto.builder()
                .id(achat.getId())
                .quantite(achat.getQuantite())
                .nomProduit(achat.getNomProduit())
                .prixUnitaire(achat.getPrixUnitaire())
                .prixTotal(achat.getPrixTotal())
                .dateAchat(achat.getDateAchat())
                .fournisseur(achat.getFournisseur())
                .prixVenteSuggere(achat.getPrixVenteSuggere())
                .utilisateur(UserDto.fromEntityMinimal(achat.getUtilisateur()))
                .estRecent(achat.estRecent())
                .mois(achat.getMois())
                .annee(achat.getAnnee())
                .build();
    }

    /**
     * Version sans utilisateur (pour certains contextes)
     */
    public static AchatDto fromEntityWithoutUser(AchatEntity achat) {
        if (achat == null) {
            return null;
        }

        return AchatDto.builder()
                .id(achat.getId())
                .quantite(achat.getQuantite())
                .nomProduit(achat.getNomProduit())
                .prixUnitaire(achat.getPrixUnitaire())
                .prixTotal(achat.getPrixTotal())
                .dateAchat(achat.getDateAchat())
                .fournisseur(achat.getFournisseur())
                .prixVenteSuggere(achat.getPrixVenteSuggere())
                .estRecent(achat.estRecent())
                .mois(achat.getMois())
                .annee(achat.getAnnee())
                .build();
    }
}
