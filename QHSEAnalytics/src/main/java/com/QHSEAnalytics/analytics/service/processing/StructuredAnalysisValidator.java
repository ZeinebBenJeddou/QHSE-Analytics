package com.QHSEAnalytics.analytics.service.processing;

import com.QHSEAnalytics.shared.dto.response.AiAnalysisStructuredResponse;
import com.QHSEAnalytics.shared.dto.response.AiConfidenceResponse;
import com.QHSEAnalytics.shared.dto.response.AiKpiInsightResponse;
import com.QHSEAnalytics.shared.dto.response.AiPredictiveAlertResponse;
import com.QHSEAnalytics.shared.dto.response.AiRootCauseResponse;
import com.QHSEAnalytics.shared.dto.response.AiTraceabilityResponse;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.analytics.service.TextNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class StructuredAnalysisValidator {

    private static final Set<String> VALID_SEVERITY = Set.of("LOW", "MEDIUM", "HIGH");

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
        // modelName and generatedAt are injected by enrichStructuredResponse after validation — not validated here
        if (trace.getContextSourcesUsed() == null) {
            trace.setContextSourcesUsed(new ArrayList<>());
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
            int total = availableKpis.size();
            int missing = missingKpis.size();
            double coverageRatio = (double)(total - missing) / total;
            if (coverageRatio < 0.5) {
                errors.add("kpiInsights coverage is incomplete: missing " + missing + " KPI(s) -> " + String.join(", ", missingKpis));
            } else {
                log.warn("[AI Validation] Partial KPI coverage {}/{} — missing: {}", total - missing, total, String.join(", ", missingKpis));
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Clamps numeric fields to valid ranges and truncates text fields in-place.
     * Called BEFORE validate() so out-of-range values are fixed rather than triggering a retry.
     */
    public void sanitize(AiAnalysisStructuredResponse response) {
        if (response == null) return;

        response.setGlobalSummary(truncate(response.getGlobalSummary(), 3000));

        // Initialize null required fields to prevent null-check failures in validate()
        if (response.getProbableCauses() == null) response.setProbableCauses(new ArrayList<>());
        if (response.getRecommendations() == null) response.setRecommendations(new ArrayList<>());
        if (response.getActionPlan() == null) response.setActionPlan(new ArrayList<>());
        if (response.getTraceability() == null) {
            response.setTraceability(AiTraceabilityResponse.builder().contextSourcesUsed(new ArrayList<>()).build());
        } else if (response.getTraceability().getContextSourcesUsed() == null) {
            response.getTraceability().setContextSourcesUsed(new ArrayList<>());
        }

        AiConfidenceResponse confidence = response.getConfidence();
        if (confidence != null) {
            if (confidence.getOverall() != null) {
                double clamped = clamp(confidence.getOverall(), 0, 100);
                if (Double.compare(clamped, confidence.getOverall()) != 0) {
                    log.warn("[AI Validation] Clamped confidence.overall {} → {}", confidence.getOverall(), clamped);
                    confidence.setOverall(clamped);
                }
            }
            if (confidence.getSections() != null) {
                confidence.getSections().replaceAll((k, v) -> v == null ? 0.0 : clamp(v, 0, 100));
            }
        }

        if (response.getKpiInsights() != null) {
            response.getKpiInsights().forEach(insight -> {
                if (insight == null) return;
                if (insight.getConfidence() != null) {
                    insight.setConfidence(clamp(insight.getConfidence(), 0, 100));
                }
                if (insight.getProbableCauses() == null) insight.setProbableCauses(new ArrayList<>());
                if (insight.getRecommendations() == null) insight.setRecommendations(new ArrayList<>());
                insight.setInsight(truncate(insight.getInsight(), 2000));
                insight.setActionImmediate(truncate(insight.getActionImmediate(), 500));
                insight.setSuccessMetric(truncate(insight.getSuccessMetric(), 500));
                insight.setRiskIfNotDone(truncate(insight.getRiskIfNotDone(), 500));
                insight.setNote(truncate(insight.getNote(), 500));
            });
        }

        if (response.getRecommendations() != null) {
            response.getRecommendations().forEach(rec -> {
                if (rec == null) return;
                rec.setTitle(truncate(rec.getTitle(), 200));
                rec.setRationale(truncate(rec.getRationale(), 1000));
                rec.setExpectedBenefit(truncate(rec.getExpectedBenefit(), 500));
            });
        }

        if (response.getPredictiveAlerts() != null) {
            response.getPredictiveAlerts().forEach(alert -> {
                if (alert == null) return;
                if (alert.getConfidence() != null) {
                    alert.setConfidence(clamp(alert.getConfidence(), 0, 100));
                }
                if (alert.getEstimatedHorizonMonths() != null) {
                    alert.setEstimatedHorizonMonths(Math.max(0, Math.min(24, alert.getEstimatedHorizonMonths())));
                }
                if (alert.getSeverity() != null) {
                    String upper = alert.getSeverity().toUpperCase();
                    if (!VALID_SEVERITY.contains(upper)) {
                        log.warn("[AI Validation] Invalid predictiveAlert severity '{}' normalized to MEDIUM", alert.getSeverity());
                        alert.setSeverity("MEDIUM");
                    } else {
                        alert.setSeverity(upper);
                    }
                }
                alert.setProjection(truncate(alert.getProjection(), 500));
            });
        }
    }

    private double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) + "…" : value;
    }
}
