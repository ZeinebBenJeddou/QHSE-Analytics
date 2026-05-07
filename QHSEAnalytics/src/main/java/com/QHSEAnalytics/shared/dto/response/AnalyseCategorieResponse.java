package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyseCategorieResponse {
    private Long id;
    private Long importSessionId;
    private String categorieCode;
    private String categorieLibelle;
    private String contenu;
    private LocalDateTime createdAt;
}