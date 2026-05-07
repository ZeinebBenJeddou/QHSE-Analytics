package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultatGlobalResponse {
    private Long importSessionId;
    private Integer periodeN1;
    private Integer periodeN;
    private List<ResultatKpiResponse> resultats;
    private long nombreFaible;
    private long nombreModere;
    private long nombreCritique;
    private String analyseGlobaleIa;
}
