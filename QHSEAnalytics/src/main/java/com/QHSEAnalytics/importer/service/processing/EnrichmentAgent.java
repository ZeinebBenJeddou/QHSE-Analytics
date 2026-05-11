package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.enums.NiveauVariation;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class EnrichmentAgent {

    private static final double DEFAULT_THRESHOLD = 0.0;
    private static final String CLASSIFICATION_CRITICAL = "CRITICAL";
    private static final String CLASSIFICATION_WARNING  = "WARNING";
    private static final String CLASSIFICATION_OK       = "OK";
    private static final String CLASSIFICATION_UNKNOWN  = "UNKNOWN";

    public List<KpiCalculatedDTO> enrich(List<KpiCalculatedDTO> calculatedData) {
        if (calculatedData == null || calculatedData.isEmpty()) {
            return calculatedData;
        }
        for (KpiCalculatedDTO kpi : calculatedData) {
            enrichKpi(kpi);
        }
        return calculatedData;
    }

    private void enrichKpi(KpiCalculatedDTO kpi) {
        double seuilFaible   = kpi.getSeuilFaible()   != null ? kpi.getSeuilFaible()   : DEFAULT_THRESHOLD;
        double seuilModere   = kpi.getSeuilModere()   != null ? kpi.getSeuilModere()   : DEFAULT_THRESHOLD;
        double seuilCritique = kpi.getSeuilCritique() != null ? kpi.getSeuilCritique() : DEFAULT_THRESHOLD;
        double variation     = kpi.getVariationPercentage() != null ? kpi.getVariationPercentage() : 0.0;

        double degradationMagnitude = computeDegradationMagnitude(variation, kpi.getDirection());
        boolean isImproving         = isImprovement(variation, kpi.getDirection());


        String businessClassification = determineClassification(degradationMagnitude, seuilModere, seuilCritique, isImproving);
        kpi.setBusinessClassification(businessClassification);


        String existing = kpi.getClassification();
        if (existing == null || existing.isBlank() || CLASSIFICATION_UNKNOWN.equals(existing) || "INDETERMINE".equals(existing)) {
            kpi.setClassification(computeLegacyClassification(degradationMagnitude, seuilFaible, seuilModere, seuilCritique, isImproving));
        }

        kpi.setIsAnomaly(detectAnomaly(degradationMagnitude, seuilCritique, businessClassification));
    }


    private double computeDegradationMagnitude(double variation, String direction) {
        if ("LOWER_IS_BETTER".equals(direction)) {

            return Math.max(0.0, variation);
        }
        if ("HIGHER_IS_BETTER".equals(direction)) {

            return Math.max(0.0, -variation);
        }

        return Math.abs(variation);
    }


    private boolean isImprovement(double variation, String direction) {
        if (variation == 0.0) return false;
        if ("LOWER_IS_BETTER".equals(direction)) return variation < 0.0;
        if ("HIGHER_IS_BETTER".equals(direction)) return variation > 0.0;
        return false;
    }

    private String determineClassification(double degradationMagnitude, double seuilModere, double seuilCritique, boolean isImproving) {
        if (isImproving) {
            return CLASSIFICATION_OK;
        }
        if (degradationMagnitude > seuilCritique) {
            return CLASSIFICATION_CRITICAL;
        } else if (degradationMagnitude > seuilModere) {
            return CLASSIFICATION_WARNING;
        } else {
            return CLASSIFICATION_OK;
        }
    }

    private String computeLegacyClassification(double degradationMagnitude, double seuilFaible, double seuilModere, double seuilCritique, boolean isImproving) {
        if (isImproving) {
            return NiveauVariation.FAIBLE.name();
        }
        if (degradationMagnitude >= seuilCritique) {
            return NiveauVariation.CRITIQUE.name();
        } else if (degradationMagnitude >= seuilModere) {
            return NiveauVariation.MODERE.name();
        } else if (degradationMagnitude >= seuilFaible) {
            return NiveauVariation.FAIBLE.name();
        }
        return CLASSIFICATION_UNKNOWN;
    }

    private boolean detectAnomaly(double degradationMagnitude, double seuilCritique, String businessClassification) {
        return CLASSIFICATION_CRITICAL.equals(businessClassification) || degradationMagnitude > seuilCritique;
    }
}
