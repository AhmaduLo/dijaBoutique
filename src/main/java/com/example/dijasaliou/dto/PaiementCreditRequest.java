package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.PaiementCreditEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaiementCreditRequest {

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    @NotNull(message = "Le mode de paiement est obligatoire")
    private PaiementCreditEntity.ModePaiement modePaiement;

    private String note;

    /**
     * Date du paiement (optionnel). Si null, le backend utilise LocalDate.now().
     * Permet de saisir un paiement antédaté pour rattraper un encaissement.
     */
    private LocalDate datePaiement;
}
