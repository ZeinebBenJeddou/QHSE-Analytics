package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RiskDetectionAgent: Anomaly and critical risk detection
 * 
 * Responsibilities:
 * - Filter KPIs with critical classifications or anomalies
 * - Compute overall risk score
 * - Return prioritized list of risky KPIs for dashboard display
 * 
 * Detection Criteria:
 * - businessClassification = "CRITICAL"
 * - OR isAnomaly = true
 * 
 * Risk Scoring:
 * - riskScore = count of CRITICAL KPIs
 */
@Service
public class RiskDetectionAgent {
    
    private static final String CRITICAL_CLASSIFICATION = "CRITICAL";
    private static final int PRIORITY_RANKING_LIMIT = 20; // Top 20 risky KPIs
    
    /**
     * Detect and filter critical risks from enriched KPI data
     * 
     * @param enrichedData List of enriched KpiCalculatedDTO from EnrichmentAgent
     * @return RiskAnalysisResult containing critical KPIs and risk score
     */
    public RiskAnalysisResult detect(List<KpiCalculatedDTO> enrichedData) {
        if (enrichedData == null || enrichedData.isEmpty()) {
            return new RiskAnalysisResult(List.of(), 0);
        }
        
        // STEP 1: Filter critical KPIs
        List<KpiCalculatedDTO> criticalKpis = filterCriticalRisks(enrichedData);
        
        // STEP 2: Compute risk score (count of CRITICAL classifications)
        int riskScore = computeRiskScore(criticalKpis);
        
        // STEP 3: Sort by variation magnitude (descending) - most severe first
        List<KpiCalculatedDTO> prioritizedRisks = prioritizeRisks(criticalKpis);
        
        return new RiskAnalysisResult(prioritizedRisks, riskScore);
    }
    
    /**
     * Filter KPIs that meet critical risk criteria
     * 
     * Criteria:
     * - businessClassification = "CRITICAL" OR
     * - isAnomaly = true
     * 
     * @param enrichedData Enriched KPI list
     * @return Filtered list of critical KPIs
     */
    private List<KpiCalculatedDTO> filterCriticalRisks(List<KpiCalculatedDTO> enrichedData) {
        return enrichedData.stream()
            .filter(kpi -> isCriticalRisk(kpi))
            .collect(Collectors.toList());
    }
    
    /**
     * Determine if a KPI meets critical risk criteria
     * 
     * @param kpi KPI to evaluate
     * @return true if KPI is critical or anomalous, false otherwise
     */
    private boolean isCriticalRisk(KpiCalculatedDTO kpi) {
        return (kpi.getBusinessClassification() != null && 
                CRITICAL_CLASSIFICATION.equals(kpi.getBusinessClassification())) ||
               (kpi.getIsAnomaly() != null && kpi.getIsAnomaly());
    }
    
    /**
     * Compute overall risk score based on critical KPI count
     * 
     * @param criticalKpis List of critical KPIs
     * @return Risk score (count of CRITICAL classifications)
     */
    private int computeRiskScore(List<KpiCalculatedDTO> criticalKpis) {
        return (int) criticalKpis.stream()
            .filter(kpi -> kpi.getBusinessClassification() != null &&
                          CRITICAL_CLASSIFICATION.equals(kpi.getBusinessClassification()))
            .count();
    }
    
    /**
     * Sort and limit critical KPIs by severity (variation magnitude, descending)
     * 
     * Highest priority given to KPIs with largest deviations from thresholds
     * 
     * @param criticalKpis Unordered critical KPIs
     * @return Top N prioritized KPIs, sorted by severity
     */
    private List<KpiCalculatedDTO> prioritizeRisks(List<KpiCalculatedDTO> criticalKpis) {
        return criticalKpis.stream()
            .sorted(Comparator.comparingDouble((KpiCalculatedDTO kpi) -> 
                Math.abs(kpi.getVariationPercentage() != null ? kpi.getVariationPercentage() : 0.0))
                .reversed())
            .limit(PRIORITY_RANKING_LIMIT)
            .collect(Collectors.toList());
    }
    
    /**
     * Data class for risk analysis results
     * Encapsulates critical KPIs and overall risk metric
     */
    public static class RiskAnalysisResult {
        private final List<KpiCalculatedDTO> criticalKpis;
        private final int riskScore;
        
        public RiskAnalysisResult(List<KpiCalculatedDTO> criticalKpis, int riskScore) {
            this.criticalKpis = criticalKpis;
            this.riskScore = riskScore;
        }
        
        public List<KpiCalculatedDTO> getCriticalKpis() {
            return criticalKpis;
        }
        
        public int getRiskScore() {
            return riskScore;
        }
    }
}
