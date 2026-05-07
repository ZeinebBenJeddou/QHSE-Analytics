package com.QHSEAnalytics.shared.dto.response;

import com.QHSEAnalytics.shared.enums.StatutNettoyage;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StagingDonneeResponse {
    private Long id;
    private Long kpiId;
    private String kpiNom;
    private String kpiUnite;
    private String categorieCode;
    private String valeurBruteN1;
    private String valeurBruteN;
    private Double valeurN1;
    private Double valeurN;
    private StatutNettoyage statutNettoyage;
    private String noteNettoyage;
}
