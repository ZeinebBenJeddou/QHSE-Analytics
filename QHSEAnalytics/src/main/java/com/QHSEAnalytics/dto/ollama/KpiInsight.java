package com.QHSEAnalytics.dto.ollama;

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
@JsonIgnoreProperties(ignoreUnknown = false)
public class KpiInsight {

    @JsonProperty("name")
    private String name;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("insight")
    private String insight;
}
