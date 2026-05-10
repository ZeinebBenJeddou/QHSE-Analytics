package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.enums.Direction;
import com.QHSEAnalytics.importer.service.processing.ComparativeCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComparativeCalculatorTest {

    private final ComparativeCalculator calc = new ComparativeCalculator();

    @Test
    void testN1ZeroToZeroIsStable() {
        ComparativeCalculator.ComparativeResult r = calc.compute(null, 0d, 0d);
        assertEquals("STABLE", r.getSpecialCase());
        assertTrue(r.getCalcConfidence() >= 0 && r.getCalcConfidence() <= 100);
    }

    @Test
    void testN1ZeroToNonZeroEmergingRisk() {
        ComparativeCalculator.ComparativeResult r = calc.compute(null, 0d, 5d);
        assertEquals("EMERGING_RISK", r.getSpecialCase());
        assertTrue(r.getDataFlags().contains(com.QHSEAnalytics.shared.enums.DataFlag.LOW_BASE));
    }

    @Test
    void testN1NonZeroToZeroStrongImprovement() {
        ComparativeCalculator.ComparativeResult r = calc.compute(null, 12d, 0d);
        assertEquals("STRONG_IMPROVEMENT", r.getSpecialCase());
        assertEquals(-100d, r.getRelativePercentage(), 1e-6);
    }

    @Test
    void testNormalRelativeCalculation() {
        ComparativeCalculator.ComparativeResult r = calc.compute(null, 10d, 15d);
        assertNull(r.getSpecialCase());
        assertEquals(5.0, r.getAbsoluteGap(), 1e-6);
        assertEquals(60, r.getCalcConfidence()); // base confidence is now 60
    }

    @Test
    void testDirectionResolverLowerIsBetterByName() {
        Kpi kpi = Kpi.builder()
                .nom("Taux d'accidents")
                .categorieKpi(CategorieKpi.builder().code("S").libelle("Sécurité").build())
                .build();

        ComparativeCalculator.ComparativeResult r = calc.compute(kpi, 10d, 11d);
        assertEquals(Direction.LOWER_IS_BETTER, r.getDirection());
    }

    @Test
    void testDirectionResolverTargetByName() {
        Kpi kpi = Kpi.builder()
                .nom("KPI cible conformité")
                .categorieKpi(CategorieKpi.builder().code("Q").libelle("Qualité").build())
                .build();

        ComparativeCalculator.ComparativeResult r = calc.compute(kpi, 95d, 92d);
        assertEquals(Direction.TARGET_IS_BEST, r.getDirection());
    }

    @Test
    void testDirectionResolverUsesExplicitKpiDirectionFirst() {
        Kpi kpi = Kpi.builder()
                .nom("Taux de conformité")
                .categorieKpi(CategorieKpi.builder().code("S").libelle("Sécurité").build())
                .build();
        kpi.setDirection(Direction.LOWER_IS_BETTER);

        ComparativeCalculator.ComparativeResult r = calc.compute(kpi, 90d, 80d);
        assertEquals(Direction.LOWER_IS_BETTER, r.getDirection());
    }

    @Test
    void testConfidenceAndFlagsOutlier() {
        ComparativeCalculator.ComparativeResult r = calc.compute(null, 1d, 10d);
        assertTrue(r.getDataFlags().contains(com.QHSEAnalytics.shared.enums.DataFlag.OUTLIER));
        assertTrue(r.getCalcConfidence() < 60);
        assertTrue(r.isReviewRequired());
    }
}
