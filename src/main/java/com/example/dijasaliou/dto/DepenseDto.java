package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.DepenseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour les dépenses
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
public class DepenseDto {
    private Long id;
    private String libelle;
    private BigDecimal montant;
    private LocalDate dateDepense;
    private DepenseEntity.CategorieDepense categorie;
    private String notes;
    private Boolean estRecurrente;

    // Informations sur l'utilisateur qui a créé la dépense
    private UserDto utilisateur;

    // Champs calculés (optionnels)
    private Boolean estRecente;
    private Integer mois;
    private Integer annee;
    private String libelleCategorie;
    private Boolean aDesNotes;

    /**
     * Convertit une entité Depense en DTO
     */
    public static DepenseDto fromEntity(DepenseEntity depense) {
        if (depense == null) {
            return null;
        }

        return DepenseDto.builder()
                .id(depense.getId())
                .libelle(depense.getLibelle())
                .montant(depense.getMontant())
                .dateDepense(depense.getDateDepense())
                .categorie(depense.getCategorie())
                .notes(depense.getNotes())
                .estRecurrente(depense.getEstRecurrente())
                .utilisateur(UserDto.fromEntityMinimal(depense.getUtilisateur()))
                .estRecente(depense.estRecente())
                .mois(depense.getMois())
                .annee(depense.getAnnee())
                .libelleCategorie(depense.getLibelleCategorie())
                .aDesNotes(depense.aDesNotes())
                .build();
    }

    /**
     * Version sans utilisateur (pour certains contextes)
     */
    public static DepenseDto fromEntityWithoutUser(DepenseEntity depense) {
        if (depense == null) {
            return null;
        }

        return DepenseDto.builder()
                .id(depense.getId())
                .libelle(depense.getLibelle())
                .montant(depense.getMontant())
                .dateDepense(depense.getDateDepense())
                .categorie(depense.getCategorie())
                .notes(depense.getNotes())
                .estRecurrente(depense.getEstRecurrente())
                .estRecente(depense.estRecente())
                .mois(depense.getMois())
                .annee(depense.getAnnee())
                .libelleCategorie(depense.getLibelleCategorie())
                .aDesNotes(depense.aDesNotes())
                .build();
    }
}
