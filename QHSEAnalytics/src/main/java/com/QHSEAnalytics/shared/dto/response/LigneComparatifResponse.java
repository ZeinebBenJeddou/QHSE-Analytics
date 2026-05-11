package com.QHSEAnalytics.shared.dto.response;

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
public class LigneComparatifResponse {
    private Long kpiId;
    private String kpiNom;
    private String unite;
    private String categorieCode;
    private String categorieLibelle;
    private Double valeurN1;
    private Double valeurN;
    private Double variationAbsolue;
    private Double variationRelative;
    private String niveauVariation;
    private String tendance;

    private String status;
    private String commentaire;


    private String analyseIa;


    private String riskLevel;
    private String riskJustification;
    private String identificationRisque;
    private Boolean objectiveReached;
    private Boolean improvementDetected;
    private String issueDetected;
    private String problemeDetecte;
    private String correctiveAction;
    private String preventiveAction;
    private String actionsPreventives;
    private String immediateAction;
    private String actionImmediate;
    private String immediatePriority;
    private String prioriteAction;
    private Boolean requires8d;
    private String eightDDetails;
    private String methode8D;
    private String aiNote;
    private String noteFinale;
}
