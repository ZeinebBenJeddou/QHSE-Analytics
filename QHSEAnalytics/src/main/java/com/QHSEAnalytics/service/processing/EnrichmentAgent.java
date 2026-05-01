package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.enums.NiveauVariation;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * EnrichmentAgent: Business classification and metadata enrichment
 * 
 * Responsibilities:
 * - Apply business intelligence classification rules based on KPI thresholds
 * - Detect anomalies (variations exceeding critical thresholds)
 * - Enrich KPI data with classification and anomaly flags
 * 
 * Rules:
 * - if |variationPercentage| > seuilCritique → classification = "CRITICAL"
 * - else if |variationPercentage| > seuilModere → classification = "WARNING"  
 * - else → classification = "OK"
 * 
 * - isAnomaly = true if classification = CRITICAL OR variation exceeds seuilCritique
 * 
 * Fallback: If thresholds missing, defaults to 0
 */
@Service
public class EnrichmentAgent {
    
    private static final double DEFAULT_THRESHOLD = 0.0;
    private static final String CLASSIFICATION_CRITICAL = "CRITICAL";
    private static final String CLASSIFICATION_WARNING = "WARNING";
    private static final String CLASSIFICATION_OK = "OK";
    private static final String CLASSIFICATION_UNKNOWN = "UNKNOWN";
    
    /**
     * Enrich calculated KPIs with business classification and anomaly detection
     * 
     * @param calculatedData List of KpiCalculatedDTO from CalculationAgent
     * @return Enriched list with classification and isAnomaly fields populated
     */
    public List<KpiCalculatedDTO> enrich(List<KpiCalculatedDTO> calculatedData) {
        if (calculatedData == null || calculatedData.isEmpty()) {
            return calculatedData;
        }
        
        for (KpiCalculatedDTO kpi : calculatedData) {
            enrichKpi(kpi);
        }
        
        return calculatedData;
    }
    
    /**
     * Apply enrichment logic to a single KPI
     * 
     * @param kpi KPI to enrich
     */
    private void enrichKpi(KpiCalculatedDTO kpi) {
        // Get thresholds with null-safe defaults
        double seuilFaible = kpi.getSeuilFaible() != null ? kpi.getSeuilFaible() : DEFAULT_THRESHOLD;
        double seuilModere = kpi.getSeuilModere() != null ? kpi.getSeuilModere() : DEFAULT_THRESHOLD;
        double seuilCritique = kpi.getSeuilCritique() != null ? kpi.getSeuilCritique() : DEFAULT_THRESHOLD;
        double variationPercentage = kpi.getVariationPercentage() != null ? kpi.getVariationPercentage() : 0.0;
        
        // Apply absolute value for threshold comparison
        double absoluteVariation = Math.abs(variationPercentage);
        
        // STEP 1: Determine business classification (CRITICAL/WARNING/OK)
        String businessClassification = determineClassification(absoluteVariation, seuilModere, seuilCritique);
        kpi.setBusinessClassification(businessClassification);
        
        // STEP 2: Compute legacy classification (CRITIQUE/MODERE/FAIBLE) for backward compatibility
        String legacyClassification = computeLegacyClassification(absoluteVariation, seuilFaible, seuilModere, seuilCritique);
        kpi.setClassification(legacyClassification);
        
        // STEP 3: Detect anomalies
        boolean isAnomaly = detectAnomaly(absoluteVariation, seuilCritique, businessClassification);
        kpi.setIsAnomaly(isAnomaly);
    }
    
    /**
     * Determine business classification based on variation and thresholds
     * 
     * @param absoluteVariation Absolute value of variation percentage
     * @param seuilModere Moderate threshold
     * @param seuilCritique Critical threshold
     * @return Classification: "CRITICAL", "WARNING", or "OK"
     */
    private String determineClassification(double absoluteVariation, double seuilModere, double seuilCritique) {
        if (absoluteVariation > seuilCritique) {
            return CLASSIFICATION_CRITICAL;
        } else if (absoluteVariation > seuilModere) {
            return CLASSIFICATION_WARNING;
        } else {
            return CLASSIFICATION_OK;
        }
    }
    
    /**
     * Compute legacy classification for backward compatibility (CRITIQUE/MODERE/FAIBLE)
     * 
     * Used >= operator to maintain consistency with historical behavior
     * 
     * @param absoluteVariation Absolute value of variation percentage
     * @param seuilFaible Faible threshold
     * @param seuilModere Modere threshold
     * @param seuilCritique Critical threshold
     * @return Legacy classification: "CRITIQUE", "MODERE", "FAIBLE", or "UNKNOWN"
     */
    private String computeLegacyClassification(double absoluteVariation, double seuilFaible, double seuilModere, double seuilCritique) {
        if (absoluteVariation >= seuilCritique) {
            return NiveauVariation.CRITIQUE.name();
        } else if (absoluteVariation >= seuilModere) {
            return NiveauVariation.MODERE.name();
        } else if (absoluteVariation >= seuilFaible) {
            return NiveauVariation.FAIBLE.name();
        }
        return CLASSIFICATION_UNKNOWN;
    }
    
    /**
     * Detect if KPI represents an anomaly
     * 
     * Anomaly conditions:
     * - Classification is CRITICAL
     * - OR variation exceeds critical threshold
     * 
     * @param absoluteVariation Absolute value of variation percentage
     * @param seuilCritique Critical threshold
     * @param businessClassification Business classification level
     * @return true if anomaly detected, false otherwise
     */
    private boolean detectAnomaly(double absoluteVariation, double seuilCritique, String businessClassification) {
        return CLASSIFICATION_CRITICAL.equals(businessClassification) || 
               absoluteVariation > seuilCritique;
    }
}
