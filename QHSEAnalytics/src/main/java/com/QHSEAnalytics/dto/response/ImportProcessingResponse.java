package com.QHSEAnalytics.dto.response;

import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
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
    
    // Risk Intelligence Fields
    private List<KpiCalculatedDTO> risks;  // Critical/anomalous KPIs
    private Integer riskScore;  // Count of CRITICAL KPIs
}
