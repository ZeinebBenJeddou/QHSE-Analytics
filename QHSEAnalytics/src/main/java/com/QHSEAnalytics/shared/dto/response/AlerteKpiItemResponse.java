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
public class AlerteKpiItemResponse {
    private Long kpiId;
    private String kpiNom;
    private String categorieCode;
    private String categorieLibelle;
    private Double variationRelative;
    private Double variationAbsolue;
    private Double valeurN;
    private Double valeurN1;
    private String niveauVariation;
    private String tendance;
}
