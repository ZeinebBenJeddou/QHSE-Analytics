package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategorieKpiResponse {
    private Long id;
    private String code;
    private String libelle;
    private String description;
    private int nombreKpisActifs;
}
