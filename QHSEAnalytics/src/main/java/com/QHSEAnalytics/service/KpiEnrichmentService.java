package com.QHSEAnalytics.service;

import com.QHSEAnalytics.dto.response.KpiAnalysisResult;
import com.QHSEAnalytics.dto.response.KpiEnrichedResponse;
import com.QHSEAnalytics.entity.*;
import com.QHSEAnalytics.exception.ImportNotFoundException;
import com.QHSEAnalytics.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates line-by-line KPI enrichment:
 * <ol>
 *   <li>Save / overwrite {@link KpiImportPreview} rows from the provided data.</li>
 *   <li>For each row, check the RAG knowledge base; inject context if found,
 *       or ask Gemini to generate definition + thresholds and save them.</li>
 *   <li>Call Gemini with a structured prompt and parse the JSON response.</li>
 *   <li>Persist the result into {@link KpiAnalysis}.</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KpiEnrichmentService {

    private final ImportSessionRepository importSessionRepository;
    private final KpiImportPreviewRepository previewRepository;
    private final KpiAnalysisRepository analysisRepository;
    private final RagKnowledgeRepository ragKnowledgeRepository;
    private final KpiRepository kpiRepository;
    private final LlmProviderChain llmProviderChain;
    private final GroqPromptBuilder groqPromptBuilder;
    private final ObjectProvider<KpiEnrichmentService> selfProvider;
    private final ObjectMapper objectMapper;

    // ───────────────────────────── Public API ──────────────────────────────

    /**
     * Save preview rows for an import session (idempotent – deletes previous rows first).
     */
    @Transactional
    public void savePreviewRows(Long importSessionId, List<KpiPreviewInput> inputs) {
        ImportSession session = loadSession(importSessionId);
        previewRepository.deleteByImportSessionId(importSessionId);

        List<KpiImportPreview> rows = inputs.stream().map(input -> KpiImportPreview.builder()
                .importSession(session)
                .kpiName(input.getKpiName())
                .category(input.getCategory())
                .unit(input.getUnit())
                .definition(input.getDefinition())
                .valueN(input.getValueN())
                .valueN1(input.getValueN1())
                .status(input.getStatus())
                .commentaire(input.getCommentaire())
                .variationPercent(computeVariation(input.getValueN(), input.getValueN1()))
                .ecart(computeEcart(input.getValueN(), input.getValueN1()))
                .build()
        ).collect(Collectors.toList());

        previewRepository.saveAll(rows);
        log.info("[KpiEnrichment] Saved {} preview rows for session {}", rows.size(), importSessionId);
    }

    /**
     * Run line-by-line Gemini AI analysis for all preview rows of an import session.
     * Already-analysed KPIs (by name) are skipped unless {@code force=true}.
     */
    @Transactional
    public List<KpiEnrichedResponse> analyseAll(Long importSessionId, boolean force) {
        List<KpiImportPreview> previews = previewRepository
                .findByImportSessionIdOrderByIdAsc(importSessionId);

        if (previews.isEmpty()) {
            log.warn("[KpiEnrichment] No preview rows found for session {}", importSessionId);
            return List.of();
        }

        List<KpiEnrichedResponse> results = new ArrayList<>();

        for (KpiImportPreview preview : previews) {
            try {
                results.add(selfProvider.getObject().analyseOne(preview, importSessionId, force));
            } catch (Exception ex) {
                log.error("[KpiEnrichment] Error analysing KPI '{}': {}", preview.getKpiName(), ex.getMessage(), ex);
                // continue to next KPI – do not stop the whole batch
                results.add(toEnrichedResponse(preview, null));
            }
        }

        return results;
    }

    /**
     * Return the enriched view (preview + analysis) for all KPIs of an import session.
     */
    @Transactional(readOnly = true)
    public List<KpiEnrichedResponse> getEnrichedView(Long importSessionId) {
        List<KpiImportPreview> previews = previewRepository
                .findByImportSessionIdOrderByIdAsc(importSessionId);
        Map<String, KpiAnalysis> analysisByName = analysisRepository
                .findByImportSessionIdOrderByIdAsc(importSessionId)
                .stream()
            .filter(a -> normalizeKey(a.getKpiName()) != null)
            .collect(Collectors.toMap(a -> normalizeKey(a.getKpiName()), a -> a, (a, b) -> a, LinkedHashMap::new));

        return previews.stream()
            .map(p -> toEnrichedResponse(p, analysisByName.get(normalizeKey(p.getKpiName()))))
                .collect(Collectors.toList());
    }

    // ────────────────────────────── Private ────────────────────────────────

        @Cacheable(
            value = "kpiAnalysis",
            key = "#preview.getKpiName().toLowerCase().trim() + '_' + T(java.util.Objects).hashCode(#preview.getValueN()) + '_' + T(java.util.Objects).hashCode(#preview.getValueN1())",
            unless = "#result == null",
            condition = "!#force"
        )
        public KpiEnrichedResponse analyseOne(KpiImportPreview preview, Long importSessionId, boolean force) {
        // Skip if already analysed
        if (!force) {
            Optional<KpiAnalysis> existing = analysisRepository
                    .findByImportSessionIdAndKpiName(importSessionId, preview.getKpiName());
            if (existing.isPresent()) {
                log.debug("[KpiEnrichment] Skipping already-analysed KPI '{}'", preview.getKpiName());
                return toEnrichedResponse(preview, existing.get());
            }
        }

        // 1. RAG lookup
        String ragContext = buildRagContext(preview);

        // 2. Build prompt via centralized prompt builder (token safe)
        String prompt = buildGeminiPrompt(preview, ragContext);

        // 3. Call the LLM provider chain
        String rawJson = llmProviderChain.generate(prompt);

        // 4. Parse response
        KpiAnalysisResult result = parseAnalysisResult(rawJson, preview.getKpiName());

        // 5. Persist
        KpiAnalysis analysis = saveAnalysis(preview.getImportSession(), preview.getKpiName(), result);

        // 6. Update RAG if no definition existed
        updateRagIfNeeded(preview, result);

        return toEnrichedResponse(preview, analysis);
    }

    @Cacheable(value = "ragKnowledge", key = "#kpiName.toLowerCase().trim()", unless = "#result == null")
    public RagKnowledge findRagKnowledge(String kpiName) {
        return ragKnowledgeRepository.findByKpiName(kpiName).orElse(null);
    }

    private String buildRagContext(KpiImportPreview preview) {
        RagKnowledge knowledge = findRagKnowledge(preview.getKpiName());
        if (knowledge != null) {
            StringBuilder sb = new StringBuilder();
            if (knowledge.getDefinition() != null) {
                sb.append("Définition: ").append(knowledge.getDefinition()).append("\n");
            }
            if (knowledge.getThresholds() != null) {
                sb.append("Seuils: ").append(knowledge.getThresholds()).append("\n");
            }
            return sb.toString().trim();
        }
        return null;
    }

    private String buildGeminiPrompt(KpiImportPreview preview, String ragContext) {
        double valeurN = preview.getValueN() == null ? 0d : preview.getValueN();
        double valeurN1 = preview.getValueN1() == null ? 0d : preview.getValueN1();
        double variation = preview.getVariationPercent() == null ? 0d : preview.getVariationPercent();

        int currentYear = LocalDateTime.now().getYear();
        int periodeN = currentYear;
        int periodeN1 = currentYear - 1;

        // Delegate to the centralized prompt builder which enforces token-safety and strict JSON
        return groqPromptBuilder.buildKpiPrompt(
                preview.getKpiName(),
                preview.getDefinition(),
                preview.getUnit(),
                preview.getCategory(),
                valeurN1,
                valeurN,
                variation,
                null,
                null,
                periodeN1,
                periodeN
        );
    }

    private KpiAnalysisResult parseAnalysisResult(String rawJson, String kpiName) {
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("[KpiEnrichment] AI returned empty response for KPI '{}'", kpiName);
            return buildFallbackResult(kpiName);
        }

        String cleaned = cleanAiResponse(rawJson);
        try {
            log.debug("[KpiEnrichment] Cleaned KPI analysis response preview: {}",
                    cleaned.length() > 300 ? cleaned.substring(0, 300) : cleaned);

            JsonNode root = objectMapper.readTree(cleaned);

            KpiAnalysisResult result = new KpiAnalysisResult();
            result.setKpiName(kpiName);

            // Prefer French field names, fall back to legacy English ones
            result.setRiskLevel(textOrNull(root, "risqueIa") != null ? textOrNull(root, "risqueIa") : textOrNull(root, "riskLevel"));
            // risk justification: identificationRisque or short note
            String identification = textOrNull(root, "identificationRisque");
            String noteIa = textOrNull(root, "noteIa");
            result.setRiskJustification(identification != null ? identification : (noteIa != null ? noteIa : textOrNull(root, "riskJustification")));
            result.setIdentificationRisque(identification);

            result.setObjectiveReached(root.path("objectiveAtteint").asBoolean(root.path("objectiveReached").asBoolean(false)));
            result.setImprovementDetected(root.path("ameliorationDetectee").asBoolean(root.path("improvementDetected").asBoolean(false)));

            String problemeDetecte = textOrNull(root, "problemeDetecte");
            result.setIssueDetected(problemeDetecte != null ? problemeDetecte : textOrNull(root, "issueDetected"));
            result.setProblemeDetecte(problemeDetecte);

            result.setCorrectiveAction(textOrNull(root, "actionsCorrectives") != null ? textOrNull(root, "actionsCorrectives") : textOrNull(root, "correctiveAction"));
            String actionsPreventives = textOrNull(root, "actionsPreventives");
            result.setPreventiveAction(actionsPreventives != null ? actionsPreventives : textOrNull(root, "preventiveAction"));
            result.setActionsPreventives(actionsPreventives);

            String actionImmediate = textOrNull(root, "actionImmediate");
            result.setImmediateAction(actionImmediate != null ? actionImmediate : textOrNull(root, "immediateAction"));
            result.setActionImmediate(actionImmediate);
            String prioriteAction = textOrNull(root, "prioriteAction");
            result.setImmediatePriority(prioriteAction != null ? prioriteAction : textOrNull(root, "immediatePriority"));
            result.setPrioriteAction(prioriteAction);

            result.setRequires8d(root.path("methode8D").isObject() || root.path("requires8d").asBoolean(false));

            // methode8D may be an object with D1..D8
            JsonNode methode8D = root.path("methode8D");
            if (methode8D.isMissingNode() || methode8D.isNull()) {
                methode8D = root.path("eightDDetails");
            }
            if (!methode8D.isMissingNode() && !methode8D.isNull() && methode8D.isObject()) {
                String methode8DJson = objectMapper.writeValueAsString(methode8D);
                result.setEightDDetails(methode8DJson);
                result.setMethode8D(methode8DJson);
            }

            // noteFinale preferred for final note, fallback to aiNote
            String noteFinale = textOrNull(root, "noteFinale");
            result.setAiNote(noteFinale != null ? noteFinale : textOrNull(root, "aiNote"));
            result.setNoteFinale(noteFinale);

            return result;

        } catch (Exception ex) {
            String preview = cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned;
            log.warn("[KpiEnrichment] JSON parse failed for '{}': {} | Raw preview: {}", kpiName, ex.getMessage(), preview);
            return buildFallbackResult(kpiName);
        }
    }

    private String cleanAiResponse(String rawResponse) {
        if (rawResponse == null) return null;
        String cleaned = rawResponse
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return cleaned.substring(start, end + 1).trim();
        }

        return cleaned;
    }

    /**
     * Development helper: analyse a single preview by id and return raw AI response + parsed result.
     * NOTE: For debugging only; annotate or restrict access in production as needed.
     */
    public Map<String, Object> debugAnalyseOne(Long previewId, boolean force) {
        KpiImportPreview preview = previewRepository.findById(previewId).orElse(null);
        if (preview == null) {
            return Map.of("error", "preview not found");
        }

        String ragContext = buildRagContext(preview);
        String prompt = buildGeminiPrompt(preview, ragContext);
        String rawJson = llmProviderChain.generate(prompt);
        KpiAnalysisResult parsed = parseAnalysisResult(rawJson, preview.getKpiName());

        return Map.of(
                "previewId", previewId,
                "kpiName", preview.getKpiName(),
                "rawResponse", rawJson,
                "parsed", parsed
        );
    }

    private KpiAnalysisResult buildFallbackResult(String kpiName) {
        return KpiAnalysisResult.builder()
                .kpiName(kpiName)
                .riskLevel("Modéré")
                .riskJustification("Analyse IA indisponible.")
            .identificationRisque("Analyse IA indisponible.")
                .issueDetected(null)
            .problemeDetecte(null)
                .correctiveAction("Vérification manuelle recommandée.")
                .preventiveAction("Maintenir la surveillance régulière.")
            .actionsPreventives("Maintenir la surveillance régulière.")
                .immediateAction("Revoir les données manuellement.")
            .actionImmediate("Revoir les données manuellement.")
                .immediatePriority("Moyenne")
            .prioriteAction("Moyenne")
                .requires8d(false)
                .eightDDetails(null)
            .methode8D(null)
                .aiNote("L'analyse automatique n'a pas pu être générée. Une revue manuelle de ce KPI est recommandée.")
            .noteFinale("L'analyse automatique n'a pas pu être générée. Une revue manuelle de ce KPI est recommandée.")
                .build();
    }

    @Transactional
    protected KpiAnalysis saveAnalysis(ImportSession session, String kpiName, KpiAnalysisResult result) {
        KpiAnalysis existing = analysisRepository
                .findByImportSessionIdAndKpiName(session.getId(), kpiName)
                .orElse(null);

        if (existing != null && isFallbackResult(result)) {
            log.info("[KpiEnrichment] Keeping existing analysis for KPI '{}' because AI returned fallback content", kpiName);
            return existing;
        }

        // Upsert while preserving previously enriched fields when the new AI response is partial.
        KpiAnalysis analysis = existing != null
                ? existing
                : KpiAnalysis.builder().importSession(session).kpiName(kpiName).build();

        analysis.setRiskLevel(firstNonBlank(result.getRiskLevel(), analysis.getRiskLevel()));
        analysis.setRiskJustification(firstNonBlank(result.getRiskJustification(), analysis.getRiskJustification()));
        analysis.setIdentificationRisque(firstNonBlank(result.getIdentificationRisque(), analysis.getIdentificationRisque()));
        analysis.setObjectiveReached(result.getObjectiveReached() != null ? result.getObjectiveReached() : analysis.getObjectiveReached());
        analysis.setImprovementDetected(result.getImprovementDetected() != null ? result.getImprovementDetected() : analysis.getImprovementDetected());
        analysis.setIssueDetected(firstNonBlank(result.getIssueDetected(), analysis.getIssueDetected()));
        analysis.setProblemeDetecte(firstNonBlank(result.getProblemeDetecte(), analysis.getProblemeDetecte()));
        analysis.setCorrectiveAction(firstNonBlank(result.getCorrectiveAction(), analysis.getCorrectiveAction()));
        analysis.setPreventiveAction(firstNonBlank(result.getPreventiveAction(), analysis.getPreventiveAction()));
        analysis.setActionsPreventives(firstNonBlank(result.getActionsPreventives(), analysis.getActionsPreventives()));
        analysis.setImmediateAction(firstNonBlank(result.getImmediateAction(), analysis.getImmediateAction()));
        analysis.setActionImmediate(firstNonBlank(result.getActionImmediate(), analysis.getActionImmediate()));
        analysis.setImmediatePriority(firstNonBlank(result.getImmediatePriority(), analysis.getImmediatePriority()));
        analysis.setPrioriteAction(firstNonBlank(result.getPrioriteAction(), analysis.getPrioriteAction()));
        analysis.setRequires8d(result.isRequires8d() || analysis.isRequires8d());
        analysis.setEightDDetails(firstNonBlank(result.getEightDDetails(), analysis.getEightDDetails()));
        analysis.setMethode8D(firstNonBlank(result.getMethode8D(), analysis.getMethode8D()));
        analysis.setAiNote(firstNonBlank(result.getAiNote(), analysis.getAiNote()));
        analysis.setNoteFinale(firstNonBlank(result.getNoteFinale(), analysis.getNoteFinale()));

        return analysisRepository.save(analysis);
    }

    private boolean isFallbackResult(KpiAnalysisResult result) {
        if (result == null) {
            return true;
        }
        return "Analyse IA indisponible.".equals(result.getRiskJustification())
                || (result.getAiNote() != null && result.getAiNote().contains("n'a pas pu être générée"));
    }

    private String firstNonBlank(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replace("’", "'")
                .replace("‘", "'")
                .replace("`", "'")
                .replace("“", "\"")
                .replace("”", "\"")
                .replaceAll("[^\\p{Alnum}'\"]+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    /**
     * If the KPI does not yet have a RAG entry and Gemini suggested a definition or thresholds,
     * save them so future analyses benefit from the enriched context.
     */
    private void updateRagIfNeeded(KpiImportPreview preview, KpiAnalysisResult result) {
        // We only create RAG entries when the KPI does not yet exist in the knowledge base
        if (ragKnowledgeRepository.findByKpiName(preview.getKpiName()).isPresent()) {
            return;
        }

        // Nothing to save if Gemini returned only the fallback
        if ("Analyse IA indisponible.".equals(result.getRiskJustification())) {
            return;
        }

        try {
            // Re-read suggested values from the AI note or set reasonable defaults
            String suggestedDef = preview.getDefinition() != null && !preview.getDefinition().isBlank()
                    ? preview.getDefinition()
                    : "KPI généré automatiquement via analyse IA pour la catégorie " + nullSafe(preview.getCategory());

            RagKnowledge knowledge = RagKnowledge.builder()
                    .kpiName(preview.getKpiName())
                    .definition(suggestedDef)
                    .category(preview.getCategory())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            ragKnowledgeRepository.save(knowledge);
            log.info("[KpiEnrichment] Created RAG entry for new KPI '{}'", preview.getKpiName());
        } catch (Exception ex) {
            log.warn("[KpiEnrichment] Could not create RAG entry for '{}': {}", preview.getKpiName(), ex.getMessage());
        }
    }

    // ─────────────────────── Mapping helpers ──────────────────────────────

    private KpiEnrichedResponse toEnrichedResponse(KpiImportPreview preview, KpiAnalysis analysis) {
        KpiEnrichedResponse.KpiEnrichedResponseBuilder b = KpiEnrichedResponse.builder()
                .previewId(preview.getId())
                .importSessionId(preview.getImportSession().getId())
                .kpiName(preview.getKpiName())
                .category(preview.getCategory())
                .unit(preview.getUnit())
                .definition(preview.getDefinition())
                .valueN(preview.getValueN())
                .valueN1(preview.getValueN1())
                .status(preview.getStatus())
                .commentaire(preview.getCommentaire())
                .variationPercent(preview.getVariationPercent())
                .ecart(preview.getEcart())
                .createdAt(preview.getCreatedAt());

        if (analysis != null) {
            b.analysisId(analysis.getId())
             .riskLevel(analysis.getRiskLevel())
             .riskJustification(analysis.getRiskJustification())
             .objectiveReached(analysis.getObjectiveReached())
             .improvementDetected(analysis.getImprovementDetected())
             .issueDetected(analysis.getIssueDetected())
             .correctiveAction(analysis.getCorrectiveAction())
             .preventiveAction(analysis.getPreventiveAction())
             .immediateAction(analysis.getImmediateAction())
             .immediatePriority(analysis.getImmediatePriority())
             .requires8d(analysis.isRequires8d())
             .eightDDetails(analysis.getEightDDetails())
             .aiNote(analysis.getAiNote());
        }

        return b.build();
    }

    private ImportSession loadSession(Long id) {
        return importSessionRepository.findById(id)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable: " + id));
    }

    private Double computeVariation(Double n, Double n1) {
        if (n == null || n1 == null || n1 == 0.0) return null;
        return ((n - n1) / Math.abs(n1)) * 100.0;
    }

    private Double computeEcart(Double n, Double n1) {
        if (n == null || n1 == null) return null;
        return n - n1;
    }

    private String nullSafe(Object o) {
        return o == null ? "N/A" : o.toString();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode f = node.path(field);
        if (f.isMissingNode() || f.isNull()) return null;
        String v = f.asText("").trim();
        return v.isEmpty() ? null : v;
    }

    // ─────────────────── Inner input DTO ──────────────────────────────────

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class KpiPreviewInput {
        private String kpiName;
        private String category;
        private String unit;
        private String definition;
        private Double valueN;
        private Double valueN1;
        private String status;
        private String commentaire;
    }
}
