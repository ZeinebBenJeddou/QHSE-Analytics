package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiActionPlanItemResponse {

    private String action;
    private String priority;
    private String ownerRole;
    private String dueHorizon;
    private String successMetric;
    private String riskIfNotDone;
}
