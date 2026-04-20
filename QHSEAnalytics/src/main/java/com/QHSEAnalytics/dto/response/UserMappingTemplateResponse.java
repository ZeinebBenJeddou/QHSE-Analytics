package com.QHSEAnalytics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserMappingTemplateResponse {
    private Long id;
    private String nom;
    private Integer ligneEntete;
    private LocalDateTime createdAt;
    private List<ColonneMappingResponse> colonnes;
}
