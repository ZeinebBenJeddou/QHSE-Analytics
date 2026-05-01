package com.QHSEAnalytics.dto.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.StringJoiner;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaAnalysisResult {
    private String interpretation;

    @JsonProperty("kpis_critiques")
    private List<String> kpisCritiques;

    private List<String> causes;
    private List<String> recommandations;

    @JsonProperty("plan_action")
    private List<String> planAction;

    public String toSummaryString() {
        StringJoiner joiner = new StringJoiner("\n\n");

        if (interpretation != null && !interpretation.isBlank()) {
            joiner.add("Interprétation :\n" + interpretation);
        }
        if (kpisCritiques != null && !kpisCritiques.isEmpty()) {
            joiner.add("KPIs critiques :\n- " + String.join("\n- ", kpisCritiques));
        }
        if (causes != null && !causes.isEmpty()) {
            joiner.add("Causes :\n- " + String.join("\n- ", causes));
        }
        if (recommandations != null && !recommandations.isEmpty()) {
            joiner.add("Recommandations :\n- " + String.join("\n- ", recommandations));
        }
        if (planAction != null && !planAction.isEmpty()) {
            joiner.add("Plan d'action :\n- " + String.join("\n- ", planAction));
        }

        return joiner.toString();
    }
}
