package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name = "achats", indexes = {
    @Index(name = "idx_achat_date", columnList = "date_achat"),
    @Index(name = "idx_achat_produit", columnList = "nom_produit"),
    @Index(name = "idx_achat_utilisateur", columnList = "utilisateur_id"),
    @Index(name = "idx_achat_tenant", columnList = "tenant_id")
})
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = String.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = (SELECT t.id FROM tenants t WHERE t.tenant_uuid = :tenantId)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class AchatEntity extends BaseEntity{

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

    @NotNull(message = "La date d'achat est obligatoire")
    @PastOrPresent(message = "La date d'achat ne peut pas être dans le futur")
    @Column(name = "date_achat", nullable = false)
    private LocalDate dateAchat;

    @Size(max = 100, message = "Le nom du fournisseur ne peut dépasser 100 caractères")
    @Column(name = "fournisseur", length = 100)
    private String fournisseur;

    /**
     * Relation Many-to-One avec Utilisateur
     * <p>
     * PLUSIEURS achats → UN utilisateur
     *
     * @ManyToOne : Type de relation
     * fetch = LAZY : Chargement différé (performance)
     * optional = false : Utilisateur OBLIGATOIRE (NOT NULL en SQL)
     * @JoinColumn : Colonne de jointure
     * name = "utilisateur_id" : Nom de la colonne FK
     * nullable = false : Renforce le NOT NULL
     * foreignKey : Nomme la contrainte FK en SQL
     * <p>
     * LAZY vs EAGER :
     * - LAZY : Utilisateur chargé seulement si on y accède
     * - EAGER : Utilisateur TOUJOURS chargé (éviter !)
     */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, foreignKey = @ForeignKey(name = "fk_achat_utilisateur"))
    @JsonBackReference("user-achats")
    @ToString.Exclude
    private UserEntity utilisateur;

    /**
     * MULTI-TENANT : Référence au tenant (entreprise)
     * SÉCURITÉ CRITIQUE : Permet de filtrer automatiquement les achats par entreprise
     * Chaque achat appartient à UNE SEULE entreprise
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_achat_tenant"))
    @JsonIgnore
    private TenantEntity tenant;


    /**
     * Calcule le prix total : quantité × prix unitaire
     * <p>
     * BigDecimal.multiply() : Multiplication précise
     * setScale() : Arrondit à 2 décimales
     * RoundingMode.HALF_UP : Arrondi classique (0.5 → 1)
     * <p>
     * Exemple :
     * quantité = 10
     * prixUnitaire = 15.50
     * prixTotal = 155.00
     */
    public void calculerPrixTotal() {
        if (this.quantite != null && this.prixUnitaire != null) {
            this.prixTotal = this.prixUnitaire.multiply(new BigDecimal(this.quantite)).setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Calcule le prix unitaire à partir du total
     * Utile si on entre d'abord le total
     */
    public void calculerPrixUnitaire() {
        if (this.quantite != null && this.quantite > 0 && this.prixTotal != null) {
            this.prixUnitaire = this.prixTotal.divide(new BigDecimal(this.quantite), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * LIFECYCLE CALLBACKS JPA
     * Ces méthodes sont appelées automatiquement par JPA
     * lors de certains événements
     */

    /**
     * @PrePersist : Appelé AVANT l'INSERT en base
     * <p>
     * Utilité : Validation, calculs, valeurs par défaut
     */

    protected void onCreate() {
        // Calcul automatique du prix total si non défini
        if (this.prixTotal == null) {
            calculerPrixTotal();
        }

        // Date d'achat par défaut = aujourd'hui
        if (this.dateAchat == null) {
            this.dateAchat = LocalDate.now();
        }

        // Nettoyer le nom du produit
        if (this.nomProduit != null) {
            this.nomProduit = this.nomProduit.trim();
        }
    }

    /**
     * @PreUpdate : Appelé AVANT l'UPDATE en base
     */

    protected void onUpdate() {
        // Recalculer le prix total si quantité ou prix unitaire changé
        calculerPrixTotal();
    }

    /**
     * @PostLoad : Appelé APRÈS avoir chargé l'entité depuis la base
     * Utile pour des calculs ou transformations
     */
    @PostLoad
    protected void onLoad() {
        // Exemple : Logger l'accès à l'entité (audit)
    }

    /**
     * MÉTHODES UTILITAIRES
     */

    /**
     * Vérifie si l'achat est récent (< 30 jours)
     */
    public boolean estRecent() {
        return dateAchat != null && dateAchat.isAfter(LocalDate.now().minusDays(30));
    }

    /**
     * Retourne le mois de l'achat (pour les bilans mensuels)
     */
    public int getMois() {
        return dateAchat != null ? dateAchat.getMonthValue() : 0;
    }

    /**
     * Retourne l'année de l'achat
     */
    public int getAnnee() {
        return dateAchat != null ? dateAchat.getYear() : 0;
    }
}
