package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.ollama.AiResponse;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisAgent {

    private final OllamaClientService ollamaClientService;

    public String analyze(List<KpiCalculatedDTO> calculatedData) {
        if (calculatedData == null || calculatedData.isEmpty()) {
            return "Aucune donnée KPI disponible pour l'analyse.";
        }
        AiResponse aiResponse = analyzeStrict(calculatedData);
        if (aiResponse == null) {
            return "Analyse IA temporairement indisponible.";
        }
        return aiResponse.getSummary();
    }

    public AiResponse analyzeStrict(List<KpiCalculatedDTO> calculatedData) {
        if (calculatedData == null || calculatedData.isEmpty()) {
            return AiResponse.builder()
                    .overallScore(0.0)
                    .summary("Aucune donnée KPI disponible pour l'analyse.")
                    .kpis(List.of())
                    .recommendations(List.of())
                    .build();
        }

        List<KpiCalculatedDTO> topKpis = calculatedData.stream()
                .filter(k -> k.getVariationPercentage() != null)
                .sorted(Comparator.comparingDouble(k -> -Math.abs(k.getVariationPercentage())))
                .limit(20)
                .collect(Collectors.toList());

        String prompt = buildPrompt(topKpis);
        log.info("Lancement analyse IA stricte sur {} KPIs", topKpis.size());
        return ollamaClientService.generateStrictAiResponseObject(prompt);
    }

    private String buildPrompt(List<KpiCalculatedDTO> data) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un expert QHSE certifié ISO 9001/14001/45001.\n");
        prompt.append("Analyse les indicateurs de performance QHSE suivants et fournis une réponse strictement JSON.\n\n");

        prompt.append("=== DONNÉES KPI SÉLECTIONNÉES ===\n");
        data.forEach(kpi -> prompt.append(String.format(
                "KPI: %s | Catégorie: %s | N-1=%s | N=%s | Δ=%s%% | Classification=%s | Tendance=%s\n",
                safe(kpi.getKpiName()),
                safe(kpi.getCategorie()),
                safeNumber(kpi.getValeurN1()),
                safeNumber(kpi.getValeurN()),
                safeNumber(kpi.getVariationPercentage()),
                safe(kpi.getClassification()),
                safe(kpi.getTendance())
        )));
        prompt.append("\n");

        prompt.append("=== INSTRUCTIONS STRICTES ===\n");
        prompt.append("Réponds uniquement avec du JSON valide, sans markdown, sans explication, sans texte avant ou après le JSON.\n");
        prompt.append("Format attendu :\n");
        prompt.append("{\n");
        prompt.append("  \"overallScore\": number,\n");
        prompt.append("  \"summary\": string,\n");
        prompt.append("  \"kpis\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": string,\n");
        prompt.append("      \"score\": number,\n");
        prompt.append("      \"insight\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"recommendations\": [string]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    private String safeNumber(Number value) {
        if (value == null) {
            return "0";
        }
        double d = value.doubleValue();
        return d == Math.floor(d) && !Double.isInfinite(d)
                ? String.valueOf((long) d)
                : String.format("%.2f", d);
    }
}
