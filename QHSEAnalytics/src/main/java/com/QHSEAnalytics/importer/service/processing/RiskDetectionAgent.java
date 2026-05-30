package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class RiskDetectionAgent {

    private static final int PRIORITY_RANKING_LIMIT = 20;


    public RiskAnalysisResult detect(List<KpiCalculatedDTO> enrichedData) {
        if (enrichedData == null || enrichedData.isEmpty()) {
            log.info("\n[RISK]\n  Aucune donnée KPI pour l'analyse de risque");
            return new RiskAnalysisResult(List.of(), 0);
        }

        List<KpiCalculatedDTO> criticalKpis    = filterCriticalRisks(enrichedData);
        List<KpiCalculatedDTO> prioritizedRisks = prioritizeRisks(criticalKpis);

        // classify by level
        List<KpiCalculatedDTO> critique     = enrichedData.stream().filter(k -> "CRITIQUE".equals(k.getClassification())).toList();
        List<KpiCalculatedDTO> modere       = enrichedData.stream().filter(k -> "MODERE".equals(k.getClassification())).toList();
        long faible                          = enrichedData.stream().filter(k -> "FAIBLE".equals(k.getClassification())).count();
        long indetermine                     = enrichedData.stream().filter(k -> k.getClassification() == null || "INDETERMINE".equals(k.getClassification())).count();

        String critiqueDetail = critique.stream()
                .map(k -> k.getKpiName() + " " + formatVariation(k.getVariationPercentage()))
                .collect(Collectors.joining(" | "));
        String modereDetail = modere.stream()
                .map(k -> k.getKpiName() + " " + formatVariation(k.getVariationPercentage()))
                .collect(Collectors.joining(" | "));

        // compute an aggregate risk score 0-100 from individual riskScores
        int aggregateRisk100 = enrichedData.stream()
                .mapToInt(k -> k.getRiskScore() != null ? k.getRiskScore() : 0)
                .sum();
        int maxPossible = enrichedData.size() * 25; // max riskScore per KPI = 5*5=25
        int riskScore100 = maxPossible == 0 ? 0 : (int) Math.min(100, Math.round(aggregateRisk100 * 100.0 / maxPossible));

        log.info("\n[RISK]" +
                        "\n  Score risque global : {}/100" +
                        "\n  CRITIQUE ({}) : {}" +
                        "\n  MODERE   ({}) : {}" +
                        "\n  FAIBLE   ({})" +
                        "\n  INDETERMINE ({})",
                riskScore100,
                critique.size(), critiqueDetail.isEmpty() ? "aucun" : critiqueDetail,
                modere.size(),   modereDetail.isEmpty()   ? "aucun" : modereDetail,
                faible,
                indetermine);

        return new RiskAnalysisResult(prioritizedRisks, riskScore100);
    }

    private static String formatVariation(Double v) {
        if (v == null) return "(N/A)";
        return String.format("%+.1f%%", v);
    }


    private List<KpiCalculatedDTO> filterCriticalRisks(List<KpiCalculatedDTO> enrichedData) {
        return enrichedData.stream()
            .filter(this::isCriticalRisk)
            .collect(Collectors.toList());
    }


    private boolean isCriticalRisk(KpiCalculatedDTO kpi) {
        return "CRITIQUE".equals(kpi.getClassification()) ||
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
