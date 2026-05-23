package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class RiskDetectionAgent {

    private static final int PRIORITY_RANKING_LIMIT = 20;


    public RiskAnalysisResult detect(List<KpiCalculatedDTO> enrichedData) {
        if (enrichedData == null || enrichedData.isEmpty()) {
            return new RiskAnalysisResult(List.of(), 0);
        }


        List<KpiCalculatedDTO> criticalKpis = filterCriticalRisks(enrichedData);


        int riskScore = computeRiskScore(criticalKpis);


        List<KpiCalculatedDTO> prioritizedRisks = prioritizeRisks(criticalKpis);

        return new RiskAnalysisResult(prioritizedRisks, riskScore);
    }


    private List<KpiCalculatedDTO> filterCriticalRisks(List<KpiCalculatedDTO> enrichedData) {
        return enrichedData.stream()
            .filter(this::isCriticalRisk)
            .collect(Collectors.toList());
    }


    private boolean isCriticalRisk(KpiCalculatedDTO kpi) {
        return "CRITIQUE".equals(kpi.getClassification()) ||
               "PRE_ESCALADE".equals(kpi.getClassification()) ||
               (kpi.getRiskScore() != null && kpi.getRiskScore() >= 12);
    }


    private int computeRiskScore(List<KpiCalculatedDTO> criticalKpis) {
        return (int) criticalKpis.stream()
            .filter(kpi -> "CRITIQUE".equals(kpi.getClassification()))
            .count();
    }


    private List<KpiCalculatedDTO> prioritizeRisks(List<KpiCalculatedDTO> criticalKpis) {
        return criticalKpis.stream()
            .sorted(Comparator.comparingDouble((KpiCalculatedDTO kpi) ->
                Math.abs(kpi.getVariationPercentage() != null ? kpi.getVariationPercentage() : 0.0))
                .reversed())
            .limit(PRIORITY_RANKING_LIMIT)
            .collect(Collectors.toList());
    }


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
