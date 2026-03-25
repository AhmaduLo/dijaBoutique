package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.ClientEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {

    private String id;
    private String nom;
    private String telephone;
    private BigDecimal detteTotale;
    private long nombreCreditsActifs;

    public static ClientDto fromEntity(ClientEntity client, long nombreCreditsActifs) {
        return ClientDto.builder()
                .id(client.getId())
                .nom(client.getNom())
                .telephone(client.getTelephone())
                .detteTotale(client.getDetteTotale())
                .nombreCreditsActifs(nombreCreditsActifs)
                .build();
    }

    public static ClientDto fromEntity(ClientEntity client) {
        return fromEntity(client, 0L);
    }
}
