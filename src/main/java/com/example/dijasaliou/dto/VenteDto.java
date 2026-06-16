package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.VenteEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String id;
    private Double quantite;
    private String nomProduit;
    private BigDecimal prixUnitaire;
    private BigDecimal prixTotal;
    private LocalDateTime dateVente;
    private String client;
    private String telephoneClient;
    private String adresseClient;
    private String photoUrl;
    private String unite;

    // Groupe de vente multi-produits (UUID partagé entre les articles du même panier)
    private String groupeVenteId;

    // Crédit client
    private String clientId;
    private String modePaiement;
    private Boolean estSoldee;

    // Informations sur l'utilisateur qui a créé la vente
    private UserDto utilisateur;

    // Champs calculés (optionnels)
    private Boolean estRecente;
    private Integer mois;
    private Integer annee;
    private Boolean aUnClient;

    // Sortie hors vente (perte, vol, casse, don, crédit impayé)
    // null quand c'est une vente commerciale classique
    private String typeSortie;
    private String motifSortie;

    /**
     * Statut du crédit associé (null si pas de crédit ou crédit dénormalisé).
     * Valeurs possibles : EN_ATTENTE, PARTIEL, SOLDE, PERTE.
     * Sert au frontend pour afficher "Crédit perdu" plutôt que "CREDIT" sur les ventes
     * dont le crédit a été passé en perte.
     */
    private String creditStatut;

    /**
     * Convertit une entité Vente en DTO
     *
     * RESTRICTION : photoUrl n'est retourné que pour le plan ENTERPRISE
     */
    public static VenteDto fromEntity(VenteEntity vente) {
        if (vente == null) {
            return null;
        }

        // Vérifier si le plan PREMIUM ou ENTREPRISE est actif pour afficher les photos
        boolean canViewPhotos = vente.getTenant() != null &&
                                (vente.getTenant().getPlan() == TenantEntity.Plan.PRO ||
                                 vente.getTenant().getPlan() == TenantEntity.Plan.BUSINESS);

        return VenteDto.builder()
                .id(vente.getId())
                .quantite(vente.getQuantite())
                .nomProduit(vente.getNomProduit())
                .prixUnitaire(vente.getPrixUnitaire())
                .prixTotal(vente.getPrixTotal())
                .dateVente(vente.getDateVente())
                .client(vente.getClient())
                .telephoneClient(vente.getTelephoneClient())
                .adresseClient(vente.getAdresseClient())
                .photoUrl(canViewPhotos ? vente.getPhotoUrl() : null)
                .unite(vente.getUnite())
                .groupeVenteId(vente.getGroupeVenteId())
                .clientId(vente.getClientRef() != null ? vente.getClientRef().getId() : null)
                .modePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : "ESPECES")
                .estSoldee(vente.getEstSoldee())
                .utilisateur(UserDto.fromEntityMinimal(vente.getUtilisateur()))
                .estRecente(vente.estRecente())
                .mois(vente.getMois())
                .annee(vente.getAnnee())
                .aUnClient(vente.aUnClient())
                .typeSortie(vente.getTypeSortie() != null ? vente.getTypeSortie().name() : null)
                .motifSortie(vente.getMotifSortie())
                .build();
    }

    /**
     * Construit un VenteDto "virtuel" à partir d'un crédit passé en perte.
     * Utilisé par la modale Pertes pour afficher les créances irrécouvrables
     * à côté des vraies sorties hors vente, avec la même structure.
     *
     * - typeSortie = CREDIT_IMPAYE
     * - motifSortie = nom client + montant restant non payé
     * - dateVente = datePassageEnPerte (pas la date de la vente originale)
     * - id = id du crédit
     * - quantite/produit : repris de la vente originale pour traçabilité
     */
    public static VenteDto fromCreditPerdu(com.example.dijasaliou.entity.CreditClientEntity credit) {
        if (credit == null) return null;
        VenteEntity vente = credit.getVente();
        String nomClient = credit.getClient() != null ? credit.getClient().getNom() : "Client inconnu";
        java.math.BigDecimal restantDu = credit.getMontantRestant() != null ? credit.getMontantRestant() : java.math.BigDecimal.ZERO;

        return VenteDto.builder()
                .id(credit.getId())
                .quantite(vente != null ? vente.getQuantite() : 0.0)
                .nomProduit(vente != null ? vente.getNomProduit() : "—")
                .prixUnitaire(java.math.BigDecimal.ZERO)
                .prixTotal(java.math.BigDecimal.ZERO)
                .dateVente(credit.getDatePassageEnPerte() != null
                        ? credit.getDatePassageEnPerte().atStartOfDay()
                        : null)
                .client(nomClient)
                .unite(vente != null ? vente.getUnite() : "pièce")
                .typeSortie("CREDIT_IMPAYE")
                .motifSortie(String.format("%s — reste dû %s CFA",
                        nomClient,
                        restantDu.setScale(0, java.math.RoundingMode.HALF_UP)))
                .creditStatut("PERTE")
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

        // Vérifier si le plan PREMIUM ou ENTREPRISE est actif pour afficher les photos
        boolean canViewPhotos = vente.getTenant() != null &&
                                (vente.getTenant().getPlan() == TenantEntity.Plan.PRO ||
                                 vente.getTenant().getPlan() == TenantEntity.Plan.BUSINESS);

        return VenteDto.builder()
                .id(vente.getId())
                .quantite(vente.getQuantite())
                .nomProduit(vente.getNomProduit())
                .prixUnitaire(vente.getPrixUnitaire())
                .prixTotal(vente.getPrixTotal())
                .dateVente(vente.getDateVente())
                .client(vente.getClient())
                .telephoneClient(vente.getTelephoneClient())
                .adresseClient(vente.getAdresseClient())
                .photoUrl(canViewPhotos ? vente.getPhotoUrl() : null)
                .unite(vente.getUnite())
                .groupeVenteId(vente.getGroupeVenteId())
                .estRecente(vente.estRecente())
                .mois(vente.getMois())
                .annee(vente.getAnnee())
                .aUnClient(vente.aUnClient())
                .typeSortie(vente.getTypeSortie() != null ? vente.getTypeSortie().name() : null)
                .motifSortie(vente.getMotifSortie())
                .build();
    }
}
