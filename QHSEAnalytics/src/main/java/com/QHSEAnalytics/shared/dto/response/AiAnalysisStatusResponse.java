package com.QHSEAnalytics.shared.dto.response;

import com.QHSEAnalytics.shared.enums.AnalyseStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiAnalysisStatusResponse {
    private AnalyseStatus status;
    private String message;
}
