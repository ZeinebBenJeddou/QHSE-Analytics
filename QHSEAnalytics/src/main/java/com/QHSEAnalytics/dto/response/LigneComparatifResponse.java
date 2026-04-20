package com.QHSEAnalytics.dto.response;

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
public class LigneComparatifResponse {
    private Long kpiId;
    private String kpiNom;
    private String unite;
    private String categorieCode;
    private String categorieLibelle;
    private Double valeurN1;
    private Double valeurN;
    private Double variationAbsolue;
    private Double variationRelative;
    private String niveauVariation;
    private String tendance;
    private String analyseIa;
}