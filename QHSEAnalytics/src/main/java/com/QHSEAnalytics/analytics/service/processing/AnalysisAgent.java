package com.QHSEAnalytics.analytics.service.processing;

import com.QHSEAnalytics.shared.dto.llm.AiResponse;
import com.QHSEAnalytics.shared.dto.llm.KpiInsight;
import com.QHSEAnalytics.shared.dto.response.AiAnalysisStructuredResponse;
import com.QHSEAnalytics.shared.dto.response.AiConfidenceResponse;
import com.QHSEAnalytics.shared.dto.response.AiTraceabilityResponse;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.entity.AnalyseGlobale;
import com.QHSEAnalytics.shared.entity.RagKnowledge;
import com.QHSEAnalytics.shared.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import com.QHSEAnalytics.analytics.service.LlmProviderChain;
import com.QHSEAnalytics.analytics.service.RagSearchService;
import com.QHSEAnalytics.analytics.service.TextNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisAgent {

    private final LlmProviderChain llmProviderChain;
    private final KpiRepository kpiRepository;
    private final AnalyseGlobaleRepository analyseGlobaleRepository;
    private final ObjectMapper objectMapper;
    private final StructuredAnalysisPromptBuilder structuredAnalysisPromptBuilder;
    private final StructuredAnalysisValidator structuredAnalysisValidator;
    private final MeterRegistry meterRegistry;
    private final PromptSanitizer promptSanitizer;
    private final RagSearchService ragSearchService;

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
        log.info("Lancement analyse IA (Groq puis Gemini) sur {} KPIs", topKpis.size());

        String responseJson = llmProviderChain.generate(prompt);
        if (responseJson != null && !responseJson.isBlank()) {
            AiResponse parsedResponse = parseAiResponse(responseJson);
            if (parsedResponse != null) {
                return parsedResponse;
            }
            log.warn("Le JSON du provider LLM était invalide, retour de secours...");
        }

        return AiResponse.builder()
                .overallScore(0.0)
                .summary("IA indisponible")
                .kpis(List.of())
                .recommendations(List.of())
                .build();
    }

    public AiAnalysisStructuredResponse analyzeStructured(List<KpiCalculatedDTO> calculatedData) {
        return analyzeStructured(calculatedData, null);
    }

    public AiAnalysisStructuredResponse analyzeStructured(List<KpiCalculatedDTO> calculatedData, Long importSessionId) {
        return analyzeStructured(calculatedData, importSessionId, false);
    }

    public AiAnalysisStructuredResponse analyzeStructured(List<KpiCalculatedDTO> calculatedData, Long importSessionId, boolean bypassCache) {
        if (calculatedData == null || calculatedData.isEmpty()) {
            return buildStructuredFallback("FAILED", "Aucune donnée KPI disponible pour l'analyse structurée.");
        }

        List<KpiCalculatedDTO> topKpis = calculatedData.stream()
                .filter(k -> k.getVariationPercentage() != null)
                .sorted(Comparator.comparingDouble(k -> -Math.abs(k.getVariationPercentage())))
                .limit(20)
                .collect(Collectors.toList());

        String prompt = structuredAnalysisPromptBuilder.buildPrompt(topKpis);
        String cacheKeyPrefix = String.format("importSessionId=%s|mode=structured|promptVersion=%s|schemaVersion=%s|providerChainVersion=%s",
                importSessionId == null ? "unknown" : importSessionId,
                structuredAnalysisPromptBuilder.getPromptVersion(),
            "1.2",
            LlmProviderChain.PROVIDER_CHAIN_VERSION);
        log.info("[AnalysisAgent] analyseStructured: starting with {} KPIs, importSessionId={}, cachePrefix={}, bypassCache={}", topKpis.size(), importSessionId, cacheKeyPrefix, bypassCache);

        long start = System.currentTimeMillis();
        LlmProviderChain.ProviderResult providerResult = llmProviderChain.generate(prompt, cacheKeyPrefix, bypassCache);
        long latency = System.currentTimeMillis() - start;
        String providerUsed = providerResult == null ? "none" : providerResult.provider();
        String responseJson = providerResult == null ? null : providerResult.response();
        log.info("[AnalysisAgent] analyseStructured: provider response received in {}ms, importSessionId={}, provider={}, cachePrefix={}", latency, importSessionId, providerUsed, cacheKeyPrefix);

        if (responseJson == null || responseJson.isBlank()) {
            log.warn("[AnalysisAgent] analyseStructured: empty response from provider. reason=empty_response, latency={}ms, provider={}", latency, providerUsed);
            incrementCounter("ai.request.count", Tags.of("mode", "structured", "status", "failed"));
            recordLatency("ai.latency.ms", Tags.of("mode", "structured"), latency);
            return buildStructuredFallback("FAILED", "Aucune réponse du provider IA.");
        }

        AiAnalysisStructuredResponse response = parseStructuredResponse(responseJson);
        if (response != null) {
            structuredAnalysisValidator.sanitize(response);
            var errors = structuredAnalysisValidator.validate(response, topKpis);
            int missingCount = computeMissingKpiCount(topKpis, response.getKpiInsights());
            log.info("[AnalysisAgent] analyseStructured: coverage total_input_kpis={} total_output_insights={} missing_kpis_count={} importSessionId={}",
                    topKpis.size(), response.getKpiInsights() == null ? 0 : response.getKpiInsights().size(), missingCount, importSessionId);
            if (errors.isEmpty()) {
                enrichStructuredResponse(response, providerUsed, importSessionId);
                response.setStatus("SUCCESS");
                incrementCounter("ai.request.count", Tags.of("mode", "structured", "status", "success"));
                recordLatency("ai.latency.ms", Tags.of("mode", "structured"), latency);
                return response;
            }

            log.warn("[AnalysisAgent] analyseStructured: validation failed. reason=validation_errors, error_count={}, errors={}, latency={}ms", errors.size(), errors, latency);
            String retryPrompt = structuredAnalysisPromptBuilder.buildRetryPrompt(prompt, String.join("; ", errors));
            log.info("[AnalysisAgent] analyseStructured: retry attempt initiated. retry_count=1");
            LlmProviderChain.ProviderResult retryResult = llmProviderChain.generate(retryPrompt, cacheKeyPrefix, bypassCache);
            AiAnalysisStructuredResponse retryResponse = parseStructuredResponse(retryResult == null ? null : retryResult.response());
            if (retryResponse != null) {
                structuredAnalysisValidator.sanitize(retryResponse);
                var retryErrors = structuredAnalysisValidator.validate(retryResponse, topKpis);
                int retryMissingCount = computeMissingKpiCount(topKpis, retryResponse.getKpiInsights());
                log.info("[AnalysisAgent] analyseStructured: retry coverage total_input_kpis={} total_output_insights={} missing_kpis_count={} importSessionId={}",
                        topKpis.size(), retryResponse.getKpiInsights() == null ? 0 : retryResponse.getKpiInsights().size(), retryMissingCount, importSessionId);
                if (retryErrors.isEmpty()) {
                    enrichStructuredResponse(retryResponse, retryResult == null ? providerUsed : retryResult.provider(), importSessionId);
                    retryResponse.setStatus("SUCCESS");
                    incrementCounter("ai.request.count", Tags.of("mode", "structured", "status", "success"));
                    incrementCounter("ai.retry.count", Tags.of("mode", "structured"));
                    recordLatency("ai.latency.ms", Tags.of("mode", "structured"), latency);
                    return retryResponse;
                }
                log.warn("[AnalysisAgent] analyseStructured: retry validation failed. reason=retry_validation_errors, error_count={}, errors={}, retry_count=1, latency={}ms", retryErrors.size(), retryErrors, latency);
                incrementCounter("ai.request.count", Tags.of("mode", "structured", "status", "partial"));
                incrementCounter("ai.retry.count", Tags.of("mode", "structured"));
                incrementCounter("ai.validation.error.count", Tags.of("mode", "structured"));
                recordLatency("ai.latency.ms", Tags.of("mode", "structured"), latency);
                return buildStructuredFallback("PARTIAL", "Réponse IA invalide après retry : " + String.join("; ", retryErrors));
            }
            log.warn("[AnalysisAgent] analyseStructured: retry parsing failed. reason=retry_parsing_failed, retry_count=1, latency={}ms", latency);
            incrementCounter("ai.request.count", Tags.of("mode", "structured", "status", "partial"));
            incrementCounter("ai.retry.count", Tags.of("mode", "structured"));
            incrementCounter("ai.parse.error.count", Tags.of("mode", "structured"));
            recordLatency("ai.latency.ms", Tags.of("mode", "structured"), latency);
            return buildStructuredFallback("PARTIAL", "Impossible de parser la réponse IA après retry.");
        }

        log.warn("[AnalysisAgent] analyseStructured: initial parsing failed. reason=initial_parsing_failed, latency={}ms", latency);
        incrementCounter("ai.request.count", Tags.of("mode", "structured", "status", "failed"));
        incrementCounter("ai.parse.error.count", Tags.of("mode", "structured"));
        recordLatency("ai.latency.ms", Tags.of("mode", "structured"), latency);
        return buildStructuredFallback("FAILED", "Impossible de parser la réponse IA.");
    }

    private int computeMissingKpiCount(List<KpiCalculatedDTO> availableKpis, List<? extends Object> insights) {
        if (availableKpis == null || availableKpis.isEmpty() || insights == null) {
            return availableKpis == null ? 0 : availableKpis.size();
        }
        Set<String> coveredIds = new HashSet<>();
        Set<String> coveredNames = new HashSet<>();
        insights.forEach(item -> {
            if (item instanceof com.QHSEAnalytics.shared.dto.response.AiKpiInsightResponse insight) {
                if (insight.getKpiId() != null) {
                    coveredIds.add(String.valueOf(insight.getKpiId()));
                }
                String normalizedName = TextNormalizer.normalizeForMatching(insight.getKpiName());
                if (!normalizedName.isBlank()) {
                    coveredNames.add(normalizedName);
                }
            }
        });

        return (int) availableKpis.stream()
                .filter(kpi -> {
                    String expectedId = kpi.getMatchedKpiId() == null ? null : String.valueOf(kpi.getMatchedKpiId());
                    String expectedName = TextNormalizer.normalizeForMatching(kpi.getKpiName());
                    boolean matchedById = expectedId != null && coveredIds.contains(expectedId);
                    boolean matchedByName = !expectedName.isBlank() && coveredNames.contains(expectedName);
                    return !matchedById && !matchedByName;
                })
                .count();
    }

    private void enrichStructuredResponse(AiAnalysisStructuredResponse response, String provider, Long importSessionId) {
        if (response == null) {
            return;
        }
        if (response.getTraceability() == null) {
            response.setTraceability(AiTraceabilityResponse.builder()
                    .contextSourcesUsed(List.of())
                    .build());
        }
        String modelName = provider == null || provider.isBlank() || "none".equalsIgnoreCase(provider)
                ? "fallback"
                : provider;
        response.getTraceability().setModelName(modelName);
        response.getTraceability().setGeneratedAt(OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.getTraceability().setSchemaVersion("1.2");
        response.getTraceability().setPromptVersion(structuredAnalysisPromptBuilder.getPromptVersion());
        response.getTraceability().setImportSessionId(importSessionId);
        response.setSchemaVersion("1.2");
        response.setPromptVersion(structuredAnalysisPromptBuilder.getPromptVersion());
        response.setImportSessionId(importSessionId);
    }

    private AiAnalysisStructuredResponse parseStructuredResponse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        try {
            String cleaned = rawJson.trim();
            if (cleaned.startsWith("```") || cleaned.startsWith("~~~")) {
                cleaned = cleaned.replaceAll("^(```|~~~)[^\n]*\\n", "").replaceAll("(```|~~~)$", "").trim();
            }
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end >= start) {
                cleaned = cleaned.substring(start, end + 1).trim();
            }

            return objectMapper.readValue(cleaned, AiAnalysisStructuredResponse.class);
        } catch (Exception ex) {
            log.warn("Échec parsing réponse structurée IA : {}", ex.getMessage());
            return null;
        }
    }

    private AiAnalysisStructuredResponse buildStructuredFallback(String status, String fallbackReason) {
        return AiAnalysisStructuredResponse.builder()
                .globalSummary("")
                .confidence(AiConfidenceResponse.builder()
                        .overall(0.0)
                        .sections(Map.of(
                                "summary", 0.0,
                                "probableCauses", 0.0,
                                "recommendations", 0.0,
                                "actionPlan", 0.0
                        ))
                        .build())
                .kpiInsights(List.of())
                .probableCauses(List.of())
                .recommendations(List.of())
                .actionPlan(List.of())
                .traceability(AiTraceabilityResponse.builder()
                        .modelName("fallback")
                        .generatedAt(OffsetDateTime.now(ZoneOffset.UTC).toString())
                        .contextSourcesUsed(List.of())
                    .schemaVersion("1.1")
                    .promptVersion(structuredAnalysisPromptBuilder.getPromptVersion())
                    .importSessionId(null)
                        .build())
                .status(status)
                .fallbackReason(fallbackReason)
                .schemaVersion("1.1")
                .promptVersion(structuredAnalysisPromptBuilder.getPromptVersion())
                .build();
    }

    private void incrementCounter(String name, Tags tags) {
        if (meterRegistry == null || tags == null) {
            return;
        }
        var counter = meterRegistry.counter(name, tags);
        if (counter != null) {
            counter.increment();
        }
    }

    private void recordLatency(String name, Tags tags, long latencyMs) {
        if (meterRegistry == null || tags == null) {
            return;
        }
        var timer = meterRegistry.timer(name, tags);
        if (timer != null) {
            timer.record(latencyMs, TimeUnit.MILLISECONDS);
        }
    }

    private AiResponse parseAiResponse(String jsonText) {
        try {
            String cleaned = jsonText == null ? null : jsonText.trim();
            if (cleaned != null && cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```[a-zA-Z]*\\n?", "").replaceAll("```", "").trim();
            }
            if (cleaned != null) {
                int start = cleaned.indexOf('{');
                int end = cleaned.lastIndexOf('}');
                if (start >= 0 && end >= start) {
                    cleaned = cleaned.substring(start, end + 1);
                }
            }

            JsonNode root = objectMapper.readTree(cleaned);

            AiResponse response = new AiResponse();

            // Valider le score global (doit être dans [0, 100])
            double rawScore = root.path("overallScore").asDouble(0.0);
            response.setOverallScore(Double.isNaN(rawScore) || Double.isInfinite(rawScore)
                    ? 0.0 : Math.max(0.0, Math.min(100.0, rawScore)));

            String summary = root.path("summary").asText("Analyse indisponible");
            // Limiter la taille de la synthèse pour éviter les hallucinations trop longues
            if (summary.length() > 3000) {
                summary = summary.substring(0, 3000) + "…";
            }
            response.setSummary(summary);

            List<String> recs = new java.util.ArrayList<>();
            root.path("recommendations").forEach(n -> recs.add(n.asText()));
            response.setRecommendations(recs);

            List<KpiInsight> kpis = new java.util.ArrayList<>();
            root.path("kpis").forEach(n -> {
                KpiInsight insight = new KpiInsight();
                String name = n.path("name").asText();
                if (name.isBlank()) {
                    name = n.path("kpiName").asText();
                }
                if (name.isBlank()) {
                    name = n.path("kpi").asText();
                }
                insight.setName(name);

                double kpiRawScore = n.path("score").isNumber() ? n.path("score").asDouble() : n.path("rating").asDouble(0.0);
                double score = (Double.isNaN(kpiRawScore) || Double.isInfinite(kpiRawScore)) ? 0.0 : Math.max(0.0, Math.min(100.0, kpiRawScore));
                insight.setScore(score);

                String rawInsight = n.path("insight").asText();
                if (rawInsight.isBlank()) {
                    rawInsight = n.path("analysis").asText();
                }
                if (rawInsight.isBlank()) {
                    rawInsight = n.path("note").asText();
                }
                insight.setInsight(rawInsight.isBlank() ? null : rawInsight);

                String aiNote = n.path("aiNote").asText();
                if (aiNote.isBlank()) {
                    aiNote = n.path("noteFinale").asText();
                }
                if (aiNote.isBlank()) {
                    aiNote = n.path("note").asText();
                }
                if (aiNote.isBlank()) {
                    aiNote = n.path("analysis").asText();
                }
                if (aiNote.isBlank()) {
                    aiNote = rawInsight;
                }
                insight.setAiNote(aiNote.isBlank() ? null : aiNote);

                String identificationRisque = n.path("identificationRisque").asText();
                if (identificationRisque.isBlank()) {
                    identificationRisque = n.path("riskJustification").asText();
                }
                insight.setIdentificationRisque(identificationRisque.isBlank() ? null : identificationRisque);
                insight.setRiskJustification(n.path("riskJustification").asText());

                String problemeDetecte = n.path("problemeDetecte").asText();
                if (problemeDetecte.isBlank()) {
                    problemeDetecte = n.path("issueDetected").asText();
                }
                insight.setProblemeDetecte(problemeDetecte.isBlank() ? null : problemeDetecte);
                insight.setIssueDetected(n.path("issueDetected").asText());

                String actionsPreventives = n.path("actionsPreventives").asText();
                if (actionsPreventives.isBlank()) {
                    actionsPreventives = n.path("preventiveAction").asText();
                }
                insight.setActionsPreventives(actionsPreventives.isBlank() ? null : actionsPreventives);
                insight.setPreventiveAction(n.path("preventiveAction").asText());

                insight.setActionImmediate(n.path("actionImmediate").asText());
                insight.setPrioriteAction(n.path("prioriteAction").asText());

                String methode8D = n.path("methode8D").asText();
                insight.setMethode8D(methode8D.isBlank() ? null : methode8D);

                String noteFinale = n.path("noteFinale").asText();
                if (noteFinale.isBlank()) {
                    noteFinale = n.path("insight").asText();
                }
                insight.setNoteFinale(noteFinale.isBlank() ? null : noteFinale);

                kpis.add(insight);
            });
            response.setKpis(kpis);

            return response;
        } catch (Exception e) {
            log.error("Erreur de parsing de la réponse Groq: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(List<KpiCalculatedDTO> data) {
        StringBuilder prompt = new StringBuilder();

        // RAG Context: vector similarity search on combined KPI names (one embedding call)
        String combinedQuery = data.stream()
                .map(k -> promptSanitizer.sanitize(k.getKpiName()))
                .collect(Collectors.joining(", "));
        List<RagKnowledge> ragResults = ragSearchService.findRelevant(combinedQuery, 10, null, 0.65);
        if (!ragResults.isEmpty()) {
            prompt.append("=== CONTEXTE MÉTIER QHSE (base de connaissances) ===\n");
            ragResults.forEach(r -> {
                prompt.append("• ").append(r.getKpiName()).append(": ").append(r.getDefinition());
                if (r.getThresholds() != null) {
                    prompt.append(" | Seuils: ").append(r.getThresholds());
                }
                prompt.append("\n");
            });
            prompt.append("\n");
        }

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
                promptSanitizer.sanitize(kpi.getKpiName()),
                promptSanitizer.sanitize(kpi.getCategorie()),
                promptSanitizer.sanitizeNumber(kpi.getValeurN1()),
                promptSanitizer.sanitizeNumber(kpi.getValeurN()),
                promptSanitizer.sanitizeNumber(kpi.getVariationPercentage()),
                promptSanitizer.sanitize(kpi.getClassification())
        )));
        prompt.append("\n");

        prompt.append("=== INSTRUCTIONS STRICTES ===\n");
        prompt.append("1. NIVEAU D'ANALYSE : Ton analyse doit être extrêmement détaillée (ISO compliance).\n");
        prompt.append("2. PLANS D'ACTIONS IMMÉDIATS : Fournis des actions correctives concrètes pour chaque risque détecté.\n");
        prompt.append("3. RAG : Utilise les seuils et l'historique fournis pour contextualiser chaque variation.\n");
        prompt.append("4. IMPORTANT : Tu dois OBLIGATOIREMENT renvoyer un objet d'analyse dans le tableau 'kpis' pour CHAQUE KPI listé dans les données actuelles. Aucun KPI ne doit être ignoré.\n");
        prompt.append("5. TOUS LES CHAMPS OBLIGATOIRES : Chaque objet KPI doit contenir tous les champs demandés (identificationRisque, actionImmediate, etc.). Ne laisse aucun champ vide.\n");
        prompt.append("Réponds uniquement avec du JSON valide.\n");
        prompt.append("Format attendu :\n");
        prompt.append("{\n");
        prompt.append("  \"overallScore\": number,\n");
        prompt.append("  \"summary\": \"Synthèse globale détaillée avec niveau d'analyse QHSE\",\n");
        prompt.append("  \"kpis\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": string,\n");
        prompt.append("      \"score\": number,\n");
        prompt.append("      \"insight\": \"Analyse détaillée + Plan d'action immédiat spécifique\",\n");
        prompt.append("      \"identificationRisque\": \"Identification précise du risque détecté\",\n");
        prompt.append("      \"problemeDetecte\": \"Description du problème ou déviation observée\",\n");
        prompt.append("      \"actionsPreventives\": \"Actions préventives ou correctives recommandées\",\n");
        prompt.append("      \"actionImmediate\": \"Action immédiate prioritaire\",\n");
        prompt.append("      \"prioriteAction\": \"CRITIQUE|HAUTE|MOYENNE|BASSE\",\n");
        prompt.append("      \"methode8D\": \"Résumé de la méthode 8D si applicable\",\n");
        prompt.append("      \"noteFinale\": \"Synthèse finale complète de l'analyse pour ce KPI\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"recommendations\": [\"Action immédiate 1\", \"Action corrective 2\"]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

}
