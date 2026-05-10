package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.enums.Direction;
import com.QHSEAnalytics.importer.service.processing.ClassificationEngine;
import com.QHSEAnalytics.importer.service.processing.ComparativeCalculator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationEngineTest {

    private final ClassificationEngine engine = new ClassificationEngine();
    private final ComparativeCalculator compCalc = new ComparativeCalculator();

    @Test
    void testThresholdsClassificationCriticalOnDegradation() {
        Kpi k = Kpi.builder()
                .nom("Taux de conformité")
                .categorieKpi(CategorieKpi.builder().code("Q").libelle("Qualité").build())
                .seuilFaible(1.0).seuilModere(5.0).seuilCritique(10.0)
                .build();
        ComparativeCalculator.ComparativeResult comp = compCalc.compute(k, 90d, 70d);
        ClassificationEngine.ClassificationResult res = engine.classify(k, comp, null);
        assertEquals("CRITIQUE", res.getClassification());
    }

    @Test
    void testThresholdsImprovementNotCritical() {
        Kpi k = Kpi.builder()
                .nom("Nombre d'incidents")
                .categorieKpi(CategorieKpi.builder().code("S").libelle("Sécurité").build())
                .seuilFaible(1.0).seuilModere(5.0).seuilCritique(10.0)
                .build();
        // ComparativeCalculator infers LOWER_IS_BETTER from the name "incidents".
        // Reduction 20→10 (magnitude 10) >> seuilFaible*1.5 (1.5) → EXCELLENT is correct.
        ComparativeCalculator.ComparativeResult comp = compCalc.compute(k, 20d, 10d);
        ClassificationEngine.ClassificationResult res = engine.classify(k, comp, null);
        assertEquals("EXCELLENT", res.getClassification());
    }

    @Test
    void testFallbackWithSufficientHistory() {
        ComparativeCalculator.ComparativeResult comp = compCalc.compute(null, 50d, 20d);
        ClassificationEngine.ClassificationResult res = engine.classify(null, comp,
                List.of(1d, 2d, 1.5d, 3d, 2d, 2.5d, 1d, 2d, 3d, 2d, 1.7d));

        assertTrue(List.of("MODERE", "CRITIQUE").contains(res.getClassification()));
    }

    @Test
    void testFallbackInsufficientHistoryIndeterminate() {
        Kpi k = null;
        ComparativeCalculator.ComparativeResult comp = compCalc.compute(null, 5d, 6d);
        ClassificationEngine.ClassificationResult res = engine.classify(k, comp, List.of(1d, 2d, 3d));
        assertEquals("INDETERMINE", res.getClassification());
        assertTrue(res.isReviewRequired());
    }

    @Test
    void testTargetWithoutExplicitTargetForcesReview() {
        Kpi k = Kpi.builder()
                .nom("KPI cible conformité")
                .categorieKpi(CategorieKpi.builder().code("Q").libelle("Qualité").build())
                .build();
        k.setDirection(Direction.TARGET_IS_BEST);

        ComparativeCalculator.ComparativeResult comp = compCalc.compute(k, 90d, 92d);
        ClassificationEngine.ClassificationResult res = engine.classify(k, comp, List.of(1d, 1.1, 0.9, 1.2, 1.05));

        assertTrue(res.isReviewRequired());
        assertTrue(res.getReason().contains("cible explicite absente"));
    }

    @Test
    void testTargetWithExplicitTargetValue() {
        Kpi k = Kpi.builder()
                .nom("KPI cible 100")
                .categorieKpi(CategorieKpi.builder().code("Q").libelle("Qualité").build())
                .seuilFaible(5.0).seuilModere(10.0).seuilCritique(20.0)
                .build();
        k.setDirection(Direction.TARGET_IS_BEST);
        k.setTargetValue(100.0);

        ComparativeCalculator.ComparativeResult comp = compCalc.compute(k, 95d, 102d);
        ClassificationEngine.ClassificationResult res = engine.classify(k, comp, null);

        // Degradation from 95 to 102 (moving away from target 100) should be classified
        assertEquals("FAIBLE", res.getClassification());
        assertFalse(res.isReviewRequired());
    }

    @Test
    void testIndeterminateWhenNoThresholdsNoTargetNoHistory() {
        Kpi k = Kpi.builder()
                .nom("KPI sans contexte")
                .categorieKpi(CategorieKpi.builder().code("M").libelle("Misc").build())
                .build();
        k.setDirection(Direction.TARGET_IS_BEST);

        ComparativeCalculator.ComparativeResult comp = compCalc.compute(k, 50d, 55d);
        ClassificationEngine.ClassificationResult res = engine.classify(k, comp, List.of(60d));

        assertEquals("INDETERMINE", res.getClassification());
        assertTrue(res.isReviewRequired());
    }
}
