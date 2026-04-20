package com.QHSEAnalytics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApercuResponse {
    private ImportSessionResponse importSession;
    private List<StagingDonneeResponse> donnees;
    private boolean peutConfirmer;
}
