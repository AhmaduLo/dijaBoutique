package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité Tenant (Entreprise)
 *
 * PRINCIPE MULTI-TENANT :
 * - Chaque entreprise qui s'inscrit = 1 TENANT
 * - Toutes les données sont isolées par tenant_id
 * - Garantit que l'Entreprise A ne voit JAMAIS les données de l'Entreprise B
 *
 * SÉCURITÉ :
 * - L'ID du tenant est un UUID (impossible à deviner)
 * - Chaque requête est filtrée automatiquement par tenant_id
 * - Double vérification : JPA Filter + Contrôleurs
 */
@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenant_uuid", columnList = "tenant_uuid", unique = true),
    @Index(name = "idx_tenant_actif", columnList = "actif")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"utilisateurs"})
@EqualsAndHashCode(exclude = {"utilisateurs"})
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UUID unique pour identifier le tenant
     * Utilisé dans les JWT pour l'isolation des données
     *
     * Pourquoi UUID et pas un Long ?
     * - Impossible à deviner (sécurité)
     * - Universel et unique
     */
    @Column(name = "tenant_uuid", nullable = false, unique = true, length = 36, updatable = false)
    private String tenantUuid;

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit faire entre 2 et 100 caractères")
    @Column(name = "nom_entreprise", nullable = false, length = 100)
    private String nomEntreprise;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Column(name = "numero_telephone", nullable = false, length = 20)
    private String numeroTelephone;

    @Column(name = "adresse", length = 255)
    private String adresse;

    @Column(name = "ville", length = 100)
    private String ville;

    @Column(name = "pays", length = 100)
    private String pays;

    /**
     * Numéro NINEA (Sénégal) ou SIRET (France) ou autre identifiant fiscal
     * Ce champ est OPTIONNEL car toutes les entreprises n'ont pas forcément ce numéro
     * lors de leur inscription (en cours de création, micro-entreprise, etc.)
     */
    @Column(name = "ninea_siret", length = 50)
    private String nineaSiret;

    /**
     * Permet de désactiver un tenant (soft delete)
     * Si actif = false, l'entreprise ne peut plus se connecter
     */
    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_expiration")
    private LocalDateTime dateExpiration;

    /**
     * Plan de l'entreprise (pour future facturation SaaS)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    @Builder.Default
    private Plan plan = Plan.GRATUIT;

    /**
     * Relation One-to-Many avec UserEntity
     * Un tenant (entreprise) peut avoir plusieurs utilisateurs
     */
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<UserEntity> utilisateurs = new ArrayList<>();

    /**
     * Enum pour les plans de paiement
     *
     * PLAN GRATUIT : État initial après inscription - PAIEMENT REQUIS pour accéder
     * PLAN BASIC : 6555 CFA (9.99€) / mois
     *   - Gérer ventes, stock, achats, dépenses, contact
     *   - 3 utilisateurs (admin + 2 employés)
     *   - Dashboard de base
     *   - Rapports PDF mensuels
     */
    public enum Plan {
        GRATUIT("Plan Gratuit", "Paiement requis - Aucun accès aux fonctionnalités", 0, 0, 0, false),
        BASIC("Plan Basic", "Gestion complète boutique - 3 utilisateurs", 9.99, 6555, 3, true),
        PREMIUM("Plan Premium", "Pour moyennes entreprises", 15.24, 10000, 10, true),
        ENTREPRISE("Plan Entreprise", "Pour grandes entreprises", 22.87, 15000, Integer.MAX_VALUE, true);

        private final String libelle;
        private final String description;
        private final double prixEuro;
        private final double prixCFA;
        private final int maxUtilisateurs;
        private final boolean accesFonctionnalites;

        Plan(String libelle, String description, double prixEuro, double prixCFA, int maxUtilisateurs, boolean accesFonctionnalites) {
            this.libelle = libelle;
            this.description = description;
            this.prixEuro = prixEuro;
            this.prixCFA = prixCFA;
            this.maxUtilisateurs = maxUtilisateurs;
            this.accesFonctionnalites = accesFonctionnalites;
        }

        public String getLibelle() {
            return libelle;
        }

        public String getDescription() {
            return description;
        }

        public double getPrixEuro() {
            return prixEuro;
        }

        public double getPrixCFA() {
            return prixCFA;
        }

        public int getMaxUtilisateurs() {
            return maxUtilisateurs;
        }

        public boolean isAccesFonctionnalites() {
            return accesFonctionnalites;
        }

        /**
         * Vérifie si c'est un plan payant
         */
        public boolean isPayant() {
            return this != GRATUIT;
        }
    }

    /**
     * Génère automatiquement l'UUID avant la persistance
     */
    @PrePersist
    protected void onCreate() {
        if (this.tenantUuid == null) {
            this.tenantUuid = UUID.randomUUID().toString();
        }

        // Nettoyer les champs
        if (this.nomEntreprise != null) {
            this.nomEntreprise = this.nomEntreprise.trim();
        }
        if (this.numeroTelephone != null) {
            this.numeroTelephone = this.numeroTelephone.trim();
        }
    }

    /**
     * Vérifie si le tenant est actif et non expiré
     */
    public boolean estValide() {
        if (!actif) {
            return false;
        }

        if (dateExpiration != null) {
            return LocalDateTime.now().isBefore(dateExpiration);
        }

        return true;
    }

    /**
     * Méthode utilitaire pour ajouter un utilisateur
     */
    public void ajouterUtilisateur(UserEntity user) {
        utilisateurs.add(user);
        user.setTenant(this);
    }

    /**
     * Méthode utilitaire pour retirer un utilisateur
     */
    public void retirerUtilisateur(UserEntity user) {
        utilisateurs.remove(user);
        user.setTenant(null);
    }
}
