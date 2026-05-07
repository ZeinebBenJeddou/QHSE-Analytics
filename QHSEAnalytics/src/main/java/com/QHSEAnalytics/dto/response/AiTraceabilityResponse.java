package com.QHSEAnalytics.dto.response;

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
public class AiTraceabilityResponse {

    private String modelName;
    private String generatedAt;
    private List<AiContextSourceResponse> contextSourcesUsed;
    private String schemaVersion;
    private String promptVersion;
    private Long importSessionId;
}
