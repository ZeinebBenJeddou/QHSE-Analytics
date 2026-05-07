package com.QHSEAnalytics.shared.dto.response;

import com.QHSEAnalytics.shared.dto.llm.AiResponse;
import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportProcessingResponse {
    private Long importSessionId;
    private List<KpiCalculatedDTO> calculatedData;
    private List<KpiRawDataDTO> rawData;
    private String extractionMethod;
    private Double qualityScore;
    private List<String> detectedHeaders;
    private ChartResponseDTO charts;
    private String analyseIa;
    private AiResponse aiResponse;  // Full LLM response with per-KPI insights

    // Risk Intelligence Fields
    private List<KpiCalculatedDTO> risks;  // Critical/anomalous KPIs
    private Integer riskScore;  // Count of CRITICAL KPIs

    // Quality Report — rapport détaillé d'import (preview + import confirmé)
    private ImportQualityReport qualityReport;

    // P2.3 — Composite scores per QHSE category
    private java.util.List<CategoryScoreDTO> categoryScores;
}
