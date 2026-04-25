package com.QHSEAnalytics.dto.response;

import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.enums.QualityStatus;
import com.QHSEAnalytics.enums.Tendance;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultatKpiResponse {
    private Long id;
    private Long kpiId;
    private String kpiNom;
    private String kpiUnite;
    private String categorieCode;
    private String categorieLibelle;
    private Integer periodeN1;
    private Integer periodeN;
    private Double valeurN1;
    private Double valeurN;
    private Double variationAbsolue;
    private Double variationRelative;
    private NiveauVariation niveauVariation;
    private Tendance tendance;
    private Double confidenceScore;
    private QualityStatus qualityStatus;
    private String analyseIa;
    private LocalDateTime createdAt;
}
