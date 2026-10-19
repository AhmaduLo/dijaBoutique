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
    /**
     * Identifiant de la vente à crédit à laquelle appartient ce crédit (une vente de N
     * articles = N crédits). Les crédits de même groupeCreditId forment une seule carte.
     */
    private String groupeCreditId;
    private BigDecimal montantInitial;
    private BigDecimal montantRestant;
    private BigDecimal montantPaye;
    private Integer pourcentageRembourse;
    private String statut;
    private String deviseCode;
    private Double tauxChangeApplique;
    private LocalDate dateEcheance;
    private String employeNom;
    private LocalDateTime createdDate;
    private LocalDateTime dateVente;
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
                .groupeCreditId(cleGroupe(credit))
                .montantInitial(credit.getMontantInitial())
                .montantRestant(credit.getMontantRestant())
                .montantPaye(paye)
                .pourcentageRembourse(pourcentage)
                .statut(credit.getStatut() != null ? credit.getStatut().name() : null)
                .deviseCode(credit.getDeviseCode())
                .tauxChangeApplique(credit.getTauxChangeApplique())
                .dateEcheance(credit.getDateEcheance())
                .employeNom(credit.getEmployeNom())
                .createdDate(credit.getCreatedDate())
                .dateVente(credit.getVente() != null ? credit.getVente().getDateVente() : null)
                .paiements(paiementsDto)
                .build();
    }

    /**
     * Clé de la vente à crédit d'un crédit :
     *  - groupeVenteId de la vente (toutes les ventes depuis mars 2026) ;
     *  - à défaut (ventes plus anciennes) : même client + même date de vente ;
     *  - crédit sans vente : le crédit seul.
     */
    public static String cleGroupe(CreditClientEntity credit) {
        if (credit.getVente() != null) {
            if (credit.getVente().getGroupeVenteId() != null && !credit.getVente().getGroupeVenteId().isBlank()) {
                return "G:" + credit.getVente().getGroupeVenteId();
            }
            String clientId = credit.getClient() != null ? credit.getClient().getId() : "";
            return "V:" + clientId + "|" + credit.getVente().getDateVente();
        }
        return "C:" + credit.getId();
    }
}
