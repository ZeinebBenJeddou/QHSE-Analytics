package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiKpiInsightResponse {

    private Long kpiId;
    @EqualsAndHashCode.Include
    private String kpiName;
    private Double confidence;
    private String insight;
    private List<String> probableCauses;
    private List<String> recommendations;
    private String actionImmediate;
    private String urgency;
    private String ownerRole;
    private String dueHorizon;
    private String successMetric;
    private String riskIfNotDone;
    private String note;
}
