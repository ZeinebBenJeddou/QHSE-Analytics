package com.QHSEAnalytics.dto.response;

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
public class BarreGroupeeData {
    private String categorie;
    private Double valeurMoyenneN1;
    private Double valeurMoyenneN;
    private Double variationMoyenne;
}