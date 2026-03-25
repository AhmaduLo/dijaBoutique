package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bon de livraison — document remis au livreur et au client lors d'une livraison à domicile.
 *
 * - Sans prix (document logistique uniquement)
 * - Listé les produits et quantités à livrer
 * - Peut être imprimé et signé par le livreur et le client
 */
@Entity
@Table(
        name = "bons_livraison",
        indexes = {
                @Index(name = "idx_bl_tenant", columnList = "tenant_id"),
                @Index(name = "idx_bl_statut", columnList = "statut"),
                @Index(name = "idx_bl_client", columnList = "client_nom")
        }
)
@org.hibernate.annotations.Filter(
        name = "tenantFilter",
        condition = "tenant_id = (SELECT t.id FROM tenants t WHERE t.tenant_uuid = :tenantId)"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BonLivraisonEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    public enum Statut {
        EN_ATTENTE,
        EN_COURS,
        LIVRE,
        ANNULE
    }

    @Column(name = "numero_bl", nullable = false, unique = true, length = 25)
    private String numeroBL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bl_tenant"))
    private TenantEntity tenant;

    @Column(name = "client_nom", nullable = false, length = 100)
    private String clientNom;

    @Column(name = "adresse_livraison", nullable = false, length = 255)
    private String adresseLivraison;

    @Column(name = "telephone_client", length = 20)
    private String telephoneClient;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "date_prevue_livraison")
    private LocalDate datePrevueLivraison;

    @Column(name = "date_livraison_effective")
    private LocalDateTime dateLivraisonEffective;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private Statut statut = Statut.EN_ATTENTE;

    @OneToMany(mappedBy = "bonLivraison", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LigneBLEntity> lignes = new ArrayList<>();
}
