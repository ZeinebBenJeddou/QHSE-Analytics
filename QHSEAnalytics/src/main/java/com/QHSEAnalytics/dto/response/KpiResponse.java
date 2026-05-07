package com.QHSEAnalytics.dto.response;

import com.QHSEAnalytics.entity.UniteKpi;
import com.QHSEAnalytics.enums.Direction;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KpiResponse {
    private Long id;
    private String nom;
    private String definition;
    private UniteKpi unite;
    private String categorieCode;
    private String categorieLibelle;
    private Double seuilFaible;
    private Double seuilModere;
    private Double seuilCritique;
    private Integer ordre;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Direction direction;
    private Double targetValue;
}
