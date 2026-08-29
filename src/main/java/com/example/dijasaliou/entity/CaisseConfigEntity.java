package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuration de la caisse pour un tenant.
 *
 * 1 ligne par tenant. Stocke :
 *   - les soldes initiaux par compte (Espèces / Wave / OM)
 *   - la date d'activation : seules les transactions à partir de cette date
 *     impactent la caisse (les achats / ventes / dépenses antérieurs sont ignorés)
 *
 * Le solde actuel est calculé EN TEMPS RÉEL dans CaisseService — il n'est jamais
 * stocké pour éviter toute désynchronisation.
 *
 * Les 4 champs soldeInitialXxx sont des termes correctifs internes (cible saisie
 * moins contributions déjà enregistrées depuis la date d'activation), pas des
 * soldes réels affichés — ils peuvent légitimement être négatifs si des
 * ventes/achats payés ont déjà dépassé le montant cible saisi à l'activation.
 */
@Entity
@Table(
        name = "caisse_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_caisse_config_tenant", columnNames = "tenant_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class CaisseConfigEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_caisse_config_tenant"))
    @JsonIgnore
    @ToString.Exclude
    private TenantEntity tenant;

    @NotNull
    @Column(name = "solde_initial_especes", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeInitialEspeces = BigDecimal.ZERO;

    @NotNull
    @Column(name = "solde_initial_wave", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeInitialWave = BigDecimal.ZERO;

    @NotNull
    @Column(name = "solde_initial_om", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeInitialOm = BigDecimal.ZERO;

    @NotNull
    @Column(name = "solde_initial_virement", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeInitialVirement = BigDecimal.ZERO;

    @NotNull
    @Column(name = "date_activation", nullable = false)
    private LocalDateTime dateActivation;

    @Column(name = "active_par", length = 36)
    private String activePar;
}
