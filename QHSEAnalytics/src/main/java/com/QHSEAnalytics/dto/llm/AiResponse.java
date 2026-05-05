package com.QHSEAnalytics.dto.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = false)
public class AiResponse {

    @JsonProperty("overallScore")
    private Double overallScore;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("kpis")
    private List<KpiInsight> kpis;

    @JsonProperty("recommendations")
    private List<String> recommendations;
}