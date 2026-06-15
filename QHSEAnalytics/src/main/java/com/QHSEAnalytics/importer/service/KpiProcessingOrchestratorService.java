package com.QHSEAnalytics.importer.service;

import com.QHSEAnalytics.analytics.service.processing.AnalysisAgent;
import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.ChartResponseDTO;
import com.QHSEAnalytics.shared.dto.response.ImportProcessingResponse;
import com.QHSEAnalytics.shared.dto.response.ImportQualityReport;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.enums.KpiMatchingType;
import com.QHSEAnalytics.importer.service.processing.CalculationAgent;
import com.QHSEAnalytics.importer.service.processing.CleaningAgent;
import com.QHSEAnalytics.importer.service.processing.ExtractionAgent;
import com.QHSEAnalytics.importer.service.processing.MetadataEnrichmentAgent;
import com.QHSEAnalytics.importer.service.processing.QualityReportBuilder;
import com.QHSEAnalytics.importer.service.processing.RiskDetectionAgent;
import com.QHSEAnalytics.importer.service.processing.VisualizationAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class KpiProcessingOrchestratorService {

    private final ExtractionAgent         extractionAgent;
    private final CleaningAgent           cleaningAgent;
    private final CalculationAgent        calculationAgent;
    private final RiskDetectionAgent      riskDetectionAgent;
    private final VisualizationAgent      visualizationAgent;
    private final AnalysisAgent           analysisAgent;
    private final MetadataEnrichmentAgent metadataEnrichmentAgent;
    private final QualityReportBuilder    qualityReportBuilder;

    private static final AtomicLong SESSION_COUNTER = new AtomicLong(0);


    public ImportProcessingResponse process(MultipartFile file, Map<String, Integer> mapping, boolean allowPartialImport) {
        return process(file, mapping, allowPartialImport, false);
    }

    public ImportProcessingResponse process(MultipartFile file, Map<String, Integer> mapping, boolean allowPartialImport, boolean hasAnalysteContext) {
        long sessionId = SESSION_COUNTER.incrementAndGet();
        long sessionStart = System.currentTimeMillis();
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "inconnu";

        log.info("\n═══════════════════════════════════════════════════" +
                 "\n[QHSE-EVAL] IMPORT SESSION #{} — {}" +
                 "\n═══════════════════════════════════════════════════",
                sessionId, fileName);

        ExtractionAgent.ExtractionResult result = extractionAgent.extract(file, mapping);
        List<KpiRawDataDTO> rawData = cleaningAgent.clean(result.getRows());


        ImportQualityReport qualityReport = qualityReportBuilder.build(rawData, allowPartialImport);
        qualityReport.setExtractionIssues(result.getExtractionIssues() == null ? List.of() : List.copyOf(result.getExtractionIssues()));

        List<KpiCalculatedDTO> calculatedData = calculationAgent.calculate(rawData);
        List<KpiCalculatedDTO> nonRecognizedData = calculatedData.stream()
                .filter(k -> k.getMatchingType() == KpiMatchingType.NON_RECONNU)
                .toList();
        metadataEnrichmentAgent.enrichMetadata(nonRecognizedData);
        List<KpiCalculatedDTO> enrichedData = calculatedData;

        RiskDetectionAgent.RiskAnalysisResult riskAnalysis = riskDetectionAgent.detect(enrichedData);
        List<KpiCalculatedDTO> criticalRisks = riskAnalysis.getCriticalKpis();
        Integer riskScore = riskAnalysis.getRiskScore();

        ChartResponseDTO charts = visualizationAgent.build(enrichedData);

        String analyseIa = null;
        com.QHSEAnalytics.shared.dto.llm.AiResponse aiResponse = null;
        try {
            List<KpiCalculatedDTO> validKpis = enrichedData.stream()
                    .filter(k -> k.getClassification() != null
                            && !"UNKNOWN".equals(k.getClassification())
                            && !"INDETERMINE".equals(k.getClassification())
                            && k.getVariationPercentage() != null)
                    .toList();
            if (!validKpis.isEmpty()) {
                aiResponse = analysisAgent.analyzeStrict(validKpis);
                if (aiResponse != null) {
                    analyseIa = aiResponse.getSummary();
                    log.info("[Orchestrator] AiResponse : {} KPIs, summary length {}",
                            aiResponse.getKpis() != null ? aiResponse.getKpis().size() : 0,
                            analyseIa != null ? analyseIa.length() : 0);
                } else {
                    log.warn("[Orchestrator] AnalysisAgent returned null aiResponse");
                }
            } else {
                log.info("[Orchestrator] Aucun KPI valide pour l'analyse IA");
            }
        } catch (Exception ex) {
            analyseIa = "Analyse IA temporairement indisponible.";
            log.error("[Orchestrator] Exception AnalysisAgent : {}", ex.getMessage(), ex);
        }

        long totalMs = System.currentTimeMillis() - sessionStart;

        int critiqueCount = (int) enrichedData.stream()
                .filter(k -> "CRITIQUE".equals(k.getClassification()))
                .count();
        int aiConfidence = 0;
        if (aiResponse != null && aiResponse.getOverallScore() != null) {
            aiConfidence = (int) Math.round(aiResponse.getOverallScore());
        }
        String contexte = hasAnalysteContext ? "PRÉSENT" : "ABSENT";

        log.info("\n═══════════════════════════════════════════════════" +
                 "\n[SYNTHÈSE SESSION #{}]" +
                 "\n  Fichier        : {}" +
                 "\n  KPIs traités   : {}" +
                 "\n  Score qualité  : {}/100" +
                 "\n  Score risque   : {}/100" +
                 "\n  KPIs CRITIQUE  : {}" +
                 "\n  Contexte       : {}" +
                 "\n  Confiance IA   : {}/100" +
                 "\n  Durée totale   : {} ms" +
                 "\n═══════════════════════════════════════════════════",
                sessionId, fileName,
                enrichedData.size(),
                qualityReport.getQualityScore(),
                riskScore != null ? riskScore : 0,
                critiqueCount,
                contexte,
                aiConfidence,
                totalMs);

        return ImportProcessingResponse.builder()
                .rawData(rawData)
                .calculatedData(enrichedData)
                .extractionMethod(result.getExtractionMethod())
                .qualityScore(qualityReport.getQualityScore())
                .detectedHeaders(result.getDetectedHeaders())
                .charts(charts)
                .analyseIa(analyseIa)
                .aiResponse(aiResponse)
                .risks(criticalRisks)
                .riskScore(riskScore)
                .qualityReport(qualityReport)
                .build();
    }



    public ImportProcessingResponse preview(MultipartFile file, Map<String, Integer> mapping) {
        ExtractionAgent.ExtractionResult result = extractionAgent.extract(file, mapping);
        List<KpiRawDataDTO> rawData = cleaningAgent.clean(result.getRows());

        ImportQualityReport qualityReport = qualityReportBuilder.build(rawData, false);
        qualityReport.setExtractionIssues(result.getExtractionIssues() == null ? List.of() : List.copyOf(result.getExtractionIssues()));

        List<KpiCalculatedDTO> calculatedData = calculationAgent.calculate(rawData);
        List<KpiCalculatedDTO> nonRecognizedData = calculatedData.stream()
                .filter(k -> k.getMatchingType() == KpiMatchingType.NON_RECONNU)
                .toList();
        metadataEnrichmentAgent.enrichMetadata(nonRecognizedData);

        return ImportProcessingResponse.builder()
                .rawData(rawData)
                .calculatedData(calculatedData)
                .extractionMethod(result.getExtractionMethod())
                .qualityScore(qualityReport.getQualityScore())
                .detectedHeaders(result.getDetectedHeaders())
                .qualityReport(qualityReport)
                .build();
    }
}
