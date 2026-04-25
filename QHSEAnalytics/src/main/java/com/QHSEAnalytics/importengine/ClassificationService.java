package com.QHSEAnalytics.importengine;

import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.enums.NiveauVariation;
import org.springframework.stereotype.Service;

@Service
public class ClassificationService {

    public NiveauVariation classify(Kpi kpi, double variationRelative) {
        if (kpi == null) {
            return NiveauVariation.FAIBLE;
        }

        double magnitude = Math.abs(variationRelative);
        if (magnitude < kpi.getSeuilFaible()) {
            return NiveauVariation.FAIBLE;
        }
        if (magnitude < kpi.getSeuilModere()) {
            return NiveauVariation.MODERE;
        }
        return NiveauVariation.CRITIQUE;
    }
}
