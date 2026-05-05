package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.response.ChartResponseDTO;
import com.QHSEAnalytics.dto.response.ImportProcessingResponse;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
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

    private final ExtractionAgent extractionAgent;
    private final CleaningAgent cleaningAgent;
    private final CalculationAgent calculationAgent;
    private final EnrichmentAgent enrichmentAgent;
    private final RiskDetectionAgent riskDetectionAgent;
    private final VisualizationAgent visualizationAgent;
    private final AnalysisAgent analysisAgent;
    private final MetadataEnrichmentAgent metadataEnrichmentAgent;

    public ImportProcessingResponse process(MultipartFile file, Map<String, Integer> mapping) {
        ExtractionAgent.ExtractionResult result = extractionAgent.extract(file, mapping);
        List<KpiRawDataDTO> rawData = cleaningAgent.clean(result.getRows());
        
        // STEP 1: Calculate KPI metrics
        List<KpiCalculatedDTO> calculatedData = calculationAgent.calculate(rawData);
        
        // STEP 2: Enrich with business classification and anomaly detection
        List<KpiCalculatedDTO> enrichedData = enrichmentAgent.enrich(calculatedData);
        
        // STEP 2.1: Enrich metadata (Category/Definition) via RAG or LLM
        enrichedData = metadataEnrichmentAgent.enrichMetadata(enrichedData);
        
        // STEP 3: Detect risks and anomalies
        RiskDetectionAgent.RiskAnalysisResult riskAnalysis = riskDetectionAgent.detect(enrichedData);
        List<KpiCalculatedDTO> criticalRisks = riskAnalysis.getCriticalKpis();
        Integer riskScore = riskAnalysis.getRiskScore();
        
        // STEP 4: Build visualizations and AI analysis
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
                    log.info("[Orchestrator] AnalysisAgent returned AiResponse with {} KPIs and summary length {}", 
                        aiResponse.getKpis() != null ? aiResponse.getKpis().size() : 0,
                        analyseIa != null ? analyseIa.length() : 0);
                } else {
                    log.warn("[Orchestrator] AnalysisAgent returned null aiResponse");
                }
            } else {
                log.info("[Orchestrator] No valid KPIs for analysis");
            }
        } catch (Exception ex) {
            analyseIa = "Analyse IA temporairement indisponible.";
            log.error("[Orchestrator] Exception during AnalysisAgent.analyzeStrict: {}", ex.getMessage(), ex);
        }

        return ImportProcessingResponse.builder()
                .rawData(rawData)
                .calculatedData(enrichedData)
                .extractionMethod(result.getExtractionMethod())
                .qualityScore(calculateQualityScore(rawData))
                .detectedHeaders(result.getDetectedHeaders())
                .charts(charts)
                .analyseIa(analyseIa)
                .aiResponse(aiResponse)
                .risks(criticalRisks)
                .riskScore(riskScore)
                .build();
    }

    public ImportProcessingResponse preview(MultipartFile file, Map<String, Integer> mapping) {
        ExtractionAgent.ExtractionResult result = extractionAgent.extract(file, mapping);
        List<KpiRawDataDTO> rawData = cleaningAgent.clean(result.getRows());
        List<KpiCalculatedDTO> calculatedData = calculationAgent.calculate(rawData);
        calculatedData = metadataEnrichmentAgent.enrichMetadata(calculatedData);

        return ImportProcessingResponse.builder()
                .rawData(rawData)
                .calculatedData(calculatedData)
                .extractionMethod(result.getExtractionMethod())
                .qualityScore(calculateQualityScore(rawData))
                .detectedHeaders(result.getDetectedHeaders())
                .build();
    }

    private double calculateQualityScore(List<KpiRawDataDTO> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return 0.0;
        }

        double total = rawData.stream()
                .filter(KpiRawDataDTO::isValid)
                .mapToDouble(KpiRawDataDTO::getScoreConfiance)
                .sum();
        long validCount = rawData.stream().filter(KpiRawDataDTO::isValid).count();
        return validCount == 0 ? 0.0 : total / validCount;
    }
}
