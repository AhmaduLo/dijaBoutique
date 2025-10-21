package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
                @Index(name = "idx_vente_client", columnList = "client")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")

public class VenteEntity  extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    @Column(name = "quantite", nullable = false)
    private Integer quantite;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit faire entre 2 et 100 caractères")
    @Column(name = "nom_produit", nullable = false, length = 100)
    private String nomProduit;

    @NotNull(message = "Le prix unitaire est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix unitaire doit être supérieur à 0")
    @Column(name = "prix_unitaire", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @NotNull(message = "Le prix total est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix total doit être supérieur à 0")
    @Column(name = "prix_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixTotal;

    @NotNull(message = "La date de vente est obligatoire")
    @PastOrPresent(message = "La date de vente ne peut pas être dans le futur")
    @Column(name = "date_vente", nullable = false)
    private LocalDate dateVente;

    @Size(max = 100, message = "Le nom du client ne peut dépasser 100 caractères")
    @Column(name = "client", length = 100)
    private String client;

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
    //@JsonIgnoreProperties({"achats", "ventes", "depenses", "motDePasse"})
    @ToString.Exclude
    @JsonIgnore
    private UserEntity utilisateur;



    /**
     * Calcule le prix total
     */
    public void calculerPrixTotal() {
        if (this.quantite != null && this.prixUnitaire != null) {
            this.prixTotal = this.prixUnitaire
                    .multiply(new BigDecimal(this.quantite))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Calcule le prix unitaire à partir du total
     */
    public void calculerPrixUnitaire() {
        if (this.quantite != null && this.quantite > 0 && this.prixTotal != null) {
            this.prixUnitaire = this.prixTotal
                    .divide(new BigDecimal(this.quantite), 2, RoundingMode.HALF_UP);
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

    @PrePersist
    protected void onCreate() {
        // Calcul automatique
        if (this.prixTotal == null) {
            calculerPrixTotal();
        }

        // Date par défaut
        if (this.dateVente == null) {
            this.dateVente = LocalDate.now();
        }

        // Nettoyer les champs
        if (this.nomProduit != null) {
            this.nomProduit = this.nomProduit.trim();
        }
        if (this.client != null) {
            this.client = this.client.trim();
        }
    }

    @PreUpdate
    protected void onUpdate() {
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
