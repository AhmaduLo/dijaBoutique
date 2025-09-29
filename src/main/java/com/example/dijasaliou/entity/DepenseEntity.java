package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@ToString(exclude = "utilisateur")
@EqualsAndHashCode(of = "id")
public class DepenceEntity {

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
    private UserEntity utilisateur;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

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

    

}
