package com.example.dijasaliou.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateBonLivraisonRequest {

    @NotBlank(message = "Le nom du client est obligatoire")
    private String clientNom;

    @NotBlank(message = "L'adresse de livraison est obligatoire")
    private String adresseLivraison;

    private String telephoneClient;
    private String note;
    private LocalDate datePrevueLivraison;

    @NotEmpty(message = "Le bon de livraison doit contenir au moins un produit")
    @Valid
    private List<LigneBLRequest> lignes;

    @Data
    public static class LigneBLRequest {

        @NotBlank(message = "Le nom du produit est obligatoire")
        private String nomProduit;

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être positive")
        private Double quantite;

        private String unite;
    }
}
