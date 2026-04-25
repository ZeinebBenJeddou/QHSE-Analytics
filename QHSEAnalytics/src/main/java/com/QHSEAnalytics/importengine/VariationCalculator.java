package com.QHSEAnalytics.importengine;

import com.QHSEAnalytics.enums.Tendance;
import org.springframework.stereotype.Service;

@Service
public class VariationCalculator {

    public double calculateAbsolute(Double valeurN1, Double valeurN) {
        if (valeurN1 == null || valeurN == null) {
            return 0.0;
        }
        return valeurN - valeurN1;
    }

    public double calculateRelative(Double valeurN1, Double valeurN) {
        if (valeurN1 == null || valeurN == null) {
            return 0.0;
        }
        if (valeurN1 == 0.0) {
            return valeurN == 0.0 ? 0.0 : 100.0;
        }
        return ((valeurN - valeurN1) / Math.abs(valeurN1)) * 100.0;
    }

    public Tendance determineTendance(double variationRelative) {
        if (variationRelative > 0.5) {
            return Tendance.HAUSSE;
        }
        if (variationRelative < -0.5) {
            return Tendance.BAISSE;
        }
        return Tendance.STABLE;
    }
}
