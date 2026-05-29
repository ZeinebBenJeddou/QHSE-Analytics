package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiAnalysisStructuredResponse {

    private String globalSummary;
    private AiConfidenceResponse confidence;
    private List<AiKpiInsightResponse> kpiInsights;
    private List<String> probableCauses;
    private List<AiRecommendationResponse> recommendations;
    private List<AiActionPlanItemResponse> actionPlan;
    private AiTraceabilityResponse traceability;
    private String status;
    private String fallbackReason;
    private String schemaVersion;
    private String promptVersion;
    private Long importSessionId;
}
