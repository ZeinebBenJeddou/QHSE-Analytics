package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.ollama.AiResponse;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.entity.AnalyseGlobale;
import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.repository.KpiRepository;
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
    private final GeminiClientService geminiClientService;
    private final KpiRepository kpiRepository;
    private final AnalyseGlobaleRepository analyseGlobaleRepository;

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
        log.info("Lancement analyse IA (Gemini ou Ollama) sur {} KPIs", topKpis.size());
        
        if (geminiClientService.isConfigured()) {
            AiResponse geminiResponse = geminiClientService.generateAnalysis(prompt);
            if (geminiResponse != null) {
                return geminiResponse;
            }
            log.warn("Gemini a échoué, repli vers Ollama...");
        }
        
        return ollamaClientService.generateStrictAiResponseObject(prompt);
    }

    private String buildPrompt(List<KpiCalculatedDTO> data) {
        StringBuilder prompt = new StringBuilder();
        
        // RAG Context: All indicators (Definitions & Thresholds)
        List<Kpi> allKpis = kpiRepository.findByIsActiveTrueOrderByOrdreAsc();
        prompt.append("=== CONTEXTE MÉTIER : TOUS LES INDICATEURS ===\n");
        allKpis.forEach(k -> prompt.append(String.format(
            "Indicateur: %s | Définition: %s | Seuils: Faible<%s, Modéré<%s, Critique<%s\n",
            k.getNom(), k.getDefinition(), k.getSeuilFaible(), k.getSeuilModere(), k.getSeuilCritique()
        )));
        prompt.append("\n");

        // RAG Context: Recent Action Plans
        List<AnalyseGlobale> recentAnalyses = analyseGlobaleRepository.findTop10ByOrderByCreatedAtDesc();
        prompt.append("=== HISTORIQUE : PLANS D'ACTIONS RÉCENTS ===\n");
        recentAnalyses.forEach(a -> prompt.append(String.format(
            "Analyse du %s : %s\n", a.getCreatedAt(), a.getPlanActions()
        )));
        prompt.append("\n");

        prompt.append("You are a senior QHSE data analyst AI expert in ISO 9001, ISO 14001, and ISO 45001.\n\n");
        prompt.append("Analyze the provided current results (N vs N-1) by cross-referencing them with the full indicator list and historical action plans provided above.\n");
        prompt.append("Your goal is to identify trends, potential risks, and ensure compliance with ISO standards.\n\n");

        prompt.append("=== DONNÉES KPI ACTUELLES (N vs N-1) ===\n");
        data.forEach(kpi -> prompt.append(String.format(
                "KPI: %s | Categorie: %s | N-1=%s | N=%s | Δ=%s%% | Classification=%s\n",
                safe(kpi.getKpiName()),
                safe(kpi.getCategorie()),
                safeNumber(kpi.getValeurN1()),
                safeNumber(kpi.getValeurN()),
                safeNumber(kpi.getVariationPercentage()),
                safe(kpi.getClassification())
        )));
        prompt.append("\n");

        prompt.append("=== INSTRUCTIONS STRICTES ===\n");
        prompt.append("1. NIVEAU D'ANALYSE : Ton analyse doit être extrêmement détaillée (ISO compliance).\n");
        prompt.append("2. PLANS D'ACTIONS IMMÉDIATS : Fournis des actions correctives concrètes pour chaque risque détecté.\n");
        prompt.append("3. RAG : Utilise les seuils et l'historique fournis pour contextualiser chaque variation.\n");
        prompt.append("Réponds uniquement avec du JSON valide.\n");
        prompt.append("Format attendu :\n");
        prompt.append("{\n");
        prompt.append("  \"overallScore\": number,\n");
        prompt.append("  \"summary\": \"Synthèse globale détaillée avec niveau d'analyse QHSE\",\n");
        prompt.append("  \"kpis\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": string,\n");
        prompt.append("      \"score\": number,\n");
        prompt.append("      \"insight\": \"Analyse détaillée + Plan d'action immédiat spécifique\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"recommendations\": [\"Action immédiate 1\", \"Action corrective 2\"]\n");
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
