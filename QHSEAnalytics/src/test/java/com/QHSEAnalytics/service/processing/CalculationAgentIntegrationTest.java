package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.enums.UniteKpi;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import com.QHSEAnalytics.shared.repository.ResultatKpiRepository;
import com.QHSEAnalytics.importer.service.processing.CalculationAgent;
import com.QHSEAnalytics.importer.service.processing.ClassificationEngine;
import com.QHSEAnalytics.importer.service.processing.ComparativeCalculator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;

class CalculationAgentIntegrationTest {

    @Test
    void testIntegrationImportToCalculation() {
        KpiRepository repo = Mockito.mock(KpiRepository.class);
        ResultatKpiRepository histRepo = Mockito.mock(ResultatKpiRepository.class);
        Kpi sample = Kpi.builder()
                .id(1L)
                .nom("Test KPI")
                .definition("def")
                .unite(UniteKpi.NOMBRE)
                .categorieKpi(CategorieKpi.builder().libelle("Q").code("Q").build())
                .seuilFaible(1.0).seuilModere(5.0).seuilCritique(10.0)
                .ordre(1).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        ResultatKpiRepository.VariationHistoryProjection proj = new ResultatKpiRepository.VariationHistoryProjection() {
            public Long getKpiId() { return 1L; }
            public Double getVariation() { return 1.5; }
        };

        Mockito.when(repo.findByIsActiveTrueOrderByOrdreAsc()).thenReturn(List.of(sample));
        Mockito.when(histRepo.findVariationHistoryByKpiIds(anyList()))
                .thenReturn(List.of(proj, proj, proj, proj, proj, proj, proj, proj, proj, proj));

        ComparativeCalculator comp = new ComparativeCalculator();
        ClassificationEngine cls = new ClassificationEngine();
        CalculationAgent agent = new CalculationAgent(repo, histRepo, comp, cls);

        KpiRawDataDTO row = KpiRawDataDTO.builder()
                .rowIndex(1)
                .kpiName("Test KPI")
                .categorie("Q")
                .unite("%")
                .valeurN1(0d)
                .valeurN(8d)
                .valid(true)
                .build();

        List<KpiCalculatedDTO> out = agent.calculate(List.of(row));
        assertEquals(1, out.size());
        KpiCalculatedDTO dto = out.get(0);
        assertEquals("Amélioration forte", dto.getStatus());
        assertEquals("FAIBLE", dto.getClassification());
        assertFalse(dto.getReviewRequired());
        assertEquals("HIGHER_IS_BETTER", dto.getDirection());
        assertNotNull(dto.getCalcConfidence());
        assertNotNull(dto.getClassificationReason());
        assertNotNull(dto.getDataFlags());
        Mockito.verify(histRepo).findVariationHistoryByKpiIds(anyList());
    }
}
