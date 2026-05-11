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
    private AiResponse aiResponse;


    private List<KpiCalculatedDTO> risks;
    private Integer riskScore;


    private ImportQualityReport qualityReport;


    private java.util.List<CategoryScoreDTO> categoryScores;
}
