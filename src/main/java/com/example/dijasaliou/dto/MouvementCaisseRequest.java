package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.CompteCaisse;
import com.example.dijasaliou.entity.MouvementCaisseManuelEntity.TypeMouvement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MouvementCaisseRequest {

    @NotNull
    private TypeMouvement typeMouvement;

    @NotNull
    private CompteCaisse compte;

    @NotNull
    @Positive
    private BigDecimal montant;

    @NotBlank(message = "Le motif est obligatoire")
    @Size(max = 255)
    private String motif;

    /** Date du mouvement (optionnelle) — heure locale du navigateur. */
    private java.time.LocalDateTime dateMouvement;
}
