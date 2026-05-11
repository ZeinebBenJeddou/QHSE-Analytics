package com.QHSEAnalytics.shared.dto.request;

import com.QHSEAnalytics.shared.entity.UniteKpi;
import com.QHSEAnalytics.shared.enums.Direction;
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


    private Direction direction;

    private Double targetValue;
}
