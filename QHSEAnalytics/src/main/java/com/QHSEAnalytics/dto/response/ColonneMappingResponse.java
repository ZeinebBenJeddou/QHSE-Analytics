package com.QHSEAnalytics.dto.response;

import com.QHSEAnalytics.enums.TypeValeur;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColonneMappingResponse {
    private Long id;
    private String nomColonne;
    private Integer indexColonne;
    private Long kpiId;
    private String kpiNom;
    private TypeValeur typeValeur;
}
