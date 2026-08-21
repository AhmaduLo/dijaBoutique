package com.example.dijasaliou.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "paiements_credit",
        indexes = {
                @Index(name = "idx_paiement_credit", columnList = "credit_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"credit", "employe"})
@EqualsAndHashCode(exclude = {"credit", "employe"})
public class PaiementCreditEntity {

    public enum ModePaiement {
        ESPECES,
        WAVE,
        ORANGE_MONEY,
        VIREMENT;

        @com.fasterxml.jackson.annotation.JsonCreator
        public static ModePaiement fromString(String value) {
            if (value == null) return ESPECES;
            return ModePaiement.valueOf(value.toUpperCase());
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_paiement_credit"))
    @JsonIgnore
    private CreditClientEntity credit;

    @Column(name = "montant_paye", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantPaye;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false, length = 20)
    private ModePaiement modePaiement;

    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "employe_id", nullable = true, foreignKey = @ForeignKey(name = "fk_paiement_employe"))
    @JsonIgnore
    private UserEntity employe;

    @Column(name = "employe_nom", length = 100)
    private String employeNom;

    @Column(name = "note", length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "devise_code", length = 10, nullable = false)
    @Builder.Default
    private String deviseCode = "XOF";

    @Column(name = "taux_change_applique", nullable = false)
    @Builder.Default
    private Double tauxChangeApplique = 1.0;

    @PrePersist
    void onCreate() {
        if (this.deviseCode == null || this.deviseCode.isBlank()) {
            this.deviseCode = "XOF";
        }
        if (this.tauxChangeApplique == null) {
            this.tauxChangeApplique = 1.0;
        }
    }
}
