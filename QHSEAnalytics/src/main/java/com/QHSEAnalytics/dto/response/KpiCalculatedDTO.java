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
    private Double absoluteGap;
    private String status;
    private String statusColor;
    private String commentaire;
    private Boolean isBoolean;
    private String tendance;
    private String matchedKpi;
    private Long matchedKpiId;
    
    // Business Intelligence Enrichment Fields
    // direction: HIGHER_IS_BETTER | LOWER_IS_BETTER | TARGET_IS_BEST
    private String direction;
    // 0..100 confidence score computed for the comparative calculation
    private Integer calcConfidence;
    // classification: FAIBLE | MODERE | CRITIQUE | INDETERMINE
    private String classification;
    // short explanation oriented to analyst
    private String classificationReason;
    // suggest analyst review if true
    private Boolean reviewRequired;
    // data quality flags (LOW_BASE, OUTLIER, MISSING_CONTEXT, UNIT_MISMATCH)
    private java.util.List<String> dataFlags;
    // legacy fields kept for compatibility
    private String businessClassification;  // CRITICAL, WARNING, OK
    private Boolean isAnomaly;  // true if critical risk detected
} 
