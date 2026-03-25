package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.CreditClientEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditClientDto {

    private String id;
    private String clientId;
    private String clientNom;
    private String clientTelephone;
    private String venteId;
    private BigDecimal montantInitial;
    private BigDecimal montantRestant;
    private BigDecimal montantPaye;
    private Integer pourcentageRembourse;
    private String statut;
    private LocalDate dateEcheance;
    private String employeNom;
    private LocalDateTime createdDate;
    private List<PaiementCreditDto> paiements;

    public static CreditClientDto fromEntity(CreditClientEntity credit) {
        BigDecimal paye = credit.getMontantInitial().subtract(credit.getMontantRestant());

        int pourcentage = 0;
        if (credit.getMontantInitial().compareTo(BigDecimal.ZERO) > 0) {
            pourcentage = paye
                    .multiply(new BigDecimal("100"))
                    .divide(credit.getMontantInitial(), 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        List<PaiementCreditDto> paiementsDto = credit.getPaiements() != null
                ? credit.getPaiements().stream()
                        .map(PaiementCreditDto::fromEntity)
                        .collect(Collectors.toList())
                : List.of();

        return CreditClientDto.builder()
                .id(credit.getId())
                .clientId(credit.getClient() != null ? credit.getClient().getId() : null)
                .clientNom(credit.getClient() != null ? credit.getClient().getNom() : null)
                .clientTelephone(credit.getClient() != null ? credit.getClient().getTelephone() : null)
                .venteId(credit.getVente() != null ? credit.getVente().getId() : null)
                .montantInitial(credit.getMontantInitial())
                .montantRestant(credit.getMontantRestant())
                .montantPaye(paye)
                .pourcentageRembourse(pourcentage)
                .statut(credit.getStatut() != null ? credit.getStatut().name() : null)
                .dateEcheance(credit.getDateEcheance())
                .employeNom(credit.getEmployeNom())
                .createdDate(credit.getCreatedDate())
                .paiements(paiementsDto)
                .build();
    }
}
