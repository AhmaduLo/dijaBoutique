package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * DTO pour la vue Super Admin d'un tenant
 * Contient les infos agrégées : tenant + admin + stats
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAdminDto {

    private Long id;
    private String tenantUuid;
    private String nomEntreprise;
    private String adresse;
    private String ville;
    private String pays;
    private String nineaSiret;

    // Infos de l'admin du tenant
    private String adminEmail;
    private String adminNom;
    private String adminPrenom;
    private String adminTelephone;
    /** true si l'admin a vérifié son adresse email. Utilisé par le super admin pour filtrer. */
    private Boolean adminEmailVerifie;

    // CRM super-admin
    private String sourceAcquisition;

    // Abonnement
    private TenantEntity.Plan plan;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDebutEssai;
    private LocalDateTime dateExpiration;
    private Boolean essaiUtilise;

    // Stats
    private Long nbUtilisateurs;
    private Long nbVentes;
    private OffsetDateTime derniereActivite;

    // Statut calculé : ESSAI, ACTIF, EXPIRE, SUSPENDU
    private String statut;

    public static TenantAdminDto fromEntity(TenantEntity tenant, long nbUsers, long nbVentes, UserEntity admin, LocalDateTime derniereActivite) {
        return TenantAdminDto.builder()
                .id(tenant.getId())
                .tenantUuid(tenant.getTenantUuid())
                .nomEntreprise(tenant.getNomEntreprise())
                .adresse(tenant.getAdresse())
                .ville(tenant.getVille())
                .pays(tenant.getPays())
                .nineaSiret(tenant.getNineaSiret())
                .sourceAcquisition(tenant.getSourceAcquisition())
                .adminEmail(admin != null ? admin.getEmail() : null)
                .adminNom(admin != null ? admin.getNom() : null)
                .adminPrenom(admin != null ? admin.getPrenom() : null)
                .adminTelephone(admin != null ? admin.getNumeroTelephone() : null)
                .adminEmailVerifie(admin != null ? admin.getEmailVerifie() : null)
                .plan(tenant.getPlan())
                .actif(tenant.getActif())
                .dateCreation(tenant.getDateCreation())
                .dateDebutEssai(tenant.getDateDebutEssai())
                .dateExpiration(tenant.getDateExpiration())
                .essaiUtilise(tenant.getEssaiUtilise())
                .nbUtilisateurs(nbUsers)
                .nbVentes(nbVentes)
                .derniereActivite(derniereActivite != null ? derniereActivite.atOffset(ZoneOffset.UTC) : null)
                .statut(calculerStatut(tenant))
                .build();
    }

    private static String calculerStatut(TenantEntity tenant) {
        if (!Boolean.TRUE.equals(tenant.getActif())) return "SUSPENDU";
        if (tenant.essaiGratuitValide()) return "ESSAI";
        if (tenant.getPlan() == TenantEntity.Plan.GRATUIT) return "EXPIRE";
        if (tenant.getDateExpiration() == null) return "ACTIF";
        return LocalDateTime.now().isBefore(tenant.getDateExpiration()) ? "ACTIF" : "EXPIRE";
    }
}
