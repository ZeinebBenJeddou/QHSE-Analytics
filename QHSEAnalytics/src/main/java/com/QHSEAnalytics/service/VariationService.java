package com.QHSEAnalytics.service;

import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.enums.Tendance;
import org.springframework.stereotype.Service;

@Service
public class VariationService {

    public double calculerVariationAbsolue(double n1, double n) {
        return n - n1;
    }

    public double calculerVariationRelative(double n1, double n) {
        if (n1 == 0.0d) {
            return n > 0.0d ? 100.0d : 0.0d;
        }
        return ((n - n1) / n1) * 100.0d;
    }

    public NiveauVariation classifierVariation(double variationRelative, Kpi kpi) {
        double abs = Math.abs(variationRelative);
        if (abs < kpi.getSeuilFaible()) {
            return NiveauVariation.FAIBLE;
        }
        if (abs < kpi.getSeuilModere()) {
            return NiveauVariation.MODERE;
        }
        return NiveauVariation.CRITIQUE;
    }

    public Tendance determinerTendance(double variationRelative) {
        if (variationRelative > 1.0d) {
            return Tendance.HAUSSE;
        }
        if (variationRelative < -1.0d) {
            return Tendance.BAISSE;
        }
        return Tendance.STABLE;
    }
}
