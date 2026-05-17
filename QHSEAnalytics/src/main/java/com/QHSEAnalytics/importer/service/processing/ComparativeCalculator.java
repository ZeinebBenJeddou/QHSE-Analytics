package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.enums.DataFlag;
import com.QHSEAnalytics.shared.enums.Direction;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ComparativeCalculator {

    @Data
    @Builder
    public static class ComparativeResult {
        private double absoluteGap;
        private Double relativePercentage;
        private Direction direction;
        private Double previousValue;
        private Double currentValue;
        private int calcConfidence;
        private boolean reviewRequired;
        private List<DataFlag> dataFlags;
        private String specialCase;
    }

    public ComparativeResult compute(Kpi kpi, Double valN1, Double valN) {
        ComparativeResult.ComparativeResultBuilder b = ComparativeResult.builder();
        double absGap = 0.0;
        List<DataFlag> flags = new ArrayList<>();
        Direction direction = resolveDirection(kpi);

        if (valN != null && valN1 != null) {
            absGap = valN - valN1;
        } else {
            flags.add(DataFlag.MISSING_CONTEXT);
        }

        Double rel = null;
        String special = null;
        if (valN1 == null) {
            if (valN == null || valN == 0d) {
                special = "STABLE";
            }
        } else if (valN1 == 0d) {
            if (valN == null || valN == 0d) {
                special = "STABLE";
            } else {
                // For LOWER_IS_BETTER KPIs a jump from 0 is a new risk; for HIGHER_IS_BETTER it is improvement.
                if (direction == Direction.HIGHER_IS_BETTER) {
                    special = "STRONG_IMPROVEMENT";
                } else {
                    special = "EMERGING_RISK";
                    flags.add(DataFlag.LOW_BASE);
                }
            }

        } else {
            if (valN != null) {
                rel = ((valN - valN1) / Math.abs(valN1)) * 100.0;
                if (valN == 0d) {
                    special = "STRONG_IMPROVEMENT";
                }
            }
        }

        if (rel != null && Math.abs(rel) >= 300d) {
            flags.add(DataFlag.OUTLIER);
        }



        int confidence = 60;
        if (kpi != null) confidence += 20;
        if (flags.contains(DataFlag.MISSING_CONTEXT)) confidence -= 25;
        if (flags.contains(DataFlag.LOW_BASE)) confidence -= 20;
        if (flags.contains(DataFlag.OUTLIER)) confidence -= 10;
        if (valN1 != null && Math.abs(valN1) > 0 && Math.abs(valN1) < 1e-9) confidence -= 10;
        confidence = Math.max(0, Math.min(100, confidence));

        boolean review = confidence < 60 || flags.contains(DataFlag.MISSING_CONTEXT);

        return b.absoluteGap(absGap)
                .relativePercentage(rel)
                .direction(direction)
                .previousValue(valN1)
                .currentValue(valN)
                .calcConfidence(confidence)
                .reviewRequired(review)
                .dataFlags(flags)
                .specialCase(special)
                .build();
    }

    private Direction resolveDirection(Kpi kpi) {
        Direction explicit = resolveExplicitDirection(kpi);
        if (explicit != null) {
            return explicit;
        }

        Direction categoryDirection = resolveFromCategory(kpi);
        if (categoryDirection != null) {
            return categoryDirection;
        }

        Direction nameDirection = resolveFromName(kpi != null ? kpi.getNom() : null);
        if (nameDirection != null) {
            return nameDirection;
        }

        return Direction.HIGHER_IS_BETTER;
    }

    private Direction resolveExplicitDirection(Kpi kpi) {
        return kpi == null ? null : kpi.getDirection();
    }

    private Direction resolveFromCategory(Kpi kpi) {
        if (kpi == null || kpi.getCategorieKpi() == null) {
            return null;
        }
        String label = kpi.getCategorieKpi().getLibelle();
        String code = kpi.getCategorieKpi().getCode();
        Direction byLabel = inferDirectionFromText(label);
        if (byLabel != null) {
            return byLabel;
        }
        return inferDirectionFromText(code);
    }

    private Direction resolveFromName(String kpiName) {
        return inferDirectionFromText(kpiName);
    }

    private Direction inferDirectionFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String n = normalize(text);
        if (containsAny(n, "cible", "objectif", "target")) {
            return Direction.TARGET_IS_BEST;
        }
        if (containsAny(n, "accident", "incident", "nc", "nonconform", "reclamation", "rejet", "defaut", "retard", "anomal")) {
            return Direction.LOWER_IS_BETTER;
        }
        if (containsAny(n, "conformite", "tauxdereussite", "reussite", "disponibilite", "satisfaction", "performance")) {
            return Direction.HIGHER_IS_BETTER;
        }
        return null;
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]", "");
    }
}
