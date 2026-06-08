package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transfert d'argent entre 2 comptes de caisse (ex: Wave → Espèces).
 *
 * Impact sur le solde :
 *   - solde compte_source       diminue de montant
 *   - solde compte_destination  augmente de montant
 *   - solde TOTAL reste identique
 */
@Entity
@Table(
        name = "transfert_caisse",
        indexes = {
                @Index(name = "idx_transfert_tenant",      columnList = "tenant_id"),
                @Index(name = "idx_transfert_date",        columnList = "date_transfert"),
                @Index(name = "idx_transfert_tenant_date", columnList = "tenant_id, date_transfert")
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
public class TransfertCaisseEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transfert_tenant"))
    @JsonIgnore
    @ToString.Exclude
    private TenantEntity tenant;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "compte_source", nullable = false, length = 20)
    private CompteCaisse compteSource;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "compte_destination", nullable = false, length = 20)
    private CompteCaisse compteDestination;

    @NotNull
    @Positive
    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Size(max = 255)
    @Column(name = "motif", length = 255)
    private String motif;

    @NotNull
    @Column(name = "date_transfert", nullable = false)
    private LocalDateTime dateTransfert;

    @Column(name = "fait_par", length = 36)
    private String faitPar;

    @Override
    protected void beforePersist() {
        if (this.dateTransfert == null) {
            this.dateTransfert = LocalDateTime.now();
        }
        if (this.compteSource != null && this.compteSource.equals(this.compteDestination)) {
            throw new IllegalArgumentException("Le compte source et destination doivent être différents");
        }
    }
}
