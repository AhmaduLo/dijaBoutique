package com.example.dijasaliou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultatDto {

    private String type;
    private int totalTraitees;
    private int importees;
    private int ignorees;
    private List<ErreurLigne> erreurs;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErreurLigne {
        private int numeroLigne;
        private String message;
    }
}
