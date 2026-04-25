package com.QHSEAnalytics.importengine;

import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.enums.QualityStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConfidenceEngine {

    public double computeConfidence(double matchingScore, double dataValidity, double businessCoherence) {
        double confidence = matchingScore * 0.5 + dataValidity * 0.3 + businessCoherence * 0.2;
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    public QualityStatus assessQualityStatus(double confidenceScore) {
        if (confidenceScore >= 0.85) {
            return QualityStatus.OK;
        }
        if (confidenceScore >= 0.65) {
            return QualityStatus.WARNING;
        }
        return QualityStatus.REJECTED;
    }

    public double computeBusinessCoherence(Kpi kpi, Double valeurN1, Double valeurN) {
        if (valeurN1 == null || valeurN == null) {
            return 0.0;
        }

        double coherence = 1.0;
        if (kpi.getUnite() != null) {
            switch (kpi.getUnite()) {
                case POURCENTAGE -> {
                    if (valeurN1 < 0.0 || valeurN1 > 100.0 || valeurN < 0.0 || valeurN > 100.0) {
                        coherence -= 0.4;
                    }
                }
                case NOMBRE, KWH, KG -> {
                    if (valeurN1 < 0.0 || valeurN < 0.0) {
                        coherence -= 0.3;
                    }
                }
                default -> {
                    if (valeurN1.isNaN() || valeurN.isNaN()) {
                        coherence -= 0.5;
                    }
                }
            }
        }

        double variation = Math.abs(valeurN - valeurN1);
        if (variation > Math.max(1.0, Math.abs(valeurN1) * 0.5)) {
            coherence -= 0.2;
        }

        return Math.max(0.0, coherence);
    }
}
