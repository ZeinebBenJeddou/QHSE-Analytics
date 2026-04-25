package com.QHSEAnalytics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutoImportResultResponse {
    private ImportSessionResponse importSession;
    private List<ResultatKpiResponse> resultats;
    private int nombreRejetes;
}
