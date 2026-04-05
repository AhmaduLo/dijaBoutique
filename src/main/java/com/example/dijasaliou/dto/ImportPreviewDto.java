package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportPreviewDto {

    private String type;
    private int totalLignes;
    private int lignesValides;
    private int lignesInvalides;

    /** 5 premières lignes valides — champs variables selon le type */
    private List<Map<String, Object>> apercu;

    /** Erreurs détectées (ligne ignorée mais pas bloquante) */
    private List<ErreurLigne> erreurs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErreurLigne {
        private int numeroLigne;
        private String colonne;
        private String message;
    }
}
