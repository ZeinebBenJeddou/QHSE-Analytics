package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.dto.response.ChartResponseDTO;
import com.QHSEAnalytics.dto.response.ImportProcessingResponse;
import com.QHSEAnalytics.dto.response.ImportQualityReport;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KpiProcessingOrchestratorService {

    private final ExtractionAgent         extractionAgent;
    private final CleaningAgent           cleaningAgent;
    private final CalculationAgent        calculationAgent;
    private final EnrichmentAgent         enrichmentAgent;
    private final RiskDetectionAgent      riskDetectionAgent;
    private final VisualizationAgent      visualizationAgent;
    private final AnalysisAgent           analysisAgent;
    private final MetadataEnrichmentAgent metadataEnrichmentAgent;
    private final QualityReportBuilder    qualityReportBuilder;

    /**
     * Traitement complet (import confirmé) :
     * extraction → nettoyage → calcul → enrichissement → IA → qualityReport.
     *
     * Si qualityReport.blocking = true, l'appelant doit refuser la persistence.
     */
    public ImportProcessingResponse process(MultipartFile file, Map<String, Integer> mapping, boolean allowPartialImport) {
        ExtractionAgent.ExtractionResult result = extractionAgent.extract(file, mapping);
        List<KpiRawDataDTO> rawData = cleaningAgent.clean(result.getRows());

        // Rapport qualité construit avant le reste du pipeline
        ImportQualityReport qualityReport = qualityReportBuilder.build(rawData, allowPartialImport);
        qualityReport.setExtractionIssues(result.getExtractionIssues() == null ? List.of() : List.copyOf(result.getExtractionIssues()));

        List<KpiCalculatedDTO> calculatedData = calculationAgent.calculate(rawData);
        List<KpiCalculatedDTO> enrichedData   = enrichmentAgent.enrich(calculatedData);
        enrichedData = metadataEnrichmentAgent.enrichMetadata(enrichedData);

        RiskDetectionAgent.RiskAnalysisResult riskAnalysis = riskDetectionAgent.detect(enrichedData);
        List<KpiCalculatedDTO> criticalRisks = riskAnalysis.getCriticalKpis();
        Integer riskScore = riskAnalysis.getRiskScore();

        ChartResponseDTO charts = visualizationAgent.build(enrichedData);

        String analyseIa = null;
        com.QHSEAnalytics.dto.llm.AiResponse aiResponse = null;
        try {
            List<KpiCalculatedDTO> validKpis = enrichedData.stream()
                    .filter(k -> !"UNKNOWN".equals(k.getClassification()))
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

    /**
     * Prévisualisation rapide (sans IA globale, sans persistence) :
     * extraction → nettoyage → calcul → métadonnées → qualityReport.
     */
    public ImportProcessingResponse preview(MultipartFile file, Map<String, Integer> mapping) {
        ExtractionAgent.ExtractionResult result = extractionAgent.extract(file, mapping);
        List<KpiRawDataDTO> rawData = cleaningAgent.clean(result.getRows());

        ImportQualityReport qualityReport = qualityReportBuilder.build(rawData, false);
        qualityReport.setExtractionIssues(result.getExtractionIssues() == null ? List.of() : List.copyOf(result.getExtractionIssues()));

        List<KpiCalculatedDTO> calculatedData = calculationAgent.calculate(rawData);
        calculatedData = metadataEnrichmentAgent.enrichMetadata(calculatedData);

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
