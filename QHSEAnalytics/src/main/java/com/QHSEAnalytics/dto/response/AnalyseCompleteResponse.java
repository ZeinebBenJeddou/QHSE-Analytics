package com.QHSEAnalytics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyseCompleteResponse {
    private Long importSessionId;
    private Integer periodeN1;
    private Integer periodeN;
    private List<ResultatKpiResponse> analysesKpis;
    private List<AnalyseCategorieResponse> analysesCategories;
    private AnalyseGlobaleResponse analyseGlobale;
}