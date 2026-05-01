package com.QHSEAnalytics.dto.request;

import lombok.Builder;
import lombok.Data;

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
}
