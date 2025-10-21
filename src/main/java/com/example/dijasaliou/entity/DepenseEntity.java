package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(
        name = "depenses",
        indexes = {
                @Index(name = "idx_depense_date", columnList = "date_depense"),
                @Index(name = "idx_depense_categorie", columnList = "categorie"),
                @Index(name = "idx_depense_utilisateur", columnList = "utilisateur_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class DepenseEntity  extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(min = 3, max = 200, message = "Le libellé doit faire entre 3 et 200 caractères")
    @Column(name = "libelle", nullable = false, length = 200)
    private String libelle;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    @Column(name = "montant", nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @NotNull(message = "La date de dépense est obligatoire")
    @PastOrPresent(message = "La date de dépense ne peut pas être dans le futur")
    @Column(name = "date_depense", nullable = false)
    private LocalDate dateDepense;

    @NotNull(message = "La catégorie est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 50)
    private CategorieDepense categorie;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "est_recurrente")
    private Boolean estRecurrente = false;


    /**
     * Relation avec Utilisateur
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "utilisateur_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_depense_utilisateur")
    )
    @ToString.Exclude
    @JsonIgnoreProperties({"achats", "ventes", "depenses", "motDePasse"})
    private UserEntity utilisateur;


    /**
     * ENUM pour les catégories de dépenses
     *
     * ARCHITECTURE : Enum interne vs externe ?
     * - Interne (ici) : Couplage fort avec Depense
     * - Externe : Réutilisable ailleurs
     *
     * Pour ce projet : Interne suffit
     */
    public enum CategorieDepense {
        LOYER("Loyer", "Loyer du local commercial", true),
        ELECTRICITE("Électricité", "Factures d'électricité", true),
        EAU("Eau", "Factures d'eau", true),
        INTERNET("Internet/Téléphone", "Abonnements internet et téléphone", true),
        TRANSPORT("Transport", "Frais de transport et livraison", false),
        MARKETING("Marketing/Publicité", "Dépenses marketing et publicitaires", false),
        FOURNITURES("Fournitures", "Fournitures de bureau et emballages", false),
        MAINTENANCE("Maintenance/Réparation", "Maintenance et réparations", false),
        SALAIRES("Salaires", "Salaires et charges sociales", true),
        ASSURANCE("Assurance", "Assurances diverses", true),
        TAXES("Taxes/Impôts", "Taxes et impôts", false),
        FORMATION("Formation", "Formations et développement", false),
        EQUIPEMENT("Équipement", "Achat d'équipement", false),
        AUTRE("Autre", "Autres dépenses", false);

        private final String libelle;
        private final String description;
        private final boolean recurrenteParDefaut;

        CategorieDepense(String libelle, String description, boolean recurrenteParDefaut) {
            this.libelle = libelle;
            this.description = description;
            this.recurrenteParDefaut = recurrenteParDefaut;
        }

        public String getLibelle() {
            return libelle;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRecurrenteParDefaut() {
            return recurrenteParDefaut;
        }

        /**
         * Méthode utilitaire pour obtenir toutes les catégories
         * Utile pour les dropdowns dans Angular
         */
        public static List<CategorieDepense> toutes() {
            return Arrays.asList(values());
        }

        /**
         * Recherche une catégorie par son libellé
         */
        public static CategorieDepense parLibelle(String libelle) {
            return Arrays.stream(values())
                    .filter(c -> c.libelle.equalsIgnoreCase(libelle))
                    .findFirst()
                    .orElse(AUTRE);
        }

        /**
         * Obtenir les catégories récurrentes
         */
        public static List<CategorieDepense> recurrentes() {
            return Arrays.stream(values())
                    .filter(c -> c.recurrenteParDefaut)
                    .collect(Collectors.toList());
        }
    }

    /**
     * LIFECYCLE CALLBACKS
     */

    protected void onCreate() {
        // Date par défaut
        if (this.dateDepense == null) {
            this.dateDepense = LocalDate.now();
        }

        // Nettoyer les champs
        if (this.libelle != null) {
            this.libelle = this.libelle.trim();
        }

        // Définir récurrence selon la catégorie
        if (this.estRecurrente == null && this.categorie != null) {
            this.estRecurrente = this.categorie.isRecurrenteParDefaut();
        }
    }

    protected void onUpdate() {
        // Validation métier supplémentaire si nécessaire
    }

    /**
     * MÉTHODES MÉTIER
     */

    /**
     * Vérifie si la dépense est récente (< 30 jours)
     */
    public boolean estRecente() {
        return dateDepense != null &&
                dateDepense.isAfter(LocalDate.now().minusDays(30));
    }

    /**
     * Retourne le mois de la dépense
     */
    public int getMois() {
        return dateDepense != null ? dateDepense.getMonthValue() : 0;
    }

    /**
     * Retourne l'année de la dépense
     */
    public int getAnnee() {
        return dateDepense != null ? dateDepense.getYear() : 0;
    }

    /**
     * Vérifie si c'est une grosse dépense (> seuil)
     */
    public boolean estGrosseDépense(BigDecimal seuil) {
        return montant != null && montant.compareTo(seuil) > 0;
    }

    /**
     * Retourne le libellé de la catégorie
     */
    public String getLibelleCategorie() {
        return categorie != null ? categorie.getLibelle() : "";
    }

    /**
     * Vérifie si des notes sont présentes
     */
    public boolean aDesNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    /**
     * VALIDATION MÉTIER PERSONNALISÉE
     *
     * Cette méthode peut être appelée dans le Service
     * avant de sauvegarder
     */
    public void valider() throws IllegalArgumentException {
        // Vérifications métier
        if (montant != null && montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        if (dateDepense != null && dateDepense.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date ne peut pas être dans le futur");
        }

        // Plus de validations selon vos règles métier...
    }

}
