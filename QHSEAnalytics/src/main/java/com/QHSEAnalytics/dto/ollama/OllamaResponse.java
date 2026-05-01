package com.QHSEAnalytics.dto.ollama;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaResponse {
    @JsonProperty("overallScore")
    private Double overallScore;

    @JsonProperty("globalSummary")
    private String globalSummary;

    @JsonProperty("criticalKPIs")
    private List<String> criticalKPIs;

    @JsonProperty("recommendations")
    private List<String> recommendations;

    @JsonProperty("categories")
    private List<CategorieAnalysis> categories;

    @JsonProperty("kpis_analyses")
    private List<KpiAnalysis> kpisAnalyses;

    @JsonProperty("interpretation")
    private String interpretation;

    @JsonProperty("kpis_critiques")
    private List<String> kpisCritiques;

    private List<String> causes;

    @JsonProperty("recommandations")
    private List<String> recommandations;

    @JsonProperty("plan_action")
    private List<String> planAction;
}
