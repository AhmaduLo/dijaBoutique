package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.FactureEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO Facture pour la vue Super Admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactureDto {

    private Long id;
    private String numeroFacture;

    // Client
    private String nomEntreprise;
    private String adresse;
    private String ville;
    private String pays;
    private String nineaSiret;
    private String adminEmail;
    private String adminNom;
    private String adminPrenom;
    private String adminTelephone;

    // Facturation
    private String plan;
    private Double montantCFA;
    private Double montantEuro;
    private LocalDateTime dateFacture;
    private LocalDateTime dateDebutPeriode;
    private LocalDateTime dateFinPeriode;
    private String statut;
    private String waveTransactionId;
    private Boolean emailEnvoye;
    private LocalDateTime dateEnvoiEmail;

    public static FactureDto fromEntity(FactureEntity f) {
        return FactureDto.builder()
                .id(f.getId())
                .numeroFacture(f.getNumeroFacture())
                .nomEntreprise(f.getNomEntreprise())
                .adresse(f.getAdresse())
                .ville(f.getVille())
                .pays(f.getPays())
                .nineaSiret(f.getNineaSiret())
                .adminEmail(f.getAdminEmail())
                .adminNom(f.getAdminNom())
                .adminPrenom(f.getAdminPrenom())
                .adminTelephone(f.getAdminTelephone())
                .plan(f.getPlan())
                .montantCFA(f.getMontantCFA())
                .montantEuro(f.getMontantEuro())
                .dateFacture(f.getDateFacture())
                .dateDebutPeriode(f.getDateDebutPeriode())
                .dateFinPeriode(f.getDateFinPeriode())
                .statut(f.getStatut() != null ? f.getStatut().name() : null)
                .waveTransactionId(f.getWaveTransactionId())
                .emailEnvoye(f.getEmailEnvoye())
                .dateEnvoiEmail(f.getDateEnvoiEmail())
                .build();
    }
}
