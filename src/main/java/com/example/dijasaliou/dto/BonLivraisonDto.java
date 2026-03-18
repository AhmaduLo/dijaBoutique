package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.BonLivraisonEntity;
import com.example.dijasaliou.entity.LigneBLEntity;
import com.example.dijasaliou.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonLivraisonDto {

    private Long id;
    private String numeroBL;
    private String statut;
    private LocalDateTime dateCreation;

    // Client / livraison
    private String clientNom;
    private String adresseLivraison;
    private String telephoneClient;
    private String note;
    private LocalDate datePrevueLivraison;
    private LocalDateTime dateLivraisonEffective;

    // Produits à livrer
    private List<LigneBLDto> lignes;

    // Infos entreprise (pour l'impression du BL)
    private String nomEntreprise;
    private String adresseTenant;
    private String villeTenant;
    private String paysTenant;
    private String nineaSiretTenant;
    private String telephoneTenant;
    private String emailTenant;
    private String proprietaireTenant;

    // ========== Ligne ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneBLDto {
        private Long id;
        private String nomProduit;
        private Double quantite;
        private String unite;
    }

    // ========== Factory de base ==========

    public static BonLivraisonDto fromEntity(BonLivraisonEntity bl) {
        List<LigneBLDto> lignesDto = bl.getLignes() == null ? List.of() :
                bl.getLignes().stream()
                        .map(BonLivraisonDto::ligneToDto)
                        .collect(Collectors.toList());

        return BonLivraisonDto.builder()
                .id(bl.getId())
                .numeroBL(bl.getNumeroBL())
                .statut(bl.getStatut() != null ? bl.getStatut().name() : null)
                .dateCreation(bl.getCreatedDate())
                .clientNom(bl.getClientNom())
                .adresseLivraison(bl.getAdresseLivraison())
                .telephoneClient(bl.getTelephoneClient())
                .note(bl.getNote())
                .datePrevueLivraison(bl.getDatePrevueLivraison())
                .dateLivraisonEffective(bl.getDateLivraisonEffective())
                .lignes(lignesDto)
                // Infos tenant de base
                .nomEntreprise(bl.getTenant() != null ? bl.getTenant().getNomEntreprise() : null)
                .adresseTenant(bl.getTenant() != null ? bl.getTenant().getAdresse() : null)
                .villeTenant(bl.getTenant() != null ? bl.getTenant().getVille() : null)
                .paysTenant(bl.getTenant() != null ? bl.getTenant().getPays() : null)
                .nineaSiretTenant(bl.getTenant() != null ? bl.getTenant().getNineaSiret() : null)
                .build();
    }

    // ========== Factory enrichie avec admin ==========

    public static BonLivraisonDto fromEntity(BonLivraisonEntity bl, UserEntity admin) {
        BonLivraisonDto dto = fromEntity(bl);
        if (admin != null) {
            dto.setTelephoneTenant(admin.getNumeroTelephone());
            dto.setEmailTenant(admin.getEmail());
            dto.setProprietaireTenant(admin.getPrenom() + " " + admin.getNom());
        }
        return dto;
    }

    private static LigneBLDto ligneToDto(LigneBLEntity ligne) {
        return LigneBLDto.builder()
                .id(ligne.getId())
                .nomProduit(ligne.getNomProduit())
                .quantite(ligne.getQuantite())
                .unite(ligne.getUnite())
                .build();
    }
}
