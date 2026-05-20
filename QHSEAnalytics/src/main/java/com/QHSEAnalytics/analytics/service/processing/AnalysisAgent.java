package com.QHSEAnalytics.analytics.service.processing;

import com.QHSEAnalytics.analytics.service.LlmProviderChain;
import com.QHSEAnalytics.analytics.service.RagSearchService;
import com.QHSEAnalytics.shared.dto.llm.AiResponse;
import com.QHSEAnalytics.shared.dto.llm.KpiInsight;
import com.QHSEAnalytics.shared.dto.response.AiActionPlanItemResponse;
import com.QHSEAnalytics.shared.dto.response.AiAnalysisStructuredResponse;
import com.QHSEAnalytics.shared.dto.response.AiConfidenceResponse;
import com.QHSEAnalytics.shared.dto.response.AiContextSourceResponse;
import com.QHSEAnalytics.shared.dto.response.AiKpiInsightResponse;
import com.QHSEAnalytics.shared.dto.response.AiPredictiveAlertResponse;
import com.QHSEAnalytics.shared.dto.response.AiRecommendationResponse;
import com.QHSEAnalytics.shared.dto.response.AiRootCauseResponse;
import com.QHSEAnalytics.shared.dto.response.AiTraceabilityResponse;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.entity.AnalyseGlobale;
import com.QHSEAnalytics.shared.entity.RagKnowledge;
import com.QHSEAnalytics.shared.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisAgent {

    private static final int MAX_VALIDATION_RETRY_ATTEMPTS = 2;

    private final LlmProviderChain llmProviderChain;
    private final KpiRepository kpiRepository;
    private final AnalyseGlobaleRepository analyseGlobaleRepository;
    private final ObjectMapper objectMapper;
    private final StructuredAnalysisPromptBuilder structuredAnalysisPromptBuilder;
    private final StructuredAnalysisValidator structuredAnalysisValidator;
    private final MeterRegistry meterRegistry;
    private final PromptSanitizer promptSanitizer;
    private final RagSearchService ragSearchService;

    @Value("${app.analysis.batch-size:5}")
    private int analysisBatchSize = 5;

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
                .sorted(Comparator.comparingDouble(k ->
                        k.getVariationPercentage() == null ? 0.0 : -Math.abs(k.getVariationPercentage())))
                .collect(Collectors.toList());

        String cacheKeyPrefix = String.format(
                "importSessionId=%s|mode=structured|promptVersion=%s|schemaVersion=%s|providerChainVersion=%s",
                importSessionId == null ? "unknown" : importSessionId,
                structuredAnalysisPromptBuilder.getPromptVersion(),
                "1.2",
                LlmProviderChain.PROVIDER_CHAIN_VERSION
        );
        int chunkSize = Math.max(1, analysisBatchSize);
        List<List<KpiCalculatedDTO>> chunks = partition(topKpis, chunkSize);
        log.info("[AnalysisAgent] analyseStructured: starting with {} KPIs split into {} chunk(s), importSessionId={}, cachePrefix={}, bypassCache={}",
                topKpis.size(), chunks.size(), importSessionId, cacheKeyPrefix, bypassCache);

        long start = System.currentTimeMillis();
        List<AiAnalysisStructuredResponse> successfulChunkResponses = new ArrayList<>();
        List<String> chunkFailureReasons = new ArrayList<>();
        Set<String> providersUsed = new LinkedHashSet<>();
        boolean anyRetry = false;
        boolean anyParseFailure = false;
        boolean anyValidationFailure = false;

        for (int index = 0; index < chunks.size(); index++) {
            List<KpiCalculatedDTO> chunk = chunks.get(index);
            int chunkNumber = index + 1;
            String chunkCacheKeyPrefix = cacheKeyPrefix + "|chunk=" + chunkNumber;
            log.info("[AnalysisAgent] analyseStructured: starting chunk {}/{} ({} KPIs)", chunkNumber, chunks.size(), chunk.size());
            ChunkAnalysisResult chunkResult = analyzeStructuredChunkWithRetries(
                    chunk,
                    topKpis,
                    importSessionId,
                    bypassCache,
                    chunkCacheKeyPrefix,
                    chunkNumber,
                    chunks.size()
            );
            anyRetry |= chunkResult.retried();
            anyParseFailure |= chunkResult.failureType() == ChunkFailureType.PARSE;
            anyValidationFailure |= chunkResult.failureType() == ChunkFailureType.VALIDATION;

            if (chunkResult.response() != null) {
                successfulChunkResponses.add(chunkResult.response());
                if (chunkResult.providerUsed() != null && !chunkResult.providerUsed().isBlank()) {
                    providersUsed.add(chunkResult.providerUsed());
                }
            } else if (chunkResult.failureReason() != null && !chunkResult.failureReason().isBlank()) {
                chunkFailureReasons.add(String.format("chunk %d/%d: %s", chunkNumber, chunks.size(), chunkResult.failureReason()));
            }
        }

        long latency = System.currentTimeMillis() - start;
        if (successfulChunkResponses.isEmpty()) {
            log.warn("[AnalysisAgent] analyseStructured: all chunks failed, importSessionId={}, total_chunks={}, latency={}ms",
                    importSessionId, chunks.size(), latency);
            incrementCounter("ai.request.count", Tags.of("mode", "structured", "status", "failed"));
            if (anyRetry) {
                incrementCounter("ai.retry.count", Tags.of("mode", "structured"));
            }
            if (anyParseFailure) {
                incrementCounter("ai.parse.error.count", Tags.of("mode", "structured"));
            }
            if (anyValidationFailure) {
                incrementCounter("ai.validation.error.count", Tags.of("mode", "structured"));
            }
            recordLatency("ai.latency.ms", Tags.of("mode", "structured"), latency);
            String failureReason = chunkFailureReasons.isEmpty()
                    ? "Aucune réponse du provider IA."
                    : String.join(" | ", chunkFailureReasons);
            return buildStructuredFallback("FAILED", failureReason);
        }

        AiAnalysisStructuredResponse mergedResponse = mergeChunkResponses(successfulChunkResponses);
        int mergedMissingCount = computeMissingKpiCount(topKpis, mergedResponse.getKpiInsights());
        int mergedCoverage = topKpis.size() - mergedMissingCount;
        log.info("[AnalysisAgent] analyseStructured: merged coverage {}/{}", mergedCoverage, topKpis.size());

        // Generate a final globalSummary from the merged insights so it covers ALL KPIs,
        // not just the first chunk.
        if (chunks.size() > 1 && !successfulChunkResponses.isEmpty()) {
            String finalSummary = generateFinalSummary(topKpis, mergedResponse, importSessionId, cacheKeyPrefix);
            if (finalSummary != null && !finalSummary.isBlank()) {
                mergedResponse.setGlobalSummary(finalSummary);
                log.info("[AnalysisAgent] analyseStructured: final globalSummary generated ({} chars)", finalSummary.length());
            }
        }

        enrichStructuredResponse(mergedResponse, formatProviderLabel(providersUsed), importSessionId);

        boolean allChunksSucceeded = successfulChunkResponses.size() == chunks.size();
        if (allChunksSucceeded && mergedMissingCount == 0) {
            mergedResponse.setStatus("SUCCESS");
            incrementCounter("ai.request.count", Tags.of("mode", "structured", "status", "success"));
            if (anyRetry) {
                incrementCounter("ai.retry.count", Tags.of("mode", "structured"));
            }
            recordLatency("ai.latency.ms", Tags.of("mode", "structured"), latency);
            return mergedResponse;
        }

        if (mergedMissingCount > 0) {
            chunkFailureReasons.add("coverage incomplete after chunk merge: missing " + mergedMissingCount + " KPI(s)");
        }
        mergedResponse.setStatus("PARTIAL");
        mergedResponse.setFallbackReason(String.join(" | ", chunkFailureReasons));
        incrementCounter("ai.request.count", Tags.of("mode", "structured", "status", "partial"));
        if (anyRetry) {
            incrementCounter("ai.retry.count", Tags.of("mode", "structured"));
        }
        if (anyParseFailure) {
            incrementCounter("ai.parse.error.count", Tags.of("mode", "structured"));
        }
        if (anyValidationFailure || mergedMissingCount > 0) {
            incrementCounter("ai.validation.error.count", Tags.of("mode", "structured"));
        }
        recordLatency("ai.latency.ms", Tags.of("mode", "structured"), latency);
        return mergedResponse;
    }

    private ChunkAnalysisResult analyzeStructuredChunk(List<KpiCalculatedDTO> chunkKpis,
                                                       List<KpiCalculatedDTO> allKpis,
                                                       Long importSessionId,
                                                       boolean bypassCache,
                                                       String chunkCacheKeyPrefix,
                                                       int chunkNumber,
                                                       int totalChunks) {
        String prompt = structuredAnalysisPromptBuilder.buildPrompt(chunkKpis, allKpis.size(), allKpis);
        long start = System.currentTimeMillis();
        LlmProviderChain.ProviderResult providerResult = llmProviderChain.generate(prompt, chunkCacheKeyPrefix, bypassCache);
        long latency = System.currentTimeMillis() - start;
        String providerUsed = providerResult == null ? "none" : providerResult.provider();
        String responseJson = providerResult == null ? null : providerResult.response();
        log.info("[AnalysisAgent] analyseStructured: provider response received in {}ms, importSessionId={}, provider={}, cachePrefix={}",
                latency, importSessionId, providerUsed, chunkCacheKeyPrefix);

        if (responseJson == null || responseJson.isBlank()) {
            log.warn("[AnalysisAgent] analyseStructured: empty response from provider. reason=empty_response, chunk={}/{}, latency={}ms, provider={}",
                    chunkNumber, totalChunks, latency, providerUsed);
            return new ChunkAnalysisResult(null, providerUsed, "Aucune réponse du provider IA.", ChunkFailureType.EMPTY, false);
        }

        AiAnalysisStructuredResponse response = parseStructuredResponse(responseJson);
        if (response == null) {
            log.warn("[AnalysisAgent] analyseStructured: initial parsing failed. reason=initial_parsing_failed, chunk={}/{}, latency={}ms",
                    chunkNumber, totalChunks, latency);
            return new ChunkAnalysisResult(null, providerUsed, "Impossible de parser la réponse IA.", ChunkFailureType.PARSE, false);
        }

        structuredAnalysisValidator.sanitize(response);
        List<String> errors = validateChunkResponse(response, chunkKpis);
        int missingCount = computeMissingKpiCount(chunkKpis, response.getKpiInsights());
        log.info("[AnalysisAgent] analyseStructured: coverage total_input_kpis={} total_output_insights={} missing_kpis_count={} importSessionId={} chunk={}/{}",
                chunkKpis.size(), response.getKpiInsights() == null ? 0 : response.getKpiInsights().size(), missingCount, importSessionId, chunkNumber, totalChunks);
        if (errors.isEmpty()) {
            return new ChunkAnalysisResult(response, providerUsed, null, ChunkFailureType.NONE, false);
        }

        log.warn("[AnalysisAgent] analyseStructured: validation failed. reason=validation_errors, chunk={}/{}, error_count={}, errors={}, latency={}ms",
                chunkNumber, totalChunks, errors.size(), errors, latency);
        String retryPrompt = structuredAnalysisPromptBuilder.buildRetryPrompt(prompt, String.join("; ", errors));
        log.info("[AnalysisAgent] analyseStructured: retry attempt initiated. retry_count=1, chunk={}/{}", chunkNumber, totalChunks);
        LlmProviderChain.ProviderResult retryResult = llmProviderChain.generate(retryPrompt, chunkCacheKeyPrefix, bypassCache);
        AiAnalysisStructuredResponse retryResponse = parseStructuredResponse(retryResult == null ? null : retryResult.response());
        if (retryResponse == null) {
            log.warn("[AnalysisAgent] analyseStructured: retry parsing failed. reason=retry_parsing_failed, retry_count=1, chunk={}/{}, latency={}ms",
                    chunkNumber, totalChunks, latency);
            return new ChunkAnalysisResult(
                    null,
                    retryResult == null ? providerUsed : retryResult.provider(),
                    "Impossible de parser la réponse IA après retry.",
                    ChunkFailureType.PARSE,
                    true
            );
        }

        structuredAnalysisValidator.sanitize(retryResponse);
        List<String> retryErrors = validateChunkResponse(retryResponse, chunkKpis);
        int retryMissingCount = computeMissingKpiCount(chunkKpis, retryResponse.getKpiInsights());
        log.info("[AnalysisAgent] analyseStructured: retry coverage total_input_kpis={} total_output_insights={} missing_kpis_count={} importSessionId={} chunk={}/{}",
                chunkKpis.size(), retryResponse.getKpiInsights() == null ? 0 : retryResponse.getKpiInsights().size(), retryMissingCount, importSessionId, chunkNumber, totalChunks);
        if (retryErrors.isEmpty()) {
            return new ChunkAnalysisResult(
                    retryResponse,
                    retryResult == null ? providerUsed : retryResult.provider(),
                    null,
                    ChunkFailureType.NONE,
                    true
            );
        }

        log.warn("[AnalysisAgent] analyseStructured: retry validation failed. reason=retry_validation_errors, chunk={}/{}, error_count={}, errors={}, retry_count=1, latency={}ms",
                chunkNumber, totalChunks, retryErrors.size(), retryErrors, latency);
        return new ChunkAnalysisResult(
                null,
                retryResult == null ? providerUsed : retryResult.provider(),
                "Réponse IA invalide après retry : " + String.join("; ", retryErrors),
                ChunkFailureType.VALIDATION,
                true
        );
    }

    private ChunkAnalysisResult analyzeStructuredChunkWithRetries(List<KpiCalculatedDTO> chunkKpis,
                                                                  List<KpiCalculatedDTO> allKpis,
                                                                  Long importSessionId,
                                                                  boolean bypassCache,
                                                                  String chunkCacheKeyPrefix,
                                                                  int chunkNumber,
                                                                  int totalChunks) {
        String basePrompt = structuredAnalysisPromptBuilder.buildPrompt(chunkKpis, allKpis.size(), allKpis);
        String retryPromptBase = basePrompt;
        String currentPrompt = basePrompt;
        String providerUsed = "none";
        boolean retried = false;
        boolean genericRetryTriggered = false;
        int validationRetryCount = 0;
        int attemptNumber = 0;

        while (true) {
            attemptNumber++;
            long start = System.currentTimeMillis();
            LlmProviderChain.ProviderResult providerResult = llmProviderChain.generate(currentPrompt, chunkCacheKeyPrefix, bypassCache);
            long latency = System.currentTimeMillis() - start;
            providerUsed = providerResult == null ? providerUsed : providerResult.provider();
            String responseJson = providerResult == null ? null : providerResult.response();
            log.info("[AnalysisAgent] analyseStructured: provider response received in {}ms, importSessionId={}, provider={}, cachePrefix={}, attempt={}",
                    latency, importSessionId, providerUsed, chunkCacheKeyPrefix, attemptNumber);

            if (responseJson == null || responseJson.isBlank()) {
                log.warn("[AnalysisAgent] analyseStructured: empty response from provider. reason=empty_response, chunk={}/{}, latency={}ms, provider={}, attempt={}",
                        chunkNumber, totalChunks, latency, providerUsed, attemptNumber);
                return new ChunkAnalysisResult(null, providerUsed, "Aucune réponse du provider IA.", ChunkFailureType.EMPTY, retried);
            }

            AiAnalysisStructuredResponse response = parseStructuredResponse(responseJson);
            if (response == null) {
                String failureReason = attemptNumber == 1
                        ? "Impossible de parser la réponse IA."
                        : "Impossible de parser la réponse IA après retry.";
                log.warn("[AnalysisAgent] analyseStructured: parsing failed. reason={}, chunk={}/{}, attempt={}, latency={}ms",
                        attemptNumber == 1 ? "initial_parsing_failed" : "retry_parsing_failed",
                        chunkNumber, totalChunks, attemptNumber, latency);
                return new ChunkAnalysisResult(null, providerUsed, failureReason, ChunkFailureType.PARSE, retried);
            }

            boolean genericResponse = isGenericResponse(response);
            structuredAnalysisValidator.sanitize(response);
            if (genericResponse) {
                if (!genericRetryTriggered) {
                    log.warn("AI returned generic/empty response, triggering retry");
                    retryPromptBase = basePrompt + StructuredAnalysisPromptBuilder.GENERIC_RETRY_SUFFIX;
                    currentPrompt = retryPromptBase;
                    genericRetryTriggered = true;
                    retried = true;
                    continue;
                }

                log.warn("[AnalysisAgent] analyseStructured: generic response persisted after retry. chunk={}/{}, attempt={}, latency={}ms",
                        chunkNumber, totalChunks, attemptNumber, latency);
                return new ChunkAnalysisResult(
                        null,
                        providerUsed,
                        "Réponse IA générique après retry.",
                        ChunkFailureType.VALIDATION,
                        retried
                );
            }

            List<String> errors = validateChunkResponse(response, chunkKpis);
            int missingCount = computeMissingKpiCount(chunkKpis, response.getKpiInsights());
            log.info("[AnalysisAgent] analyseStructured: coverage total_input_kpis={} total_output_insights={} missing_kpis_count={} importSessionId={} chunk={}/{} attempt={}",
                    chunkKpis.size(), response.getKpiInsights() == null ? 0 : response.getKpiInsights().size(), missingCount, importSessionId, chunkNumber, totalChunks, attemptNumber);
            if (errors.isEmpty()) {
                return new ChunkAnalysisResult(response, providerUsed, null, ChunkFailureType.NONE, retried);
            }

            log.warn("[AnalysisAgent] analyseStructured: validation failed. reason=validation_errors, chunk={}/{}, error_count={}, errors={}, latency={}ms, attempt={}",
                    chunkNumber, totalChunks, errors.size(), errors, latency, attemptNumber);
            if (validationRetryCount >= MAX_VALIDATION_RETRY_ATTEMPTS) {
                log.warn("[AnalysisAgent] analyseStructured: retry validation failed. reason=retry_validation_errors, chunk={}/{}, error_count={}, errors={}, retry_count={}, latency={}ms",
                        chunkNumber, totalChunks, errors.size(), errors, validationRetryCount, latency);
                return new ChunkAnalysisResult(
                        null,
                        providerUsed,
                        "Réponse IA invalide après retry : " + String.join("; ", errors),
                        ChunkFailureType.VALIDATION,
                        retried
                );
            }

            validationRetryCount++;
            retried = true;
            currentPrompt = structuredAnalysisPromptBuilder.buildRetryPrompt(retryPromptBase, String.join("; ", errors));
            log.info("[AnalysisAgent] analyseStructured: retry attempt initiated. retry_count={}, chunk={}/{}", validationRetryCount, chunkNumber, totalChunks);
        }
    }

    private boolean isGenericResponse(AiAnalysisStructuredResponse response) {
        if (response == null || response.getKpiInsights() == null || response.getKpiInsights().isEmpty()) {
            return true;
        }
        long blankCount = response.getKpiInsights().stream()
                .filter(kpiInsight -> kpiInsight == null
                        || kpiInsight.getActionImmediate() == null
                        || kpiInsight.getActionImmediate().isBlank()
                        || kpiInsight.getActionImmediate().length() < 15
                        || kpiInsight.getInsight() == null
                        || kpiInsight.getInsight().isBlank()
                        || kpiInsight.getInsight().length() < 20)
                .count();
        return blankCount > response.getKpiInsights().size() / 2;
    }

    private List<String> validateChunkResponse(AiAnalysisStructuredResponse response, List<KpiCalculatedDTO> chunkKpis) {
        List<String> errors = new ArrayList<>(structuredAnalysisValidator.validate(response, chunkKpis));
        int missingCount = computeMissingKpiCount(chunkKpis, response.getKpiInsights());
        if (missingCount > 0) {
            log.warn("[AnalysisAgent] Chunk partial coverage: {}/{} KPIs covered — {} missing (accepted, will be PARTIAL in merge)",
                    chunkKpis.size() - missingCount, chunkKpis.size(), missingCount);
        }
        return errors.stream().distinct().toList();
    }

    private String generateFinalSummary(List<KpiCalculatedDTO> allKpis,
                                         AiAnalysisStructuredResponse merged,
                                         Long importSessionId,
                                         String cacheKeyPrefix) {
        try {
            StructuredAnalysisPromptBuilder.ScoreSummary scores = structuredAnalysisPromptBuilder.computeScoresPublic(allKpis);

            StringBuilder prompt = new StringBuilder();
            prompt.append("Tu es un consultant QHSE senior. Rédige un unique paragraphe narratif (5 à 8 phrases) ");
            prompt.append("en français, sans saut de ligne, sans puces, sans titre. ");
            prompt.append("Ce paragraphe est le globalSummary d'un rapport QHSE destiné à la direction.\n\n");

            // Inject scores
            String periodeN1 = allKpis.stream().map(KpiCalculatedDTO::getPeriodeN1).filter(p -> p != null && p > 0)
                    .findFirst().map(String::valueOf).orElse("N-1");
            String periodeN = allKpis.stream().map(KpiCalculatedDTO::getPeriodeN).filter(p -> p != null && p > 0)
                    .findFirst().map(String::valueOf).orElse("N");
            prompt.append(String.format("Période : %s → %s. Score global : %d/100 (%s). ",
                    periodeN1, periodeN, scores.globalScore, scores.globalLabel));
            prompt.append(String.format("KPIs totaux : %d | Critiques : %d | Modérés : %d | OK : %d.\n",
                    allKpis.size(), scores.critiques, scores.moderes, scores.faibledOk));
            for (StructuredAnalysisPromptBuilder.ScoreSummary.CatScore cs : scores.categories) {
                prompt.append(String.format("Catégorie %s : score %d/100 (%d critique(s), %d modéré(s), %d OK).\n",
                        cs.libelle, cs.score, cs.critiques, cs.moderes, cs.ok));
            }

            // Inject one-line summary per KPI insight
            if (merged.getKpiInsights() != null && !merged.getKpiInsights().isEmpty()) {
                prompt.append("\nRésumé des analyses par KPI :\n");
                for (var insight : merged.getKpiInsights()) {
                    String urgency = insight.getUrgency() != null ? insight.getUrgency() : "?";
                    String action = insight.getActionImmediate() != null
                            ? insight.getActionImmediate().substring(0, Math.min(80, insight.getActionImmediate().length()))
                            : "";
                    prompt.append(String.format("- %s [%s] : %s\n", insight.getKpiName(), urgency, action));
                }
            }

            prompt.append("\nConsignes strictes :\n");
            prompt.append("1. Couvre TOUS les KPIs listés ci-dessus dans le paragraphe.\n");
            prompt.append("2. Cite les valeurs chiffrées (score global, nombre de critiques, noms des KPIs les plus dégradés).\n");
            prompt.append("3. Mentionne les 2-3 actions prioritaires avec leur horizon temporel.\n");
            prompt.append("4. Termine par une conclusion sur la trajectoire globale.\n");
            prompt.append("5. Réponds avec le texte du paragraphe uniquement — aucun JSON, aucun markdown.\n");

            String summaryKey = cacheKeyPrefix + "|final-summary";
            LlmProviderChain.ProviderResult result = llmProviderChain.generate(prompt.toString(), summaryKey, false);
            if (result == null || result.response() == null || result.response().isBlank()) {
                return null;
            }
            // Strip any accidental JSON wrapping the LLM may produce
            String raw = result.response().trim();
            if (raw.startsWith("{") || raw.startsWith("[")) {
                return null;
            }
            // Remove markdown bold/italic
            raw = raw.replaceAll("[*_`#]", "").trim();
            return raw;
        } catch (Exception ex) {
            log.warn("[AnalysisAgent] generateFinalSummary failed: {}", ex.getMessage());
            return null;
        }
    }

    private AiAnalysisStructuredResponse mergeChunkResponses(List<AiAnalysisStructuredResponse> chunkResponses) {
        // Keep only the first non-blank globalSummary: each chunk generates a full synthesis
        // from partial KPI data, so concatenating them produces duplicate sections (§1, §2... repeated N times).
        String mergedSummary = chunkResponses.stream()
                .map(AiAnalysisStructuredResponse::getGlobalSummary)
                .filter(summary -> summary != null && !summary.isBlank())
                .findFirst()
                .orElse("");

        List<AiKpiInsightResponse> mergedInsights = chunkResponses.stream()
                .flatMap(response -> safeList(response.getKpiInsights()).stream())
                .distinct()
                .toList();
        List<String> mergedProbableCauses = chunkResponses.stream()
                .flatMap(response -> safeList(response.getProbableCauses()).stream())
                .distinct()
                .toList();
        List<AiRecommendationResponse> mergedRecommendations = chunkResponses.stream()
                .flatMap(response -> safeList(response.getRecommendations()).stream())
                .distinct()
                .toList();
        List<AiActionPlanItemResponse> mergedActionPlan = chunkResponses.stream()
                .flatMap(response -> safeList(response.getActionPlan()).stream())
                .distinct()
                .toList();
        List<AiRootCauseResponse> mergedRootCauses = chunkResponses.stream()
                .flatMap(response -> safeList(response.getRootCauseAnalysis()).stream())
                .distinct()
                .toList();
        List<AiPredictiveAlertResponse> mergedPredictiveAlerts = chunkResponses.stream()
                .flatMap(response -> safeList(response.getPredictiveAlerts()).stream())
                .distinct()
                .toList();
        List<AiContextSourceResponse> mergedContextSources = chunkResponses.stream()
                .map(AiAnalysisStructuredResponse::getTraceability)
                .filter(traceability -> traceability != null)
                .flatMap(traceability -> safeList(traceability.getContextSourcesUsed()).stream())
                .distinct()
                .toList();

        return AiAnalysisStructuredResponse.builder()
                .globalSummary(mergedSummary)
                .confidence(mergeConfidence(chunkResponses))
                .kpiInsights(mergedInsights)
                .probableCauses(mergedProbableCauses)
                .recommendations(mergedRecommendations)
                .actionPlan(mergedActionPlan)
                .rootCauseAnalysis(mergedRootCauses)
                .predictiveAlerts(mergedPredictiveAlerts)
                .traceability(AiTraceabilityResponse.builder()
                        .contextSourcesUsed(mergedContextSources)
                        .build())
                .build();
    }

    private AiConfidenceResponse mergeConfidence(List<AiAnalysisStructuredResponse> chunkResponses) {
        double overallSum = 0.0;
        int overallCount = 0;
        Map<String, Double> sectionSums = new LinkedHashMap<>();
        Map<String, Integer> sectionCounts = new LinkedHashMap<>();

        for (AiAnalysisStructuredResponse response : chunkResponses) {
            AiConfidenceResponse confidence = response.getConfidence();
            if (confidence == null) {
                continue;
            }
            if (confidence.getOverall() != null) {
                overallSum += confidence.getOverall();
                overallCount++;
            }
            if (confidence.getSections() != null) {
                confidence.getSections().forEach((key, value) -> {
                    if (value == null) {
                        return;
                    }
                    sectionSums.merge(key, value, Double::sum);
                    sectionCounts.merge(key, 1, Integer::sum);
                });
            }
        }

        Map<String, Double> mergedSections = new LinkedHashMap<>();
        sectionSums.forEach((key, sum) -> mergedSections.put(key, sum / sectionCounts.get(key)));

        return AiConfidenceResponse.builder()
                .overall(overallCount == 0 ? 0.0 : overallSum / overallCount)
                .sections(mergedSections)
                .build();
    }

    private String formatProviderLabel(Set<String> providersUsed) {
        List<String> normalizedProviders = providersUsed.stream()
                .filter(provider -> provider != null && !provider.isBlank() && !"none".equalsIgnoreCase(provider))
                .toList();
        if (normalizedProviders.isEmpty()) {
            return "fallback";
        }
        return String.join(",", normalizedProviders);
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
        }
        // Merge a tiny last chunk (< 3 items) into the previous one to avoid sending
        // a near-empty prompt that causes the LLM to produce a generic/empty response.
        if (chunks.size() >= 2) {
            List<T> last = chunks.get(chunks.size() - 1);
            if (last.size() < 3) {
                List<T> prev = chunks.get(chunks.size() - 2);
                prev.addAll(last);
                chunks.remove(chunks.size() - 1);
                log.debug("[AnalysisAgent] Merged tiny last chunk ({} items) into previous chunk ({} items total)",
                        last.size(), prev.size());
            }
        }
        return chunks;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private int computeMissingKpiCount(List<KpiCalculatedDTO> availableKpis, List<? extends Object> insights) {
        if (availableKpis == null || availableKpis.isEmpty() || insights == null) {
            return availableKpis == null ? 0 : availableKpis.size();
        }
        Set<String> coveredIds = new HashSet<>();
        Set<String> coveredNames = new HashSet<>();
        insights.forEach(item -> {
            if (item instanceof AiKpiInsightResponse insight) {
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
                cleaned = cleaned.replaceAll("^(```|~~~)[^\\n]*\\n", "").replaceAll("(```|~~~)$", "").trim();
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

            double rawScore = root.path("overallScore").asDouble(0.0);
            response.setOverallScore(Double.isNaN(rawScore) || Double.isInfinite(rawScore)
                    ? 0.0 : Math.max(0.0, Math.min(100.0, rawScore)));

            String summary = root.path("summary").asText("Analyse indisponible");
            if (summary.length() > 3000) {
                summary = summary.substring(0, 3000) + "…";
            }
            response.setSummary(summary);

            List<String> recs = new ArrayList<>();
            root.path("recommendations").forEach(n -> recs.add(n.asText()));
            response.setRecommendations(recs);

            List<KpiInsight> kpis = new ArrayList<>();
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
                double score = (Double.isNaN(kpiRawScore) || Double.isInfinite(kpiRawScore))
                        ? 0.0 : Math.max(0.0, Math.min(100.0, kpiRawScore));
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
        data.forEach(kpi -> {
            StringBuilder line = new StringBuilder(String.format(
                    "KPI: %s | Catégorie: %s | Direction: %s | N-1=%s | N=%s | Δ_abs=%s | Δ_rel=%s%% | Classification=%s | NiveauRisque=%s",
                    promptSanitizer.sanitize(kpi.getKpiName()),
                    promptSanitizer.sanitize(kpi.getCategorie()),
                    kpi.getDirection() != null ? kpi.getDirection() : "?",
                    promptSanitizer.sanitizeNumber(kpi.getValeurN1()),
                    promptSanitizer.sanitizeNumber(kpi.getValeurN()),
                    promptSanitizer.sanitizeNumber(kpi.getAbsoluteGap()),
                    promptSanitizer.sanitizeNumber(kpi.getVariationPercentage()),
                    promptSanitizer.sanitize(kpi.getClassification()),
                    kpi.getRiskLevel() != null ? kpi.getRiskLevel() : "?"
            ));
            if (kpi.getSeuilFaible() != null || kpi.getSeuilModere() != null || kpi.getSeuilCritique() != null) {
                line.append(String.format(" | Seuils[F=%s,M=%s,C=%s]",
                        promptSanitizer.sanitizeNumber(kpi.getSeuilFaible()),
                        promptSanitizer.sanitizeNumber(kpi.getSeuilModere()),
                        promptSanitizer.sanitizeNumber(kpi.getSeuilCritique())));
            }
            if (kpi.getSpcOutOfControl() != null && kpi.getSpcOutOfControl()) {
                line.append(" | HORS_CONTROLE_SPC");
            }
            if (Boolean.TRUE.equals(kpi.getReviewRequired())) {
                line.append(" | REVUE_REQUISE");
            }
            prompt.append(line).append("\n");
        });
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

    private enum ChunkFailureType {
        NONE,
        EMPTY,
        PARSE,
        VALIDATION
    }

    private record ChunkAnalysisResult(
            AiAnalysisStructuredResponse response,
            String providerUsed,
            String failureReason,
            ChunkFailureType failureType,
            boolean retried
    ) {
    }
}
