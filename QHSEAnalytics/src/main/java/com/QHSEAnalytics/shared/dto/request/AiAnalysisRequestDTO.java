package com.QHSEAnalytics.shared.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiAnalysisRequestDTO {
    private Long importSessionId;
    private Integer periodeN1;
    private Integer periodeN;
    private List<KpiRawDataDTO> kpiRows;
}
