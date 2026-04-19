package com.QHSEAnalytics.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategorieKpiResponse {
    private Long id;
    private String code;
    private String libelle;
    private String description;
    private int nombreKpisActifs;
}
