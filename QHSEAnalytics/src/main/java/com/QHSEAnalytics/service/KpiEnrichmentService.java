package com.QHSEAnalytics.service;

import com.QHSEAnalytics.dto.response.KpiAnalysisResult;
import com.QHSEAnalytics.dto.response.KpiEnrichedResponse;
import com.QHSEAnalytics.entity.*;
import com.QHSEAnalytics.exception.ImportNotFoundException;
import com.QHSEAnalytics.repository.*;
import com.QHSEAnalytics.service.processing.GeminiClientService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final GeminiClientService geminiClientService;
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
                results.add(analyseOne(preview, importSessionId, force));
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
                .collect(Collectors.toMap(KpiAnalysis::getKpiName, a -> a, (a, b) -> a));

        return previews.stream()
                .map(p -> toEnrichedResponse(p, analysisByName.get(p.getKpiName())))
                .collect(Collectors.toList());
    }

    // ────────────────────────────── Private ────────────────────────────────

    private KpiEnrichedResponse analyseOne(KpiImportPreview preview, Long importSessionId, boolean force) {
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

        // 2. Build prompt
        String prompt = buildGeminiPrompt(preview, ragContext);

        // 3. Call Gemini
        String rawJson = geminiClientService.generateRaw(prompt);

        // 4. Parse response
        KpiAnalysisResult result = parseAnalysisResult(rawJson, preview.getKpiName());

        // 5. Persist
        KpiAnalysis analysis = saveAnalysis(preview.getImportSession(), preview.getKpiName(), result);

        // 6. Update RAG if no definition existed
        updateRagIfNeeded(preview, result);

        return toEnrichedResponse(preview, analysis);
    }

    private String buildRagContext(KpiImportPreview preview) {
        Optional<RagKnowledge> rag = ragKnowledgeRepository.findByKpiName(preview.getKpiName());
        if (rag.isPresent()) {
            RagKnowledge knowledge = rag.get();
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
        Double varPct = preview.getVariationPercent();
        Double ecart  = preview.getEcart();

        // Fetch official thresholds from KPI table if available
        String officialThresholds = "";
        Optional<Kpi> kpiOpt = kpiRepository.findByNom(preview.getKpiName());
        if (kpiOpt.isPresent()) {
            Kpi k = kpiOpt.get();
            officialThresholds = String.format(
                "SEUILS OFFICIELS (Table KPI):\n- Faible: < %.2f\n- Modéré: %.2f - %.2f\n- Critique: > %.2f\n",
                k.getSeuilFaible(), k.getSeuilFaible(), k.getSeuilModere(), k.getSeuilCritique()
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert QHSE (Quality, Health, Safety, Environment) analyst.\n");
        sb.append("Analyze the following KPI and return a valid JSON object ONLY (no markdown, no explanation).\n\n");
        sb.append("KPI DATA:\n");
        sb.append("- Name: ").append(preview.getKpiName()).append("\n");
        sb.append("- Category: ").append(nullSafe(preview.getCategory())).append("\n");
        sb.append("- Unit: ").append(nullSafe(preview.getUnit())).append("\n");
        sb.append("- Value N (2026): ").append(nullSafe(preview.getValueN())).append("\n");
        sb.append("- Value N-1 (2025): ").append(nullSafe(preview.getValueN1())).append("\n");
        sb.append("- Variation %: ").append(varPct != null ? String.format("%.2f%%", varPct) : "N/A").append("\n");
        sb.append("- Écart (absolute): ").append(ecart != null ? String.format("%.2f", ecart) : "N/A").append("\n");
        if (preview.getDefinition() != null && !preview.getDefinition().isBlank()) {
            sb.append("- Definition: ").append(preview.getDefinition()).append("\n");
        }
        if (!officialThresholds.isEmpty()) {
            sb.append("\n").append(officialThresholds).append("\n");
        }
        if (ragContext != null && !ragContext.isBlank()) {
            sb.append("\nKNOWLEDGE BASE CONTEXT (RAG):\n").append(ragContext).append("\n");
        }
        sb.append("\nReturn ONLY this exact JSON structure (all fields required):\n");
        sb.append("{\n");
        sb.append("  \"riskLevel\": \"Faible|Modéré|Élevé\",\n");
        sb.append("  \"riskJustification\": \"string\",\n");
        sb.append("  \"objectiveReached\": true|false,\n");
        sb.append("  \"improvementDetected\": true|false,\n");
        sb.append("  \"issueDetected\": \"string or null\",\n");
        sb.append("  \"correctiveAction\": \"string or null\",\n");
        sb.append("  \"preventiveAction\": \"string\",\n");
        sb.append("  \"immediateAction\": \"string\",\n");
        sb.append("  \"immediatePriority\": \"Haute|Moyenne|Basse\",\n");
        sb.append("  \"requires8d\": true|false,\n");
        sb.append("  \"eightDDetails\": {\n");
        sb.append("    \"D1\": \"Equipe\",\n");
        sb.append("    \"D2\": \"Problème\",\n");
        sb.append("    \"D3\": \"Confinement\",\n");
        sb.append("    \"D4\": \"Cause Racine\",\n");
        sb.append("    \"D5\": \"Actions Correctives\",\n");
        sb.append("    \"D6\": \"Validation\",\n");
        sb.append("    \"D7\": \"Prévention\",\n");
        sb.append("    \"D8\": \"Clôture\"\n");
        sb.append("  },\n");
        sb.append("  \"aiNote\": \"Analyse business en français: indiquez si l'objectif est atteint et si la tendance s'améliore.\"\n");
        sb.append("}\n");
        sb.append("\nRules:\n");
        sb.append("- objectiveReached is true if Value N meets the thresholds (usually < Faible or within target).\n");
        sb.append("- improvementDetected is true if the variation shows a positive trend compared to N-1.\n");
        sb.append("- If requires8d is false, set eightDDetails to null.\n");
        sb.append("- Write everything in French, maximum 3 sentences for aiNote.\n");

        return sb.toString();
    }

    private KpiAnalysisResult parseAnalysisResult(String rawJson, String kpiName) {
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("[KpiEnrichment] Gemini returned empty response for KPI '{}'", kpiName);
            return buildFallbackResult(kpiName);
        }

        try {
            // Strip markdown code blocks if present
            String cleaned = rawJson.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```[a-z]*\\n?", "").replaceAll("```", "").trim();
            }

            JsonNode root = objectMapper.readTree(cleaned);

            KpiAnalysisResult result = new KpiAnalysisResult();
            result.setKpiName(kpiName);
            result.setRiskLevel(textOrNull(root, "riskLevel"));
            result.setRiskJustification(textOrNull(root, "riskJustification"));
            result.setObjectiveReached(root.path("objectiveReached").asBoolean(false));
            result.setImprovementDetected(root.path("improvementDetected").asBoolean(false));
            result.setIssueDetected(textOrNull(root, "issueDetected"));
            result.setCorrectiveAction(textOrNull(root, "correctiveAction"));
            result.setPreventiveAction(textOrNull(root, "preventiveAction"));
            result.setImmediateAction(textOrNull(root, "immediateAction"));
            result.setImmediatePriority(textOrNull(root, "immediatePriority"));
            result.setRequires8d(root.path("requires8d").asBoolean(false));
            result.setAiNote(textOrNull(root, "aiNote"));

            // Serialize eightDDetails as compact JSON string
            JsonNode eightD = root.path("eightDDetails");
            if (!eightD.isMissingNode() && !eightD.isNull()) {
                result.setEightDDetails(objectMapper.writeValueAsString(eightD));
            }

            // Carry through suggested values for RAG update
            JsonNode suggestedDef = root.path("suggestedDefinition");
            if (!suggestedDef.isMissingNode() && !suggestedDef.isNull()) {
                result.setAiNote(result.getAiNote()); // already set
                // Store suggested definition temporarily in riskJustification field is NOT what we want.
                // We will handle it in updateRagIfNeeded via direct node parsing.
            }

            // Attach the raw root node for RAG update (piggyback via a transient field is not ideal;
            // instead we re-parse in updateRagIfNeeded using the raw json)
            result.setEightDDetails(result.isRequires8d() ? result.getEightDDetails() : null);

            return result;

        } catch (Exception ex) {
            log.error("[KpiEnrichment] Failed to parse Gemini response for '{}': {}", kpiName, ex.getMessage());
            return buildFallbackResult(kpiName);
        }
    }

    private KpiAnalysisResult buildFallbackResult(String kpiName) {
        return KpiAnalysisResult.builder()
                .kpiName(kpiName)
                .riskLevel("Modéré")
                .riskJustification("Analyse IA indisponible.")
                .issueDetected(null)
                .correctiveAction("Vérification manuelle recommandée.")
                .preventiveAction("Maintenir la surveillance régulière.")
                .immediateAction("Revoir les données manuellement.")
                .immediatePriority("Moyenne")
                .requires8d(false)
                .eightDDetails(null)
                .aiNote("L'analyse automatique n'a pas pu être générée. Une revue manuelle de ce KPI est recommandée.")
                .build();
    }

    @Transactional
    protected KpiAnalysis saveAnalysis(ImportSession session, String kpiName, KpiAnalysisResult result) {
        // Upsert
        KpiAnalysis analysis = analysisRepository
                .findByImportSessionIdAndKpiName(session.getId(), kpiName)
                .orElse(KpiAnalysis.builder().importSession(session).kpiName(kpiName).build());

        analysis.setRiskLevel(result.getRiskLevel());
        analysis.setRiskJustification(result.getRiskJustification());
        analysis.setObjectiveReached(result.getObjectiveReached());
        analysis.setImprovementDetected(result.getImprovementDetected());
        analysis.setIssueDetected(result.getIssueDetected());
        analysis.setCorrectiveAction(result.getCorrectiveAction());
        analysis.setPreventiveAction(result.getPreventiveAction());
        analysis.setImmediateAction(result.getImmediateAction());
        analysis.setImmediatePriority(result.getImmediatePriority());
        analysis.setRequires8d(result.isRequires8d());
        analysis.setEightDDetails(result.getEightDDetails());
        analysis.setAiNote(result.getAiNote());

        return analysisRepository.save(analysis);
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
