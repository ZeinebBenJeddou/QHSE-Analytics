package com.QHSEAnalytics.analytics.service.processing;

import com.QHSEAnalytics.shared.dto.response.AiActionPlanItemResponse;
import com.QHSEAnalytics.shared.dto.response.AiAnalysisStructuredResponse;
import com.QHSEAnalytics.shared.dto.response.AiConfidenceResponse;
import com.QHSEAnalytics.shared.dto.response.AiKpiInsightResponse;
import com.QHSEAnalytics.shared.dto.response.AiPredictiveAlertResponse;
import com.QHSEAnalytics.shared.dto.response.AiRecommendationResponse;
import com.QHSEAnalytics.shared.dto.response.AiRootCauseResponse;
import com.QHSEAnalytics.shared.dto.response.AiTraceabilityResponse;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.analytics.service.TextNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class StructuredAnalysisValidator {

    public List<String> validate(AiAnalysisStructuredResponse response, List<KpiCalculatedDTO> availableKpis) {
        List<String> errors = new ArrayList<>();
        if (response == null) {
            errors.add("Response is null.");
            return errors;
        }

        if (isBlank(response.getGlobalSummary())) {
            errors.add("globalSummary is mandatory.");
        }

        AiConfidenceResponse confidence = response.getConfidence();
        if (confidence == null) {
            errors.add("confidence object is mandatory.");
        } else {
            if (confidence.getOverall() == null) {
                errors.add("confidence.overall is mandatory.");
            } else if (confidence.getOverall() < 0 || confidence.getOverall() > 100) {
                errors.add("confidence.overall must be between 0 and 100.");
            }
            if (confidence.getSections() == null || confidence.getSections().isEmpty()) {
                errors.add("confidence.sections is mandatory and should contain section-level values.");
            }
        }

        if (response.getProbableCauses() == null) {
            errors.add("probableCauses array is mandatory.");
        }
        if (response.getRecommendations() == null) {
            errors.add("recommendations array is mandatory.");
        }
        if (response.getActionPlan() == null) {
            errors.add("actionPlan array is mandatory.");
        }

        if (response.getTraceability() == null) {
            errors.add("traceability object is mandatory.");
        } else {
            validateTraceability(response.getTraceability(), errors);
        }

        if (response.getKpiInsights() == null || response.getKpiInsights().isEmpty()) {
            errors.add("kpiInsights must contain at least one KPI item.");
        } else {
            validateKpiInsights(response.getKpiInsights(), availableKpis, errors);
        }

        validateRootCauseAnalysis(response.getRootCauseAnalysis(), errors);
        validatePredictiveAlerts(response.getPredictiveAlerts(), errors);

        return errors;
    }

    private void validateRootCauseAnalysis(List<AiRootCauseResponse> rootCauses, List<String> errors) {
        if (rootCauses == null || rootCauses.isEmpty()) return;
        Set<String> validIshikawa = Set.of("Homme", "Machine", "Méthode", "Milieu", "Matière");
        for (int i = 0; i < rootCauses.size(); i++) {
            AiRootCauseResponse rc = rootCauses.get(i);
            String prefix = "rootCauseAnalysis[" + i + "]";
            if (rc == null) { errors.add(prefix + " must not be null."); continue; }
            if (isBlank(rc.getKpiRef()))     errors.add(prefix + ".kpiRef is mandatory.");
            if (isBlank(rc.getRootCause()))  errors.add(prefix + ".rootCause is mandatory.");
            if (rc.getWhyChain() == null || rc.getWhyChain().size() < 3)
                errors.add(prefix + ".whyChain must contain at least 3 entries.");
            if (!isBlank(rc.getIshikawaCategory()) && !validIshikawa.contains(rc.getIshikawaCategory()))
                errors.add(prefix + ".ishikawaCategory must be one of: " + validIshikawa);
        }
    }

    private void validatePredictiveAlerts(List<AiPredictiveAlertResponse> alerts, List<String> errors) {
        if (alerts == null || alerts.isEmpty()) return;
        Set<String> validSeverity = Set.of("LOW", "MEDIUM", "HIGH");
        for (int i = 0; i < alerts.size(); i++) {
            AiPredictiveAlertResponse alert = alerts.get(i);
            String prefix = "predictiveAlerts[" + i + "]";
            if (alert == null) { errors.add(prefix + " must not be null."); continue; }
            if (isBlank(alert.getKpiRef()))    errors.add(prefix + ".kpiRef is mandatory.");
            if (isBlank(alert.getProjection())) errors.add(prefix + ".projection is mandatory.");
            if (alert.getEstimatedHorizonMonths() != null
                    && (alert.getEstimatedHorizonMonths() < 0 || alert.getEstimatedHorizonMonths() > 24))
                errors.add(prefix + ".estimatedHorizonMonths must be between 0 and 24.");
            if (alert.getConfidence() != null
                    && (alert.getConfidence() < 0 || alert.getConfidence() > 100))
                errors.add(prefix + ".confidence must be between 0 and 100.");
            if (!isBlank(alert.getSeverity()) && !validSeverity.contains(alert.getSeverity()))
                errors.add(prefix + ".severity must be LOW, MEDIUM or HIGH.");
        }
    }

    private void validateTraceability(AiTraceabilityResponse trace, List<String> errors) {
        if (isBlank(trace.getModelName())) {
            errors.add("traceability.modelName is mandatory.");
        }
        if (isBlank(trace.getGeneratedAt())) {
            errors.add("traceability.generatedAt is mandatory.");
        }
        if (trace.getContextSourcesUsed() == null || trace.getContextSourcesUsed().isEmpty()) {
            errors.add("traceability.contextSourcesUsed must contain at least one source.");
        }
    }

    private void validateKpiInsights(List<AiKpiInsightResponse> insights, List<KpiCalculatedDTO> availableKpis, List<String> errors) {
        Set<String> knownNames = new HashSet<>();
        Set<String> knownIds = new HashSet<>();
        if (availableKpis != null) {
            for (KpiCalculatedDTO kpi : availableKpis) {
                if (kpi.getKpiName() != null) {
                    String normalizedName = TextNormalizer.normalizeForMatching(kpi.getKpiName());
                    knownNames.add(normalizedName);
                }
                if (kpi.getMatchedKpiId() != null) {
                    String normalizedId = String.valueOf(kpi.getMatchedKpiId());
                    knownIds.add(normalizedId);
                }
            }
        }

        Set<String> coveredIds = new HashSet<>();
        Set<String> coveredNames = new HashSet<>();

        for (int i = 0; i < insights.size(); i++) {
            AiKpiInsightResponse insight = insights.get(i);
            String prefix = String.format("kpiInsights[%d]", i);
            if (insight == null) {
                errors.add(prefix + " must not be null.");
                continue;
            }
            if (insight.getKpiId() == null && isBlank(insight.getKpiName())) {
                errors.add(prefix + " must contain either kpiId or kpiName.");
            }
            if (insight.getKpiId() != null && !knownIds.isEmpty() && !knownIds.contains(String.valueOf(insight.getKpiId()))) {
                errors.add(prefix + " references unknown kpiId=" + insight.getKpiId());
            }
            if (isBlank(insight.getKpiName())) {
                errors.add(prefix + ".kpiName is mandatory.");
            } else {
                String normalizedName = TextNormalizer.normalizeForMatching(insight.getKpiName());
                if (!knownNames.isEmpty() && !knownNames.contains(normalizedName)) {
                    errors.add(prefix + " references unknown kpiName='" + insight.getKpiName() + "'.");
                }
                coveredNames.add(normalizedName);
            }
            if (insight.getKpiId() != null) {
                coveredIds.add(String.valueOf(insight.getKpiId()));
            }
            if (insight.getKpiId() != null && !knownIds.isEmpty() && !knownIds.contains(String.valueOf(insight.getKpiId()))) {
                errors.add(prefix + " references unknown kpiId=" + insight.getKpiId());
            }
            if (insight.getConfidence() == null) {
                errors.add(prefix + ".confidence is mandatory.");
            } else if (insight.getConfidence() < 0 || insight.getConfidence() > 100) {
                errors.add(prefix + ".confidence must be between 0 and 100.");
            }
            if (isBlank(insight.getInsight())) {
                errors.add(prefix + ".insight is mandatory.");
            }
            if (insight.getProbableCauses() == null) {
                errors.add(prefix + ".probableCauses is mandatory.");
            }
            if (insight.getRecommendations() == null) {
                errors.add(prefix + ".recommendations is mandatory.");
            }
            if (isBlank(insight.getActionImmediate())) {
                errors.add(prefix + ".actionImmediate is mandatory.");
            }
            if (isBlank(insight.getUrgency())) {
                errors.add(prefix + ".urgency is mandatory.");
            }
            if (isBlank(insight.getOwnerRole())) {
                errors.add(prefix + ".ownerRole is mandatory.");
            }
            if (isBlank(insight.getDueHorizon())) {
                errors.add(prefix + ".dueHorizon is mandatory.");
            }
            if (isBlank(insight.getSuccessMetric())) {
                errors.add(prefix + ".successMetric is mandatory.");
            }
            if (isBlank(insight.getRiskIfNotDone())) {
                errors.add(prefix + ".riskIfNotDone is mandatory.");
            }
        }

        if (availableKpis == null || availableKpis.isEmpty()) {
            return;
        }

        List<String> missingKpis = new ArrayList<>();
        for (KpiCalculatedDTO expected : availableKpis) {
            String expectedId = expected.getMatchedKpiId() == null ? null : String.valueOf(expected.getMatchedKpiId());
            String expectedName = TextNormalizer.normalizeForMatching(expected.getKpiName());
            boolean matchedById = expectedId != null && coveredIds.contains(expectedId);
            boolean matchedByName = !expectedName.isBlank() && coveredNames.contains(expectedName);
            if (!matchedById && !matchedByName) {
                missingKpis.add(expected.getKpiName());
            }
        }

        if (!missingKpis.isEmpty()) {
            errors.add("kpiInsights coverage is incomplete: missing " + missingKpis.size() + " KPI(s) -> " + String.join(", ", missingKpis));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
