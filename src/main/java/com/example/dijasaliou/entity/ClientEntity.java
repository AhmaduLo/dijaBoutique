package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "clients",
        indexes = {
                @Index(name = "idx_client_tenant", columnList = "tenant_id"),
                @Index(name = "idx_client_nom", columnList = "nom"),
                @Index(name = "idx_client_telephone", columnList = "telephone")
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
@ToString(exclude = {"credits"})
@EqualsAndHashCode(callSuper = false, exclude = {"credits"})
public class ClientEntity extends BaseEntity {

    @NotBlank(message = "Le nom du client est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit faire entre 2 et 100 caractères")
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Size(max = 20, message = "Le téléphone ne peut dépasser 20 caractères")
    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "dette_totale", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal detteTotale = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_client_tenant"))
    @JsonIgnore
    private TenantEntity tenant;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<CreditClientEntity> credits = new ArrayList<>();

    @Override
    protected void beforePersist() {
        if (this.nom != null) {
            this.nom = this.nom.trim();
        }
        if (this.telephone != null) {
            this.telephone = this.telephone.trim();
        }
        if (this.detteTotale == null) {
            this.detteTotale = BigDecimal.ZERO;
        }
    }
}
