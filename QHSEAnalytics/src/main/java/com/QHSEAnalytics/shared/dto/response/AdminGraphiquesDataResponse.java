package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminGraphiquesDataResponse {
    private Map<String, Integer> repartitionNiveaux;
    private Map<String, Integer> kpisCritiquesByCategorie;
    private List<BarreGroupeeData> evolutionParCategorie;
}