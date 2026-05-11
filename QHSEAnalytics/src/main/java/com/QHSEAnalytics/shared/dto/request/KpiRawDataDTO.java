package com.QHSEAnalytics.shared.dto.request;

import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class KpiRawDataDTO {

    private int rowIndex;
    private String kpiName;
    private String categorie;
    private String unite;
    private Double valeurN1;
    private Double valeurN;
    private String valeurN1Raw;
    private String valeurNRaw;
    private boolean valid;
    private String validationMessage;
    private String methodeExtraction;
    private Double scoreConfiance;


    private String originalKpiName;


    private String normalizedKpiName;


    private Integer rowQualityScore;


    @Singular
    private List<ImportIssue> issues;
}
