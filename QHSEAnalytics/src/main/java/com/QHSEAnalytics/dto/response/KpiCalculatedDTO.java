package com.QHSEAnalytics.dto.response;

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
    private String classification;
    private String tendance;
    private String matchedKpi;
    private Long matchedKpiId;
    
    // Business Intelligence Enrichment Fields
    private String businessClassification;  // CRITICAL, WARNING, OK
    private Boolean isAnomaly;  // true if critical risk detected
} 
