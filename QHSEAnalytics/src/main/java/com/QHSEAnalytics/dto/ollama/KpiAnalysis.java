package com.QHSEAnalytics.dto.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiAnalysis {
    @JsonProperty("kpi_id")
    private Long kpiId;

    @JsonProperty("kpi_name")
    private String kpiName;

    @JsonProperty("analyse_ia")
    private String analyseIa;
}
