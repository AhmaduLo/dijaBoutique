package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "credits_clients",
        indexes = {
                @Index(name = "idx_credit_tenant", columnList = "tenant_id"),
                @Index(name = "idx_credit_client", columnList = "client_id"),
                @Index(name = "idx_credit_statut", columnList = "statut")
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
@ToString(exclude = {"client", "vente", "employe", "paiements"})
@EqualsAndHashCode(callSuper = false, exclude = {"client", "vente", "employe", "paiements"})
public class CreditClientEntity extends BaseEntity {

    public enum StatutCredit {
        EN_ATTENTE,
        PARTIEL,
        SOLDE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, foreignKey = @ForeignKey(name = "fk_credit_client"))
    @JsonIgnore
    private ClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "vente_id", nullable = true, foreignKey = @ForeignKey(name = "fk_credit_vente"))
    @JsonIgnore
    private VenteEntity vente;

    @Column(name = "montant_initial", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantInitial;

    @Column(name = "montant_restant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantRestant;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutCredit statut = StatutCredit.EN_ATTENTE;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "employe_id", nullable = true, foreignKey = @ForeignKey(name = "fk_credit_employe"))
    @JsonIgnore
    private UserEntity employe;

    @Column(name = "employe_nom", length = 100)
    private String employeNom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_credit_tenant"))
    @JsonIgnore
    private TenantEntity tenant;

    @OneToMany(mappedBy = "credit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<PaiementCreditEntity> paiements = new ArrayList<>();
}
