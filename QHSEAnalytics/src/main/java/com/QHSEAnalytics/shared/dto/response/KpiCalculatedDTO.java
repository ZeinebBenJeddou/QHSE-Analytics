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

    // P2.1 — Matching quality
    private Double matchConfidence; // 0.0–1.0 Jaro-Winkler score (null = exact match or no match)

    // P3.1 — SPC Statistical Process Control (requires >= 5 historical points)
    private Double spcMean;
    private Double spcStd;
    private Double spcUcl;  // mean + 3σ
    private Double spcLcl;  // mean - 3σ
    private Boolean spcOutOfControl; // true if valeurN is outside [LCL, UCL]

    // P2.2 — Risk matrix
    private Integer riskProbability; // 1–5 (derived from classification + trend)
    private Integer riskImpact;      // 1–5 (derived from category severity + magnitude)
    private Integer riskScore;       // riskProbability × riskImpact (1–25)
    private String  riskLevel;       // LOW | MEDIUM | HIGH | CRITICAL
} 
