package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.enums.Direction;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassificationEngine {

    private record DegradationContext(double magnitude, boolean targetFallbackUsed) {
    }

    @Data
    public static class ClassificationResult {
        private String classification;
        private String reason;
        private boolean reviewRequired;
    }

    private static final String EXCELLENT    = "EXCELLENT";
    private static final String CRITIQUE     = "CRITIQUE";
    private static final String PRE_ESCALADE = "PRE_ESCALADE";
    private static final String MODERE       = "MODERE";
    private static final String FAIBLE       = "FAIBLE";
    private static final String INDETERMINE  = "INDETERMINE";

    public ClassificationResult classify(Kpi kpi, ComparativeCalculator.ComparativeResult comp, List<Double> historical) {
        ClassificationResult r = new ClassificationResult();
        if (comp == null) {
            r.setClassification(INDETERMINE);
            r.setReason("Comparatif manquant");
            r.setReviewRequired(true);
            return r;
        }

        if ("STABLE".equals(comp.getSpecialCase())) {
            r.setClassification(FAIBLE);
            r.setReason("Base N-1 nulle et valeur stable (0 -> 0)");
            r.setReviewRequired(comp.isReviewRequired());
            return r;
        }
        if ("EMERGING_RISK".equals(comp.getSpecialCase())) {
            r.setClassification(MODERE);
            r.setReason("Risque émergent: passage de 0 vers une valeur non nulle");
            r.setReviewRequired(true);
            return r;
        }
        if ("STRONG_IMPROVEMENT".equals(comp.getSpecialCase())) {
            r.setClassification(FAIBLE);
            r.setReason("Amélioration forte: passage vers 0");
            r.setReviewRequired(comp.isReviewRequired());
            return r;
        }

        boolean hasThresholds = hasThresholds(kpi);
        DegradationContext degradationContext = computeDegradationContext(kpi, comp);
        double degradationMagnitude = degradationContext.magnitude();
        boolean hasDegradation = degradationMagnitude > 0;
        double degradationScore = computeDegradationScore(kpi, comp, degradationMagnitude);
        boolean forceReviewForTargetFallback = degradationContext.targetFallbackUsed();

        if (hasThresholds) {
            double faible = safe(kpi.getSeuilFaible());
            double modere = safe(kpi.getSeuilModere());
            double critique = safe(kpi.getSeuilCritique());

            if (!hasDegradation) {

                double improvementMagnitude = computeImprovementMagnitude(kpi, comp);
                if (improvementMagnitude >= faible * 1.5 && faible > 0
                        && resolveDirection(kpi, comp) != Direction.TARGET_IS_BEST) {
                    r.setClassification(EXCELLENT);
                    r.setReason(withTargetFallbackNote("Performance excellente — amélioration significative au-delà des seuils", forceReviewForTargetFallback));
                    r.setReviewRequired(false);
                    return r;
                }
                r.setClassification(FAIBLE);
                r.setReason(withTargetFallbackNote("Variation orientée amélioration selon la direction métier", forceReviewForTargetFallback));
                r.setReviewRequired(comp.isReviewRequired() || forceReviewForTargetFallback);
                return r;
            }

            if (degradationMagnitude >= critique || degradationScore >= 85) {
                r.setClassification(CRITIQUE);
                r.setReason(withTargetFallbackNote("Dégradation critique selon les seuils KPI", forceReviewForTargetFallback));
                r.setReviewRequired(true);
                return r;
            }

            if (degradationScore >= 72 || (critique > 0 && degradationMagnitude >= critique * 0.75 && degradationMagnitude < critique)) {
                r.setClassification(PRE_ESCALADE);
                r.setReason(withTargetFallbackNote("Dégradation pré-critique — surveillance immédiate requise", forceReviewForTargetFallback));
                r.setReviewRequired(true);
                return r;
            }
            if (degradationMagnitude >= modere || degradationScore >= 60) {
                r.setClassification(MODERE);
                r.setReason(withTargetFallbackNote("Dégradation modérée selon les seuils KPI", forceReviewForTargetFallback));
                r.setReviewRequired(comp.isReviewRequired() || degradationScore >= 70 || forceReviewForTargetFallback);
                return r;
            }
            if (degradationMagnitude >= faible || degradationScore >= 35) {
                r.setClassification(FAIBLE);
                r.setReason(withTargetFallbackNote("Dégradation faible mais surveillée", forceReviewForTargetFallback));
                r.setReviewRequired(comp.isReviewRequired() || forceReviewForTargetFallback);
                return r;
            }

            r.setClassification(FAIBLE);
            r.setReason(withTargetFallbackNote("Dans les seuils attendus", forceReviewForTargetFallback));
            r.setReviewRequired(comp.isReviewRequired() || forceReviewForTargetFallback);
            return r;
        }

        List<Double> history = historical == null ? List.of() : historical;
        if (history.size() >= 10) {
            List<Double> historyDegradations = toDegradationSeries(kpi, comp, history);
            double p95 = percentile(historyDegradations, 95);
            double p75 = percentile(historyDegradations, 75);
            double p25 = percentile(historyDegradations, 25);

            if (!hasDegradation && p25 > 0 && degradationMagnitude == 0
                    && resolveDirection(kpi, comp) != Direction.TARGET_IS_BEST) {
                double improvementMag = computeImprovementMagnitude(kpi, comp);
                if (improvementMag > p75) {
                    r.setClassification(EXCELLENT);
                    r.setReason(withTargetFallbackNote("Performance excellente dans le contexte historique (amélioration > p75)", forceReviewForTargetFallback));
                    r.setReviewRequired(false);
                    return r;
                }
            }
            if (degradationMagnitude > p95) {
                r.setClassification(CRITIQUE);
                r.setReason(withTargetFallbackNote("Dégradation hors distribution historique (p95)", forceReviewForTargetFallback));
                r.setReviewRequired(true);
                return r;
            }
            if (degradationMagnitude > p75 * 0.85) {
                r.setClassification(PRE_ESCALADE);
                r.setReason(withTargetFallbackNote("Dégradation pré-critique dans l'historique (approche p75)", forceReviewForTargetFallback));
                r.setReviewRequired(true);
                return r;
            }
            if (degradationMagnitude > p75) {
                r.setClassification(MODERE);
                r.setReason(withTargetFallbackNote("Dégradation notable dans l'historique (p75)", forceReviewForTargetFallback));
                r.setReviewRequired(comp.isReviewRequired() || forceReviewForTargetFallback);
                return r;
            }
            r.setClassification(FAIBLE);
            r.setReason(withTargetFallbackNote("Dégradation contenue dans l'historique", forceReviewForTargetFallback));
            r.setReviewRequired(comp.isReviewRequired() || forceReviewForTargetFallback);
            return r;
        }

        if (history.size() >= 5) {
            List<Double> historyDegradations = toDegradationSeries(kpi, comp, history);
            double robustZ = robustZScore(historyDegradations, degradationMagnitude);
            if (robustZ >= 3.0) {
                r.setClassification(CRITIQUE);
                r.setReason(withTargetFallbackNote("Dégradation atypique détectée (z robuste)", forceReviewForTargetFallback));
                r.setReviewRequired(true);
                return r;
            }
            if (robustZ >= 2.4) {
                r.setClassification(PRE_ESCALADE);
                r.setReason(withTargetFallbackNote("Dégradation pré-critique (z robuste ≥ 2.4)", forceReviewForTargetFallback));
                r.setReviewRequired(true);
                return r;
            }
            if (robustZ >= 1.8) {
                r.setClassification(MODERE);
                r.setReason(withTargetFallbackNote("Dégradation au-dessus du comportement normal (z robuste)", forceReviewForTargetFallback));
                r.setReviewRequired(comp.isReviewRequired() || forceReviewForTargetFallback);
                return r;
            }

            if (!hasDegradation && resolveDirection(kpi, comp) != Direction.TARGET_IS_BEST) {
                double improvementZ = robustZScore(historyDegradations, computeImprovementMagnitude(kpi, comp));
                if (improvementZ >= 2.0) {
                    r.setClassification(EXCELLENT);
                    r.setReason(withTargetFallbackNote("Performance excellente (z robuste amélioration ≥ 2.0)", forceReviewForTargetFallback));
                    r.setReviewRequired(false);
                    return r;
                }
            }
            r.setClassification(FAIBLE);
            r.setReason(withTargetFallbackNote("Dégradation non atypique selon l'historique court", forceReviewForTargetFallback));
            r.setReviewRequired(comp.isReviewRequired() || forceReviewForTargetFallback);
            return r;
        }

        r.setClassification(INDETERMINE);
        r.setReason(withTargetFallbackNote("Pas de seuils ni historique suffisant", forceReviewForTargetFallback));
        r.setReviewRequired(true);
        return r;
    }

    private boolean hasThresholds(Kpi kpi) {
        return kpi != null && kpi.getSeuilFaible() != null && kpi.getSeuilModere() != null && kpi.getSeuilCritique() != null;
    }

    private double computeDegradationScore(Kpi kpi, ComparativeCalculator.ComparativeResult comp, double degradationMagnitude) {
        double relative = comp.getRelativePercentage() == null ? 0d : Math.abs(comp.getRelativePercentage());
        double absolute = Math.abs(comp.getAbsoluteGap());
        double score = 0d;

        if (degradationMagnitude <= 0) {
            return 0d;
        }

        score += Math.min(55d, relative * 0.55d);
        score += Math.min(30d, Math.log10(1d + absolute) * 15d);
        score += Math.min(15d, Math.log10(1d + degradationMagnitude) * 10d);

        if (comp.getDataFlags() != null && !comp.getDataFlags().isEmpty()) {
            score += 5d;
        }

        if (resolveDirection(kpi, comp) == Direction.TARGET_IS_BEST) {
            score += 5d;
        }

        return Math.min(100d, score);
    }

    private double computeImprovementMagnitude(Kpi kpi, ComparativeCalculator.ComparativeResult comp) {
        Direction direction = resolveDirection(kpi, comp);
        double gap = comp.getAbsoluteGap();
        if (direction == Direction.HIGHER_IS_BETTER) return Math.max(0d, gap);
        if (direction == Direction.LOWER_IS_BETTER) return Math.max(0d, -gap);
        return 0d;
    }

    private DegradationContext computeDegradationContext(Kpi kpi, ComparativeCalculator.ComparativeResult comp) {
        Direction direction = resolveDirection(kpi, comp);
        double gap = comp.getAbsoluteGap();

        if (direction == Direction.HIGHER_IS_BETTER) {
            return new DegradationContext(Math.max(0d, -gap), false);
        }
        if (direction == Direction.LOWER_IS_BETTER) {
            return new DegradationContext(Math.max(0d, gap), false);
        }


        if (kpi == null || kpi.getTargetValue() == null) {
            return new DegradationContext(Math.abs(gap), true);
        }

        double target = kpi.getTargetValue();
        Double prev = comp.getPreviousValue();
        Double curr = comp.getCurrentValue();
        if (prev == null || curr == null) {
            return new DegradationContext(Math.abs(gap), true);
        }
        double prevDistance = Math.abs(prev - target);
        double currDistance = Math.abs(curr - target);
        return new DegradationContext(Math.max(0d, currDistance - prevDistance), false);
    }

    private List<Double> toDegradationSeries(Kpi kpi, ComparativeCalculator.ComparativeResult comp, List<Double> historical) {
        Direction direction = resolveDirection(kpi, comp);
        List<Double> series = new ArrayList<>();
        for (Double value : historical) {
            if (value == null) {
                continue;
            }
            double v = value;
            if (direction == Direction.HIGHER_IS_BETTER) {
                series.add(Math.max(0d, -v));
            } else if (direction == Direction.LOWER_IS_BETTER) {
                series.add(Math.max(0d, v));
            } else {
                series.add(Math.abs(v));
            }
        }
        return series;
    }

    private Direction resolveDirection(Kpi kpi, ComparativeCalculator.ComparativeResult comp) {
        if (comp != null && comp.getDirection() != null) {
            return comp.getDirection();
        }
        if (kpi != null && kpi.getDirection() != null) {
            return kpi.getDirection();
        }
        return Direction.HIGHER_IS_BETTER;
    }

    private double safe(Double value) {
        return value == null ? 0d : value;
    }

    private double percentile(List<Double> data, double p) {
        List<Double> copy = data.stream().sorted().toList();
        if (copy.isEmpty()) return 0d;
        int idx = (int) Math.ceil((p / 100.0) * copy.size()) - 1;
        idx = Math.max(0, Math.min(idx, copy.size() - 1));
        return copy.get(idx);
    }

    private double robustZScore(List<Double> data, double value) {
        if (data == null || data.isEmpty()) {
            return 0d;
        }
        double med = median(data);
        double mad = mad(data, med);
        if (mad < 1e-9) {
            return 0d;
        }
        return Math.abs(value - med) / (1.4826d * mad);
    }

    private double mad(List<Double> data, double med) {
        List<Double> deviations = data.stream().map(d -> Math.abs(d - med)).sorted().toList();
        return median(deviations);
    }

    private double median(List<Double> data) {
        List<Double> copy = data.stream().sorted().toList();
        int n = copy.size();
        if (n == 0) return 0d;
        if (n % 2 == 1) return copy.get(n / 2);
        return (copy.get(n / 2 - 1) + copy.get(n / 2)) / 2.0;
    }

    private String withTargetFallbackNote(String baseReason, boolean targetFallbackUsed) {
        if (!targetFallbackUsed) {
            return baseReason;
        }
        return baseReason + " (cible explicite absente, fallback sur variation absolue)";
    }
}
