package com.QHSEAnalytics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoriqueItemResponse {
    private Long importId;
    private String nomFichier;
    private Integer periodeN1;
    private Integer periodeN;
    private String statut;
    private int nombreKpisCritiques;
    private LocalDateTime dateImport;
}