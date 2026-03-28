package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;


@Entity
@Table(
        name = "ventes",
        indexes = {
                @Index(name = "idx_vente_date", columnList = "date_vente"),
                @Index(name = "idx_vente_produit", columnList = "nom_produit"),
                @Index(name = "idx_vente_utilisateur", columnList = "utilisateur_id"),
                @Index(name = "idx_vente_client", columnList = "client"),
                @Index(name = "idx_vente_tenant", columnList = "tenant_id"),
                @Index(name = "idx_vente_mode_paiement", columnList = "mode_paiement")
        }
)
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = (SELECT t.id FROM tenants t WHERE t.tenant_uuid = :tenantId)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class VenteEntity  extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    @Column(name = "quantite", nullable = false)
    private Double quantite;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit faire entre 2 et 100 caractères")
    @Column(name = "nom_produit", nullable = false, length = 100)
    private String nomProduit;

    @NotNull(message = "Le prix unitaire est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix unitaire doit être supérieur à 0")
    @Column(name = "prix_unitaire", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(name = "prix_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixTotal;

    @NotNull(message = "La date de vente est obligatoire")
    @PastOrPresent(message = "La date de vente ne peut pas être dans le futur")
    @Column(name = "date_vente", nullable = false)
    private LocalDate dateVente;

    @Size(max = 100, message = "Le nom du client ne peut dépasser 100 caractères")
    @Column(name = "client", length = 100)
    private String client;

    @Size(max = 20, message = "Le téléphone client ne peut dépasser 20 caractères")
    @Column(name = "telephone_client", length = 20)
    private String telephoneClient;

    @Size(max = 255, message = "L'adresse client ne peut dépasser 255 caractères")
    @Column(name = "adresse_client", length = 255)
    private String adresseClient;

    /**
     * URL de la photo du produit (optionnel)
     * <p>
     * Format : /api/files/photos/{tenant_uuid}/ventes/2024-01-15_produit_abc123.jpg
     * <p>
     * La photo aide à :
     * - Identifier visuellement le produit vendu
     * - Vérifier que le bon produit a été vendu
     * - Améliorer l'expérience utilisateur
     * <p>
     * IMPORTANT : La photo est OPTIONNELLE
     * Le nom du produit reste OBLIGATOIRE pour la recherche et les exports
     */
    @Size(max = 500, message = "L'URL de la photo ne peut dépasser 500 caractères")
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Size(max = 20, message = "L'unité ne peut dépasser 20 caractères")
    @Column(name = "unite", length = 20)
    @Builder.Default
    private String unite = "pièce";


    /**
     * Relation avec Utilisateur
     * Qui a enregistré cette vente ?
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "utilisateur_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_vente_utilisateur")
    )
    @JsonBackReference("user-ventes")
    @ToString.Exclude
    private UserEntity utilisateur;

    /**
     * MULTI-TENANT : Référence au tenant (entreprise)
     * SÉCURITÉ CRITIQUE : Permet de filtrer automatiquement les ventes par entreprise
     * Chaque vente appartient à UNE SEULE entreprise
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vente_tenant"))
    @JsonIgnore
    private TenantEntity tenant;

    /**
     * Lien structuré vers le client enregistré (Plan Entreprise — crédit)
     * Nullable : les ventes classiques n'ont pas forcément un client enregistré
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "client_id", nullable = true, foreignKey = @ForeignKey(name = "fk_vente_client_ref"))
    @JsonIgnore
    private ClientEntity clientRef;

    /**
     * ID du client enregistré — champ transient pour la désérialisation JSON
     * Résolu en ClientEntity dans VenteService avant la création du crédit
     */
    @Transient
    private String clientId;

    /**
     * Date d'échéance pour les ventes à crédit — champ transient pour la désérialisation JSON
     */
    @Transient
    private LocalDate dateEcheance;

    public enum ModePaiementVente {
        ESPECES, WAVE, ORANGE_MONEY, CREDIT;

        @com.fasterxml.jackson.annotation.JsonCreator
        public static ModePaiementVente fromString(String value) {
            if (value == null) return ESPECES;
            return ModePaiementVente.valueOf(value.toUpperCase());
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 20)
    @Builder.Default
    private ModePaiementVente modePaiement = ModePaiementVente.ESPECES;

    @Column(name = "est_soldee", nullable = false)
    @Builder.Default
    private Boolean estSoldee = true;

    /**
     * Identifiant de groupe pour relier les produits d'une même vente multi-produits.
     * Généré côté frontend (UUID) — tous les articles d'un même panier partagent le même UUID.
     */
    @Size(max = 36, message = "Le groupeVenteId ne peut dépasser 36 caractères")
    @Column(name = "groupe_vente_id", length = 36)
    private String groupeVenteId;



    /**
     * Calcule le prix total
     */
    public void calculerPrixTotal() {
        if (this.quantite != null && this.prixUnitaire != null) {
            this.prixTotal = this.prixUnitaire
                    .multiply(BigDecimal.valueOf(this.quantite))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Calcule le prix unitaire à partir du total
     */
    public void calculerPrixUnitaire() {
        if (this.quantite != null && this.quantite > 0 && this.prixTotal != null) {
            this.prixUnitaire = this.prixTotal
                    .divide(BigDecimal.valueOf(this.quantite), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Calcule la marge brute si on a le prix d'achat
     *
     * @param prixAchatUnitaire Prix d'achat unitaire du produit
     * @return Marge en euros
     *
     * Exemple :
     * Prix achat : 10€
     * Prix vente : 15€
     * Marge : 5€
     */
    public BigDecimal calculerMargeBrute(BigDecimal prixAchatUnitaire) {
        if (prixAchatUnitaire == null || this.prixUnitaire == null) {
            return BigDecimal.ZERO;
        }
        return this.prixUnitaire.subtract(prixAchatUnitaire);
    }

    /**
     * Calcule le taux de marge en pourcentage
     *
     * @param prixAchatUnitaire Prix d'achat unitaire
     * @return Taux de marge en %
     *
     * Formule : ((Prix vente - Prix achat) / Prix achat) × 100
     *
     * Exemple :
     * Prix achat : 10€
     * Prix vente : 15€
     * Taux : ((15-10)/10) × 100 = 50%
     */
    public BigDecimal calculerTauxMarge(BigDecimal prixAchatUnitaire) {
        if (prixAchatUnitaire == null || prixAchatUnitaire.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal marge = calculerMargeBrute(prixAchatUnitaire);
        return marge
                .divide(prixAchatUnitaire, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule le chiffre d'affaires généré
     * (= prix total de la vente)
     */
    public BigDecimal getChiffreAffaires() {
        return this.prixTotal != null ? this.prixTotal : BigDecimal.ZERO;
    }

    /**
     * LIFECYCLE CALLBACKS
     */

    @Override
    protected void beforePersist() {
        if (this.prixTotal == null) {
            calculerPrixTotal();
        }
        if (this.dateVente == null) {
            this.dateVente = LocalDate.now();
        }
        if (this.nomProduit != null) {
            this.nomProduit = this.nomProduit.trim();
        }
        if (this.client != null) {
            this.client = this.client.trim();
        }
    }

    @Override
    protected void beforeUpdate() {
        calculerPrixTotal();
    }

    /**
     * MÉTHODES UTILITAIRES
     */

    public boolean estRecente() {
        return dateVente != null &&
                dateVente.isAfter(LocalDate.now().minusDays(30));
    }

    public int getMois() {
        return dateVente != null ? dateVente.getMonthValue() : 0;
    }

    public int getAnnee() {
        return dateVente != null ? dateVente.getYear() : 0;
    }

    /**
     * Vérifie si la vente a un client renseigné
     */
    public boolean aUnClient() {
        return client != null && !client.trim().isEmpty();
    }
}
