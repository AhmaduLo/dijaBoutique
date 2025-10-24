package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité représentant une devise (monnaie)
 *
 * Gère les différentes devises utilisées dans le système
 * avec leur taux de change par rapport à la devise de référence.
 *
 * Exemple :
 * - XOF (Franc CFA) : devise de référence (taux = 1.0)
 * - EUR (Euro) : taux = 655.957 (1 EUR = 655.957 XOF)
 * - USD (Dollar) : taux = 600.0 (1 USD = 600 XOF)
 */
@Entity
@Table(name = "devises", indexes = {
        @Index(name = "idx_code", columnList = "code"),
        @Index(name = "idx_default", columnList = "is_default")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class DeviseEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Code ISO de la devise (ex: USD, EUR, XOF)
     * Doit être unique
     */
    @Column(name = "code", unique = true, nullable = false, length = 10)
    private String code;

    /**
     * Nom complet de la devise (ex: "Dollar américain")
     */
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    /**
     * Symbole de la devise (ex: $, €, CFA)
     */
    @Column(name = "symbole", nullable = false, length = 10)
    private String symbole;

    /**
     * Pays principal d'utilisation
     */
    @Column(name = "pays", nullable = false, length = 100)
    private String pays;

    /**
     * Taux de change par rapport à la devise de référence
     *
     * Exemple : Si XOF est la devise de référence (taux = 1.0)
     * - EUR : taux = 655.957 → 1 EUR = 655.957 XOF
     * - USD : taux = 600.0 → 1 USD = 600 XOF
     *
     * Pour convertir :
     * - De devise X vers devise de référence : montant * tauxChange
     * - De devise de référence vers devise X : montant / tauxChange
     */
    @Column(name = "taux_change", nullable = false)
    @Builder.Default
    private Double tauxChange = 1.0;

    /**
     * Indique si c'est la devise par défaut du système
     *
     * RÈGLE : Une seule devise peut être par défaut à la fois
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Date de création de la devise
     */
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Valide les données de la devise
     */
    @PrePersist
    @PreUpdate
    protected void validate() {
        // Normaliser le code en majuscules
        if (code != null) {
            code = code.toUpperCase().trim();
        }

        // Vérifier que le taux de change est positif
        if (tauxChange != null && tauxChange <= 0) {
            throw new IllegalArgumentException("Le taux de change doit être supérieur à 0");
        }

        // Par défaut, isDefault est false si null
        if (isDefault == null) {
            isDefault = false;
        }
    }

    /**
     * Méthodes utilitaires pour les conversions
     */

    /**
     * Convertit un montant de cette devise vers la devise de référence
     */
    public Double convertirVersReference(Double montant) {
        if (montant == null) return 0.0;
        return montant * tauxChange;
    }

    /**
     * Convertit un montant de la devise de référence vers cette devise
     */
    public Double convertirDepuisReference(Double montant) {
        if (montant == null) return 0.0;
        if (tauxChange == 0) return 0.0;
        return montant / tauxChange;
    }

    /**
     * Retourne une représentation textuelle de la devise
     */
    public String getLibelleComplet() {
        return String.format("%s - %s (%s)", code, nom, symbole);
    }
}
