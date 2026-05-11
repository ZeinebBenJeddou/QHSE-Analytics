package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KpiCalculatedDTO {
    private int rowIndex;
    private String kpiName;
    private String categorie;
    private String categorieCode;
    private String unite;
    private Double valeurN1;
    private Double valeurN;
    private Double seuilFaible;
    private Double seuilModere;
    private Double seuilCritique;
    private String definition;
    private Double variationAbsolute;
    private Double variationPercentage;
    private Double absoluteGap;
    private String status;
    private String statusColor;
    private String commentaire;
    private Boolean isBoolean;
    private String tendance;
    private String matchedKpi;
    private Long matchedKpiId;


    private String direction;

    private Integer calcConfidence;

    private String classification;

    private String classificationReason;

    private Boolean reviewRequired;

    private java.util.List<String> dataFlags;

    private String businessClassification;
    private Boolean isAnomaly;


    private Double matchConfidence;


    private Double spcMean;
    private Double spcStd;
    private Double spcUcl;
    private Double spcLcl;
    private Boolean spcOutOfControl;


    private Integer riskProbability;
    private Integer riskImpact;
    private Integer riskScore;
    private String  riskLevel;
}
