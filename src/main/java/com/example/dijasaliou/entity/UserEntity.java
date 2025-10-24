package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "utilisateurs", indexes = {@Index(name = "idx_email", columnList = "email"), @Index(name = "idx_role", columnList = "role")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 50)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "mot_de_passe", nullable = false, length = 255)
    @JsonIgnore
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Référence à l'utilisateur (ADMIN) qui a créé ce compte
     * null = compte créé lors de l'initialisation (premier ADMIN)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnore
    private UserEntity createdByUser;

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default  // Pour Lombok Builder
    @JsonManagedReference("user-achats")
    @ToString.Exclude
    private List<AchatEntity> achats = new ArrayList<>();

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonManagedReference("user-ventes")
    @ToString.Exclude
    private List<VenteEntity> ventes = new ArrayList<>();

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonManagedReference("user-depenses")
    @ToString.Exclude
    private List<DepenseEntity> depenses = new ArrayList<>();


    /**
     * MÉTHODES UTILITAIRES pour gérer les relations bidirectionnelles
     * <p>
     * Pourquoi ? Pour maintenir la cohérence des deux côtés :
     * - Côté parent (Utilisateur) : liste d'achats
     * - Côté enfant (Achat) : référence à l'utilisateur
     */
    public void ajouterAchat(AchatEntity achat) {
        achats.add(achat);
        achat.setUtilisateur(this);
    }

    public void retirerAchat(AchatEntity achat) {
        achats.remove(achat);
        achat.setUtilisateur(null);
    }

    public void ajouterVente(VenteEntity vente) {
        ventes.add(vente);
        vente.setUtilisateur(this);
    }

    public void retirerVente(VenteEntity vente) {
        ventes.remove(vente);
        vente.setUtilisateur(null);
    }

    public void ajouterDepense(DepenseEntity depense) {
        depenses.add(depense);
        depense.setUtilisateur(this);
    }

    public void retirerDepense(DepenseEntity depense) {
        depenses.remove(depense);
        depense.setUtilisateur(null);
    }

    /**
     * Enum pour les rôles
     * <p>
     * Définir comme classe interne = Couplage fort avec Utilisateur
     * Alternative : Classe séparée si utilisé ailleurs
     */
    public enum Role {
        ADMIN("Administrateur", "Accès complet"), USER("Utilisateur", "Accès standard");

        private final String libelle;
        private final String description;

        Role(String libelle, String description) {
            this.libelle = libelle;
            this.description = description;
        }

        public String getLibelle() {
            return libelle;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Callback JPA : Exécuté AVANT persist/update
     * Utile pour validation ou calculs
     */
    protected void validate() {
        // Validation personnalisée
        if (email != null) {
            email = email.toLowerCase().trim();
        }
    }
}
