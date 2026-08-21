package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrée/sortie manuelle d'argent dans la caisse, hors achats/ventes/dépenses.
 *
 * Exemples d'usage :
 *   - ENTREE  : prêt familial, argent trouvé, ajustement positif d'inventaire caisse
 *   - SORTIE  : retrait personnel du propriétaire, perte, dépôt en banque
 *
 * Le motif est OBLIGATOIRE pour la traçabilité.
 */
@Entity
@Table(
        name = "mouvement_caisse_manuel",
        indexes = {
                @Index(name = "idx_mvt_manuel_tenant",      columnList = "tenant_id"),
                @Index(name = "idx_mvt_manuel_date",        columnList = "date_mouvement"),
                @Index(name = "idx_mvt_manuel_tenant_date", columnList = "tenant_id, date_mouvement")
        }
)
@org.hibernate.annotations.Filter(name = "tenantFilter",
        condition = "tenant_id = (SELECT t.id FROM tenants t WHERE t.tenant_uuid = :tenantId)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class MouvementCaisseManuelEntity extends BaseEntity {

    public enum TypeMouvement {
        ENTREE,
        SORTIE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mvt_manuel_tenant"))
    @JsonIgnore
    @ToString.Exclude
    private TenantEntity tenant;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false, length = 10)
    private TypeMouvement typeMouvement;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "compte", nullable = false, length = 20)
    private CompteCaisse compte;

    @NotNull
    @Positive
    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @NotBlank(message = "Le motif est obligatoire")
    @Size(max = 255)
    @Column(name = "motif", nullable = false, length = 255)
    private String motif;

    @NotNull
    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;

    @Column(name = "fait_par", length = 36)
    private String faitPar;

    @Override
    protected void beforePersist() {
        if (this.dateMouvement == null) {
            this.dateMouvement = LocalDateTime.now();
        }
    }
}
