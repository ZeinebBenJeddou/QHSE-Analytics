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

        if ("NA".equals(comp.getSpecialCase())) {
            r.setClassification(INDETERMINE);
            r.setReason("Valeur absente pour l'une des périodes — comparaison impossible");
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
        double degradationMagnitude = computeDegradationMagnitude(kpi, comp);
        boolean hasDegradation = degradationMagnitude > 0;
        double degradationScore = computeDegradationScore(comp, degradationMagnitude);

        if (hasThresholds) {
            double faible   = safe(kpi.getSeuilFaible());
            double modere   = safe(kpi.getSeuilModere());
            double critique = safe(kpi.getSeuilCritique());

            Direction dir = resolveDirection(kpi, comp);
            Double currentValue = comp.getCurrentValue();
            String absolutePosition = computeAbsolutePosition(dir, currentValue, faible, modere, critique);

            if (!hasDegradation) {
                if ("EXCELLENT".equals(absolutePosition)) {
                    r.setClassification(EXCELLENT);
                    r.setReason("Performance excellente — valeur actuelle au-delà du seuil optimal");
                    r.setReviewRequired(false);
                    return r;
                }
                if ("MODERE".equals(absolutePosition)) {
                    r.setClassification(MODERE);
                    r.setReason("Amélioration en cours mais valeur encore en zone modérée");
                    r.setReviewRequired(comp.isReviewRequired());
                    return r;
                }
                if ("CRITIQUE".equals(absolutePosition)) {
                    r.setClassification(CRITIQUE);
                    r.setReason("Valeur en zone critique malgré une amélioration par rapport à N-1");
                    r.setReviewRequired(true);
                    return r;
                }
                r.setClassification(FAIBLE);
                r.setReason("Variation orientée amélioration selon la direction métier");
                r.setReviewRequired(comp.isReviewRequired());
                return r;
            }

            // Position absolue prime sur le score composite
            if ("CRITIQUE".equals(absolutePosition)) {
                r.setClassification(CRITIQUE);
                r.setReason("Valeur en zone critique absolue selon les seuils KPI");
                r.setReviewRequired(true);
                return r;
            }
            if ("MODERE".equals(absolutePosition)) {
                r.setClassification(MODERE);
                r.setReason("Valeur en zone modérée absolue selon les seuils KPI");
                r.setReviewRequired(comp.isReviewRequired());
                return r;
            }
            if ("EXCELLENT".equals(absolutePosition)) {
                r.setClassification(FAIBLE);
                r.setReason("Légère dégradation mais valeur dans la zone optimale");
                r.setReviewRequired(comp.isReviewRequired());
                return r;
            }

            if (degradationMagnitude >= critique || degradationScore >= 85) {
                r.setClassification(CRITIQUE);
                r.setReason("Dégradation critique selon les seuils KPI");
                r.setReviewRequired(true);
                return r;
            }
            if (degradationScore >= 72 || (critique > 0 && degradationMagnitude >= critique * 0.75 && degradationMagnitude < critique)) {
                r.setClassification(PRE_ESCALADE);
                r.setReason("Dégradation pré-critique — surveillance immédiate requise");
                r.setReviewRequired(true);
                return r;
            }
            if (degradationMagnitude >= modere || degradationScore >= 60) {
                r.setClassification(MODERE);
                r.setReason("Dégradation modérée selon les seuils KPI");
                r.setReviewRequired(comp.isReviewRequired() || degradationScore >= 70);
                return r;
            }
            if (degradationMagnitude >= faible || degradationScore >= 35) {
                r.setClassification(FAIBLE);
                r.setReason("Dégradation faible mais surveillée");
                r.setReviewRequired(comp.isReviewRequired());
                return r;
            }

            r.setClassification(FAIBLE);
            r.setReason("Dans les seuils attendus");
            r.setReviewRequired(comp.isReviewRequired());
            return r;
        }

        List<Double> history = historical == null ? List.of() : historical;
        if (history.size() >= 10) {
            List<Double> historyDegradations = toDegradationSeries(kpi, comp, history);
            double p95 = percentile(historyDegradations, 95);
            double p75 = percentile(historyDegradations, 75);
            double p25 = percentile(historyDegradations, 25);

            if (!hasDegradation && p25 > 0 && degradationMagnitude == 0) {
                double improvementMag = computeImprovementMagnitude(kpi, comp);
                if (improvementMag > p75) {
                    r.setClassification(EXCELLENT);
                    r.setReason("Performance excellente dans le contexte historique (amélioration > p75)");
                    r.setReviewRequired(false);
                    return r;
                }
            }
            if (degradationMagnitude > p95) {
                r.setClassification(CRITIQUE);
                r.setReason("Dégradation hors distribution historique (p95)");
                r.setReviewRequired(true);
                return r;
            }
            if (degradationMagnitude > p75) {
                r.setClassification(MODERE);
                r.setReason("Dégradation notable dans l'historique (p75)");
                r.setReviewRequired(comp.isReviewRequired());
                return r;
            }
            if (degradationMagnitude > p75 * 0.85) {
                r.setClassification(PRE_ESCALADE);
                r.setReason("Dégradation pré-critique dans l'historique (approche p75)");
                r.setReviewRequired(true);
                return r;
            }
            r.setClassification(FAIBLE);
            r.setReason("Dégradation contenue dans l'historique");
            r.setReviewRequired(comp.isReviewRequired());
            return r;
        }

        if (history.size() >= 5) {
            List<Double> historyDegradations = toDegradationSeries(kpi, comp, history);
            double robustZ = robustZScore(historyDegradations, degradationMagnitude);
            if (robustZ >= 3.0) {
                r.setClassification(CRITIQUE);
                r.setReason("Dégradation atypique détectée (z robuste)");
                r.setReviewRequired(true);
                return r;
            }
            if (robustZ >= 2.4) {
                r.setClassification(PRE_ESCALADE);
                r.setReason("Dégradation pré-critique (z robuste ≥ 2.4)");
                r.setReviewRequired(true);
                return r;
            }
            if (robustZ >= 1.8) {
                r.setClassification(MODERE);
                r.setReason("Dégradation au-dessus du comportement normal (z robuste)");
                r.setReviewRequired(comp.isReviewRequired());
                return r;
            }
            if (!hasDegradation) {
                double improvementZ = robustZScore(historyDegradations, computeImprovementMagnitude(kpi, comp));
                if (improvementZ >= 2.0) {
                    r.setClassification(EXCELLENT);
                    r.setReason("Performance excellente (z robuste amélioration ≥ 2.0)");
                    r.setReviewRequired(false);
                    return r;
                }
            }
            r.setClassification(FAIBLE);
            r.setReason("Dégradation non atypique selon l'historique court");
            r.setReviewRequired(comp.isReviewRequired());
            return r;
        }

        r.setClassification(INDETERMINE);
        r.setReason("Pas de seuils ni historique suffisant");
        r.setReviewRequired(true);
        return r;
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────

    private boolean hasThresholds(Kpi kpi) {
        return kpi != null
                && kpi.getSeuilFaible() != null
                && kpi.getSeuilModere() != null
                && kpi.getSeuilCritique() != null;
    }

    private String computeAbsolutePosition(Direction dir, Double currentValue, double faible, double modere, double critique) {
        if (currentValue == null) return "UNKNOWN";
        double v = currentValue;
        if (dir == Direction.LOWER_IS_BETTER) {
            if (faible   > 0 && v <= faible)   return "EXCELLENT";
            if (modere   > 0 && v <= modere)   return "FAIBLE";
            if (critique > 0 && v <= critique) return "MODERE";
            return "CRITIQUE";
        }
        if (dir == Direction.HIGHER_IS_BETTER) {
            if (critique > 0 && v >= critique) return "EXCELLENT";
            if (modere   > 0 && v >= modere)   return "FAIBLE";
            if (faible   > 0 && v >= faible)   return "MODERE";
            return "CRITIQUE";
        }
        return "UNKNOWN";
    }

    private double computeDegradationScore(ComparativeCalculator.ComparativeResult comp, double degradationMagnitude) {
        if (degradationMagnitude <= 0) return 0d;
        double relative = comp.getRelativePercentage() == null ? 0d : Math.abs(comp.getRelativePercentage());
        double absolute = Math.abs(comp.getAbsoluteGap());
        double score = 0d;
        score += Math.min(55d, relative * 0.55d);
        score += Math.min(30d, Math.log10(1d + absolute) * 15d);
        score += Math.min(15d, Math.log10(1d + degradationMagnitude) * 10d);
        if (comp.getDataFlags() != null && !comp.getDataFlags().isEmpty()) {
            score += 5d;
        }
        return Math.min(100d, score);
    }

    private double computeImprovementMagnitude(Kpi kpi, ComparativeCalculator.ComparativeResult comp) {
        Direction direction = resolveDirection(kpi, comp);
        double gap = comp.getAbsoluteGap();
        if (direction == Direction.HIGHER_IS_BETTER) return Math.max(0d, gap);
        if (direction == Direction.LOWER_IS_BETTER)  return Math.max(0d, -gap);
        return 0d;
    }

    private double computeDegradationMagnitude(Kpi kpi, ComparativeCalculator.ComparativeResult comp) {
        Direction direction = resolveDirection(kpi, comp);
        double gap = comp.getAbsoluteGap();
        if (direction == Direction.HIGHER_IS_BETTER) return Math.max(0d, -gap);
        if (direction == Direction.LOWER_IS_BETTER)  return Math.max(0d, gap);
        return Math.abs(gap);
    }

    private List<Double> toDegradationSeries(Kpi kpi, ComparativeCalculator.ComparativeResult comp, List<Double> historical) {
        Direction direction = resolveDirection(kpi, comp);
        List<Double> series = new ArrayList<>();
        for (Double value : historical) {
            if (value == null) continue;
            double v = value;
            if (direction == Direction.HIGHER_IS_BETTER) {
                series.add(Math.max(0d, -v));
            } else {
                series.add(Math.max(0d, v));
            }
        }
        return series;
    }

    private Direction resolveDirection(Kpi kpi, ComparativeCalculator.ComparativeResult comp) {
        if (comp != null && comp.getDirection() != null) return comp.getDirection();
        if (kpi  != null && kpi.getDirection()  != null) return kpi.getDirection();
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
        if (data == null || data.isEmpty()) return 0d;
        double med = median(data);
        double mad = mad(data, med);
        if (mad < 1e-9) return 0d;
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
}
