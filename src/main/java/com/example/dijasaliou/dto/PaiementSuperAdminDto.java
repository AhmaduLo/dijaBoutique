package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.PaiementSuperAdminEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un paiement super admin.
 */
public record PaiementSuperAdminDto(
        Long id,
        Long tenantId,
        String nomEntreprise,
        String plan,
        BigDecimal montant,
        LocalDateTime datePaiement,
        String moisDebut,
        String modePaiement,
        String periode,
        String note,
        Long validePar,
        LocalDateTime createdAt
) {
    public static PaiementSuperAdminDto fromEntity(PaiementSuperAdminEntity e) {
        return new PaiementSuperAdminDto(
                e.getId(),
                e.getTenant().getId(),
                e.getTenant().getNomEntreprise(),
                e.getPlan().name(),
                e.getMontant(),
                e.getDatePaiement(),
                e.getMoisDebut(),
                e.getModePaiement(),
                e.getPeriode(),
                e.getNote(),
                e.getValidePar(),
                e.getCreatedAt()
        );
    }
}
