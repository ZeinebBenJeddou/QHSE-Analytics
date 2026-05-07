package com.QHSEAnalytics.dto.request;

import com.QHSEAnalytics.entity.UniteKpi;
import com.QHSEAnalytics.enums.Direction;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateKpiRequest {

    private String nom;
    private String definition;
    private UniteKpi unite;
    private String categorieCode;

    @Positive(message = "Le seuil faible doit être positif")
    private Double seuilFaible;

    @Positive(message = "Le seuil modéré doit être positif")
    private Double seuilModere;

    @Positive(message = "Le seuil critique doit être positif")
    private Double seuilCritique;

    @Positive(message = "L'ordre doit être positif")
    private Integer ordre;

    /**
     * Optional: explicit direction for the KPI (HIGHER_IS_BETTER, LOWER_IS_BETTER, TARGET_IS_BEST).
     */
    private Direction direction;

    /**
     * Optional: explicit target value for TARGET_IS_BEST KPIs.
     */
    private Double targetValue;
}
