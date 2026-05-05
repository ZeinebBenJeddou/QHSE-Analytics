package com.QHSEAnalytics.dto.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpiInsight {

    @JsonProperty("name")
    private String name;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("insight")
    private String insight;

    @JsonProperty("aiNote")
    private String aiNote;

    // Additional enrichment fields (French + English variants)
    @JsonProperty("identificationRisque")
    private String identificationRisque;
    
    @JsonProperty("riskJustification")
    private String riskJustification;

    @JsonProperty("problemeDetecte")
    private String problemeDetecte;
    
    @JsonProperty("issueDetected")
    private String issueDetected;

    @JsonProperty("actionsPreventives")
    private String actionsPreventives;
    
    @JsonProperty("preventiveAction")
    private String preventiveAction;

    @JsonProperty("actionImmediate")
    private String actionImmediate;

    @JsonProperty("prioriteAction")
    private String prioriteAction;

    @JsonProperty("methode8D")
    private String methode8D;

    @JsonProperty("noteFinale")
    private String noteFinale;
}