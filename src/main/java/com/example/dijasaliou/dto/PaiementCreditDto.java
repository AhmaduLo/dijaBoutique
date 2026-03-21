package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.PaiementCreditEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaiementCreditDto {

    private Long id;
    private Long creditId;
    private BigDecimal montantPaye;
    private String modePaiement;
    private LocalDate datePaiement;
    private String employeNom;
    private String note;
    private LocalDateTime createdDate;

    public static PaiementCreditDto fromEntity(PaiementCreditEntity p) {
        return PaiementCreditDto.builder()
                .id(p.getId())
                .creditId(p.getCredit() != null ? p.getCredit().getId() : null)
                .montantPaye(p.getMontantPaye())
                .modePaiement(p.getModePaiement() != null ? p.getModePaiement().name() : null)
                .datePaiement(p.getDatePaiement())
                .employeNom(p.getEmployeNom())
                .note(p.getNote())
                .createdDate(p.getCreatedDate())
                .build();
    }
}
