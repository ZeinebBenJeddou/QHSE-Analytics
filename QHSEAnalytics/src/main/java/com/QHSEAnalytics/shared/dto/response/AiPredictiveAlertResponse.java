package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiPredictiveAlertResponse {

    /** Nom du KPI concerné */
    private String kpiRef;

    /** Description de la projection : "Si la tendance continue, X atteindra le seuil critique" */
    private String projection;

    /** Dans combien de mois le KPI atteindra le seuil CRITIQUE (0-24) */
    private Integer estimatedHorizonMonths;

    /** Confiance de la projection (0-100) */
    private Double confidence;

    /** Sévérité de l'alerte : LOW | MEDIUM | HIGH */
    private String severity;
}
