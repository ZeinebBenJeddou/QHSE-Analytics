package com.QHSEAnalytics.analytics.service.processing;

import com.QHSEAnalytics.shared.dto.response.AiAnalysisStructuredResponse;
import com.QHSEAnalytics.shared.dto.response.AiConfidenceResponse;
import com.QHSEAnalytics.shared.dto.response.AiKpiInsightResponse;
import com.QHSEAnalytics.shared.dto.response.AiTraceabilityResponse;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
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

        return errors;
    }


    private static final Set<String> VALID_URGENCY_VALUES = Set.of("HIGH", "MEDIUM", "LOW");

    private void validateTraceability(AiTraceabilityResponse trace, List<String> errors) {
        // modelName, generatedAt, schemaVersion, promptVersion sont injectés côté serveur
        // après la validation du chunk — leur présence est garantie par enrichStructuredResponse().
        // La vérification de la présence de l'objet est effectuée par l'appelant.
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
                log.warn("[AI Validation] {} references unknown kpiId={} — skipping id validation", prefix, insight.getKpiId());
            }
            if (isBlank(insight.getKpiName())) {
                errors.add(prefix + ".kpiName is mandatory.");
            } else {
                String normalizedName = TextNormalizer.normalizeForMatching(insight.getKpiName());
                if (!knownNames.isEmpty() && !knownNames.contains(normalizedName)) {
                    log.warn("[AI Validation] {} references unknown kpiName='{}' — skipping name validation", prefix, insight.getKpiName());
                }
                coveredNames.add(normalizedName);
            }
            if (insight.getKpiId() != null) {
                coveredIds.add(String.valueOf(insight.getKpiId()));
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
            } else if (!VALID_URGENCY_VALUES.contains(insight.getUrgency())) {
                errors.add(prefix + ".urgency must be one of: HIGH, MEDIUM, LOW.");
            }
            if (isBlank(insight.getOwnerRole())) {
                errors.add(prefix + ".ownerRole is mandatory.");
            }
            if (isBlank(insight.getDueHorizon())) {
                errors.add(prefix + ".dueHorizon is mandatory.");
            }
            if (isBlank(insight.getSuccessMetric())) {
                log.debug("[Validation] {} successMetric absent — ignoré", prefix);
            }
            if (isBlank(insight.getRiskIfNotDone())) {
                log.debug("[Validation] {} riskIfNotDone absent — ignoré", prefix);
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

    public void sanitize(AiAnalysisStructuredResponse response) {
        if (response == null) return;

        response.setGlobalSummary(truncate(response.getGlobalSummary(), 3000));


        if (response.getProbableCauses() == null) response.setProbableCauses(new ArrayList<>());
        if (response.getRecommendations() == null) response.setRecommendations(new ArrayList<>());
        if (response.getActionPlan() == null) response.setActionPlan(new ArrayList<>());
        if (response.getTraceability() == null) {
            response.setTraceability(AiTraceabilityResponse.builder().build());
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
                insight.setInsight(truncate(insight.getInsight(), 3000));
                insight.setActionImmediate(truncate(insight.getActionImmediate(), 800));
                insight.setSuccessMetric(truncate(insight.getSuccessMetric(), 500));
                insight.setRiskIfNotDone(truncate(insight.getRiskIfNotDone(), 500));
                insight.setNote(truncate(insight.getNote(), 800));

                String kpiName = isBlank(insight.getKpiName()) ? "cet indicateur" : insight.getKpiName().trim();
                if (isBlank(insight.getInsight()) || insight.getInsight().length() < 20) {
                    insight.setInsight("Variation de " + kpiName + " nécessite une analyse approfondie.");
                }
                if (isBlank(insight.getActionImmediate()) || insight.getActionImmediate().length() < 15) {
                    insight.setActionImmediate("Planifier une revue de cet indicateur avec le responsable concerné.");
                }
                if (isBlank(insight.getUrgency())) {
                    insight.setUrgency("MEDIUM");
                }
                if (isBlank(insight.getOwnerRole())) {
                    String kpiNameLower = insight.getKpiName() != null
                            ? insight.getKpiName().toLowerCase() : "";
                    String defaultOwner;
                    if (kpiNameLower.contains("accident")
                            || kpiNameLower.contains("frequence")
                            || kpiNameLower.contains("gravite")
                            || kpiNameLower.contains("securite")
                            || kpiNameLower.contains("epi")
                            || kpiNameLower.contains("incident")
                            || kpiNameLower.contains("absenteisme")
                            || kpiNameLower.contains("maladie")) {
                        defaultOwner = "Responsable HSE";
                    } else if (kpiNameLower.contains("qualite")
                            || kpiNameLower.contains("conformite")
                            || kpiNameLower.contains("audit")
                            || kpiNameLower.contains("reclamation")
                            || kpiNameLower.contains("satisfaction")
                            || kpiNameLower.contains("yield")
                            || kpiNameLower.contains("rebus")
                            || kpiNameLower.contains("livraison")) {
                        defaultOwner = "Responsable Qualité";
                    } else if (kpiNameLower.contains("emission")
                            || kpiNameLower.contains("energie")
                            || kpiNameLower.contains("co2")
                            || kpiNameLower.contains("dechet")
                            || kpiNameLower.contains("environnement")
                            || kpiNameLower.contains("eau")
                            || kpiNameLower.contains("valorisation")) {
                        defaultOwner = "Responsable Environnement";
                    } else {
                        defaultOwner = "Responsable QHSE";
                    }
                    insight.setOwnerRole(defaultOwner);
                }
                if (isBlank(insight.getDueHorizon())) {
                    insight.setDueHorizon("1 mois");
                }
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
