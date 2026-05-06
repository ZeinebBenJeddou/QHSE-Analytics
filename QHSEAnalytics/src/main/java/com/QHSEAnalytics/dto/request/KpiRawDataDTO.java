package com.QHSEAnalytics.dto.request;

import com.QHSEAnalytics.dto.response.ImportIssue;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class KpiRawDataDTO {
    // ── Champs existants (inchangés) ──────────────────────────────
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

    // ── Nouveaux champs qualité ───────────────────────────────────
    /** Libellé KPI affiché tel quel (avant normalisation). */
    private String originalKpiName;

    /** Libellé normalisé (trim, accents, casse) utilisé pour le matching. */
    private String normalizedKpiName;

    /** Score qualité de la ligne : 100 = OK, 70 = warning, 0 = invalide. */
    private Integer rowQualityScore;

    /** Issues associées à cette ligne (ERROR / WARNING / INFO). */
    @Singular
    private List<ImportIssue> issues;
}
