package com.QHSEAnalytics.analytics.service.processing;

import com.QHSEAnalytics.shared.dto.response.AiContextSourceResponse;
import com.QHSEAnalytics.shared.dto.response.AiTraceabilityResponse;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.analytics.service.TextNormalizer;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StructuredAnalysisPromptBuilder {

    public String buildPrompt(List<KpiCalculatedDTO> kpis) {
        if (kpis == null || kpis.isEmpty()) {
            throw new IllegalArgumentException("At least one KPI is required to build a structured IA prompt.");
        }

        List<KpiCalculatedDTO> orderedKpis = kpis.stream()
                .sorted(Comparator.comparingDouble(k -> -Math.abs(k.getVariationPercentage() == null ? 0.0 : k.getVariationPercentage())))
                .collect(Collectors.toList());

        StringBuilder prompt = new StringBuilder();
        prompt.append("SYSTEM:\n");
        prompt.append("You are a senior QHSE data analyst AI expert with deep knowledge of ISO 9001, ISO 14001, ISO 45001 and regulatory compliance.\n");
        prompt.append("Your task is to produce a strictly formatted JSON analysis for the current import data.\n");
        prompt.append("You MUST not invent numbers, dates, or indicators. Use only data provided below.\n");
        prompt.append("Cite the KPI references that support each conclusion.\n");
        prompt.append(String.format("You MUST produce exactly one kpiInsights entry for each provided KPI. Expected KPI insights: %d.\n", orderedKpis.size()));
        prompt.append("ALL output must be valid JSON only, without markdown, code fences, or explanatory text.\n\n");

        prompt.append("USER:\n");
        prompt.append("You have the following targeted context sources and KPI results.\n");
        prompt.append("Use the provided sources to answer. Do not use any external knowledge.\n\n");

        prompt.append("=== KPI CONTEXT & RAG SOURCES ===\n");
        for (KpiCalculatedDTO kpi : orderedKpis) {
            prompt.append(String.format("sourceId: kpi_def_%s | sourceName: KPI definition for %s | relevanceScore: 0.85\n",
                safeId(kpi.getMatchedKpiId(), kpi.getKpiName()), safeText(kpi.getKpiName())));
            prompt.append(String.format("KPI: %s | Categorie: %s | Definition: %s | Seuils: Faible<%s, Modere<%s, Critique<%s | N-1=%s | N=%s | Variation=%s%% | Classification=%s\n",
                    safeText(kpi.getKpiName()), safeText(kpi.getCategorie()), safeText(kpi.getDefinition()),
                    safeNumber(kpi.getSeuilFaible()), safeNumber(kpi.getSeuilModere()), safeNumber(kpi.getSeuilCritique()),
                    safeNumber(kpi.getValeurN1()), safeNumber(kpi.getValeurN()), safeNumber(kpi.getVariationPercentage()),
                    safeText(kpi.getClassification())));
            prompt.append("\n");
        }

        prompt.append("=== INSTRUCTIONS DE SORTIE ===\n");
        prompt.append("Return a single JSON object with the following structure exactly.\n");
        prompt.append("Do not include extra fields outside the defined schema.\n");
        prompt.append("Do not use markdown or backticks.\n");
        prompt.append("If a field cannot be determined from the provided data, return an empty string or an empty array, but do not fabricate a value.\n");
        prompt.append("Do not omit any provided KPI: every KPI must appear in kpiInsights, even if the insight is a concise justification based on the provided data.\n");
        prompt.append("Cite the KPI names or ids that justify each recommendation, cause, and action.\n");
        prompt.append("Confidence values should be numeric percentages between 0 and 100.\n\n");

        prompt.append("OUTPUT SCHEMA:\n");
        prompt.append("{\n");
        prompt.append("  \"globalSummary\": string,\n");
        prompt.append("  \"confidence\": {\n");
        prompt.append("    \"overall\": number,\n");
        prompt.append("    \"sections\": {\n");
        prompt.append("      \"summary\": number,\n");
        prompt.append("      \"probableCauses\": number,\n");
        prompt.append("      \"recommendations\": number,\n");
        prompt.append("      \"actionPlan\": number\n");
        prompt.append("    }\n");
        prompt.append("  },\n");
        prompt.append("  \"kpiInsights\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"kpiId\": number,\n");
        prompt.append("      \"kpiName\": string,\n");
        prompt.append("      \"confidence\": number,\n");
        prompt.append("      \"insight\": string,\n");
        prompt.append("      \"probableCauses\": [string],\n");
        prompt.append("      \"recommendations\": [string],\n");
        prompt.append("      \"actionImmediate\": string,\n");
        prompt.append("      \"urgency\": string,\n");
        prompt.append("      \"ownerRole\": string,\n");
        prompt.append("      \"dueHorizon\": string,\n");
        prompt.append("      \"successMetric\": string,\n");
        prompt.append("      \"riskIfNotDone\": string,\n");
        prompt.append("      \"note\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"probableCauses\": [string],\n");
        prompt.append("  \"recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": string,\n");
        prompt.append("      \"rationale\": string,\n");
        prompt.append("      \"expectedBenefit\": string,\n");
        prompt.append("      \"urgency\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"actionPlan\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"action\": string,\n");
        prompt.append("      \"priority\": string,\n");
        prompt.append("      \"ownerRole\": string,\n");
        prompt.append("      \"dueHorizon\": string,\n");
        prompt.append("      \"successMetric\": string,\n");
        prompt.append("      \"riskIfNotDone\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"rootCauseAnalysis\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"kpiRef\": string,\n");
        prompt.append("      \"method\": \"5_whys\",\n");
        prompt.append("      \"whyChain\": [string],\n");
        prompt.append("      \"ishikawaCategory\": string,\n");
        prompt.append("      \"rootCause\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"predictiveAlerts\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"kpiRef\": string,\n");
        prompt.append("      \"projection\": string,\n");
        prompt.append("      \"estimatedHorizonMonths\": number,\n");
        prompt.append("      \"confidence\": number,\n");
        prompt.append("      \"severity\": \"LOW|MEDIUM|HIGH\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"traceability\": {\n");
        prompt.append("    \"modelName\": string,\n");
        prompt.append("    \"generatedAt\": string,\n");
        prompt.append("    \"contextSourcesUsed\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"sourceId\": string,\n");
        prompt.append("        \"sourceName\": string,\n");
        prompt.append("        \"relevanceScore\": number\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");

        prompt.append("Make sure the output is valid JSON.\n");
        prompt.append("If you cannot provide a structured section, return an empty array/object for it instead of text.\n");
        prompt.append("\n=== INSTRUCTIONS SPÉCIFIQUES rootCauseAnalysis ===\n");
        prompt.append("For each CRITIQUE or MODERE KPI, produce exactly one rootCauseAnalysis entry.\n");
        prompt.append("whyChain must contain 3 to 5 successive 'Why' questions leading to the root cause.\n");
        prompt.append("ishikawaCategory must be one of: Homme, Machine, Méthode, Milieu, Matière.\n");
        prompt.append("rootCause must be a single concise sentence identifying the fundamental cause.\n");
        prompt.append("\n=== INSTRUCTIONS SPÉCIFIQUES predictiveAlerts ===\n");
        prompt.append("For each KPI showing a deteriorating trend (trendDirection DETERIORATING or 2+ consecutive negative variations), produce a predictiveAlert.\n");
        prompt.append("projection must describe what will happen if the trend continues (e.g. 'Si la tendance continue, le KPI X atteindra le seuil critique dans environ N mois').\n");
        prompt.append("estimatedHorizonMonths must be an integer between 1 and 24. Use null if trend is stable or improving.\n");
        prompt.append("severity must be LOW (horizon > 12 months), MEDIUM (6-12 months) or HIGH (< 6 months).\n");
        prompt.append("confidence must be a number between 0 and 100.\n");

        prompt.append("=== ADDITIONAL CONTEXT SOURCES ===\n");
        prompt.append("sourceId: historical_actions | sourceName: recent action plans | relevanceScore: 0.65\n");
        prompt.append("Analyze current KPI variations against the most recent action plans provided.\n");
        prompt.append("\n");

        prompt.append("Remember: the structured JSON is the single source of truth. Do not output other prose.\n");

        return prompt.toString();
    }

    public String buildRetryPrompt(String originalPrompt, String validationErrors) {
        StringBuilder retryPrompt = new StringBuilder(originalPrompt);
        retryPrompt.append("\n\nPREVIOUS RESPONSE WAS INVALID.\n");
        retryPrompt.append("Validation errors: ").append(safeText(validationErrors)).append(".\n");
        retryPrompt.append("Please regenerate the JSON with the exact same schema and fix the errors above.\n");
        retryPrompt.append("Respond only with valid JSON.\n");
        return retryPrompt.toString();
    }

    public String getPromptVersion() {
        return "structured-qhse-v5";
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        String normalized = TextNormalizer.normalizeForPrompt(value);
        return normalized.length() > 1000 ? normalized.substring(0, 1000) + "..." : normalized;
    }

    private String safeNumber(Number value) {
        if (value == null) return "0";
        return value.toString();
    }

    private String safeId(Long id, String name) {
        if (id != null) {
            return String.valueOf(id);
        }
        String safeName = TextNormalizer.normalizeForSearch(name);
        return safeName.isBlank() ? "unknown" : safeName.replaceAll("\\s+", "_");
    }
}
