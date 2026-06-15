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
public class CategoryScoreDTO {
    private String categoryCode;
    private String categoryLibelle;
    private Integer kpiCount;
    private Integer faibleCount;
    private Integer indetermineCount;
    private Integer modereCount;
    private Integer critiqueCount;
    private Double compositeScore;
    private String compositeLabel;
}
