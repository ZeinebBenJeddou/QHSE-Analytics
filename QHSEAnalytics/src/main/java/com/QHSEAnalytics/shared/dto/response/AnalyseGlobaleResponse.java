package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyseGlobaleResponse {
    private Long id;
    private Long importSessionId;
    private String synthese;
    private String planActions;
    private LocalDateTime createdAt;
}