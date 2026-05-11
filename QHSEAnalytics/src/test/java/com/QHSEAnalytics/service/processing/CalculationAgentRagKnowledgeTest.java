package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.importer.service.processing.CalculationAgent;
import com.QHSEAnalytics.importer.service.processing.ClassificationEngine;
import com.QHSEAnalytics.importer.service.processing.ComparativeCalculator;
import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.entity.UniteKpi;
import com.QHSEAnalytics.shared.enums.Direction;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import com.QHSEAnalytics.shared.repository.ResultatKpiRepository;
import com.QHSEAnalytics.shared.service.KpiKnowledgeLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationAgentRagKnowledgeTest {

    @Mock
    private KpiRepository kpiRepository;

    @Mock
    private ResultatKpiRepository resultatKpiRepository;

    @Mock
    private KpiKnowledgeLookup kpiKnowledgeLookup;

    @Test
    void usesRagThresholdsAndDirectionForClassification() {
        Kpi catalogKpi = Kpi.builder()
                .id(1L)
                .nom("Nombre d'incidents")
                .definition("Catalogue interne")
                .unite(UniteKpi.NOMBRE)
                .categorieKpi(CategorieKpi.builder().code("S").libelle("Securite").build())
                .seuilFaible(50.0)
                .seuilModere(75.0)
                .seuilCritique(100.0)
                .ordre(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(kpiRepository.findByIsActiveTrueOrderByOrdreAsc()).thenReturn(List.of(catalogKpi));
        when(resultatKpiRepository.findVariationHistoryByKpiId(1L)).thenReturn(List.of());
        when(kpiKnowledgeLookup.findBestMatch("Nombre d'incidents", "S", 20.0, 10.0))
                .thenReturn(Optional.of(new KpiKnowledgeLookup.KnowledgeMatch(
                        "Nombre d'incidents",
                        "S",
                        "Direction: lower_better",
                        2.0,
                        5.0,
                        10.0,
                        Direction.LOWER_IS_BETTER,
                        0.91
                )));

        CalculationAgent agent = new CalculationAgent(
                kpiRepository,
                resultatKpiRepository,
                new ComparativeCalculator(),
                new ClassificationEngine(),
                kpiKnowledgeLookup
        );

        KpiRawDataDTO row = KpiRawDataDTO.builder()
                .rowIndex(1)
                .kpiName("Nombre d'incidents")
                .categorie("S")
                .unite("U")
                .valeurN1(10d)
                .valeurN(20d)
                .valid(true)
                .build();

        KpiCalculatedDTO result = agent.calculate(List.of(row)).get(0);

        assertEquals("CRITIQUE", result.getClassification());
        assertEquals(Direction.LOWER_IS_BETTER.name(), result.getDirection());
        assertEquals(2.0, result.getSeuilFaible());
        assertEquals(5.0, result.getSeuilModere());
        assertEquals(10.0, result.getSeuilCritique());
    }
}
