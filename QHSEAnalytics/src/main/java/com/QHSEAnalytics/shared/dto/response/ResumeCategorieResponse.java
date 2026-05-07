package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumeCategorieResponse {
    private String categorieCode;
    private String categorieLibelle;
    private Double variationMoyenne;
    private int nombreKpisCritiques;
    private int nombreKpisModeres;
    private int nombreKpisFaibles;
    private String couleur;
}