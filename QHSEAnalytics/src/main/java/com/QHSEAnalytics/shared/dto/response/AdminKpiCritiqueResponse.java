package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminKpiCritiqueResponse {
    private Long kpiId;
    private String kpiNom;
    private String categorieCode;
    private String categorieLibelle;
    private int nombreAnalystesAvecCritique;
    private Double variationMoyenne;
}