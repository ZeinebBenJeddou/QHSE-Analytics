package com.QHSEAnalytics.shared.dto.request;

import com.QHSEAnalytics.shared.enums.UniteKpi;
import com.QHSEAnalytics.shared.enums.Direction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateKpiRequest {

    @NotBlank(message = "Le nom du KPI est obligatoire")
    private String nom;

    @NotBlank(message = "La définition du KPI est obligatoire")
    private String definition;

    @NotNull(message = "L'unité du KPI est obligatoire")
    private UniteKpi unite;

    @NotBlank(message = "Le code catégorie est obligatoire")
    private String categorieCode;

    @NotNull(message = "Le seuil faible est obligatoire")
    @Positive(message = "Le seuil faible doit être positif")
    private Double seuilFaible;

    @NotNull(message = "Le seuil modéré est obligatoire")
    @Positive(message = "Le seuil modéré doit être positif")
    private Double seuilModere;

    @NotNull(message = "Le seuil critique est obligatoire")
    @Positive(message = "Le seuil critique doit être positif")
    private Double seuilCritique;

    @NotNull(message = "L'ordre est obligatoire")
    @Positive(message = "L'ordre doit être positif")
    private Integer ordre;


    private Direction direction;


    private Double targetValue;
}
