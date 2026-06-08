package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.CompteCaisse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransfertCaisseRequest {

    @NotNull
    private CompteCaisse compteSource;

    @NotNull
    private CompteCaisse compteDestination;

    @NotNull
    @Positive
    private BigDecimal montant;

    @Size(max = 255)
    private String motif;
}
