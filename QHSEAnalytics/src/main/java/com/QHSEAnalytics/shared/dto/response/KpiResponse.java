package com.QHSEAnalytics.shared.dto.response;

import com.QHSEAnalytics.shared.enums.UniteKpi;
import com.QHSEAnalytics.shared.enums.Direction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isActive")
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Direction direction;
    private String source;
    private boolean aiEnriched;
}
