package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.importer.service.processing.CalculationAgent;
import com.QHSEAnalytics.importer.service.processing.ClassificationEngine;
import com.QHSEAnalytics.importer.service.processing.ComparativeCalculator;
import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.enums.UniteKpi;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import com.QHSEAnalytics.shared.repository.ResultatKpiRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;

class CalculationAgentMatchingTest {

    @Test
    void rejectsGenericJaroFalsePositives() {
        CalculationAgent agent = buildAgent(List.of(
                kpi(1L, "Taux de visites médicales"),
                kpi(2L, "Nombre de situations dangereuses")
        ));

        KpiCalculatedDTO first = agent.calculate(List.of(row("Taux de conformité des permis de travail"))).get(0);
        KpiCalculatedDTO second = agent.calculate(List.of(row("Nombre d'actions correctives clôturées dans les délais"))).get(0);

        assertNull(first.getMatchedKpi());
        assertNull(first.getMatchedKpiId());
        assertNull(first.getMatchConfidence());

        assertNull(second.getMatchedKpi());
        assertNull(second.getMatchedKpiId());
        assertNull(second.getMatchConfidence());
    }

    @Test
    void keepsExactInclusionAndTyposWorking() {
        CalculationAgent agent = buildAgent(List.of(
                kpi(1L, "Taux de Satisfaction Client")
        ));

        KpiCalculatedDTO exact = agent.calculate(List.of(row("Taux de Satisfaction Client"))).get(0);
        KpiCalculatedDTO inclusion = agent.calculate(List.of(row("Taux de Satisfaction"))).get(0);
        KpiCalculatedDTO typo = agent.calculate(List.of(row("Taux de satisfaccion client"))).get(0);

        assertEquals("Taux de Satisfaction Client", exact.getMatchedKpi());
        assertEquals(1L, exact.getMatchedKpiId());
        assertEquals(1.0, exact.getMatchConfidence(), 0.0001);

        assertEquals("Taux de Satisfaction Client", inclusion.getMatchedKpi());
        assertEquals(1L, inclusion.getMatchedKpiId());
        assertEquals(0.9, inclusion.getMatchConfidence(), 0.0001);

        assertEquals("Taux de Satisfaction Client", typo.getMatchedKpi());
        assertEquals(1L, typo.getMatchedKpiId());
        assertNotNull(typo.getMatchConfidence());
        assertTrue(typo.getMatchConfidence() >= 0.82);
    }

    private CalculationAgent buildAgent(List<Kpi> kpis) {
        KpiRepository repo = Mockito.mock(KpiRepository.class);
        ResultatKpiRepository histRepo = Mockito.mock(ResultatKpiRepository.class);
        Mockito.when(repo.findByIsActiveTrueOrderByNomAsc()).thenReturn(kpis);
        Mockito.when(histRepo.findVariationHistoryByKpiIds(anyList())).thenReturn(List.of());
        return new CalculationAgent(repo, histRepo, new ComparativeCalculator(), new ClassificationEngine());
    }

    private KpiRawDataDTO row(String kpiName) {
        return KpiRawDataDTO.builder()
                .rowIndex(1)
                .kpiName(kpiName)
                .categorie("Q")
                .unite("%")
                .valeurN1(10d)
                .valeurN(12d)
                .valid(true)
                .build();
    }

    private Kpi kpi(Long id, String name) {
        return Kpi.builder()
                .id(id)
                .nom(name)
                .definition("def")
                .unite(UniteKpi.POURCENTAGE)
                .categorieKpi(CategorieKpi.builder().code("Q").libelle("Qualite").build())
                .seuilFaible(1.0)
                .seuilModere(5.0)
                .seuilCritique(10.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
