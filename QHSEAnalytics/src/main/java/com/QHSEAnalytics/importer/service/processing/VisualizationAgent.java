package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.response.ChartResponseDTO;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VisualizationAgent {

    public ChartResponseDTO build(List<KpiCalculatedDTO> calculatedData) {
        if (calculatedData == null || calculatedData.isEmpty()) {
            return ChartResponseDTO.builder()
                    .barCharts(List.of())
                    .lineCharts(List.of())
                    .comparisonTable(List.of())
                    .build();
        }

        Map<String, List<KpiCalculatedDTO>> byCategorie = new HashMap<>();
        for (KpiCalculatedDTO item : calculatedData) {
            String categorie = item.getCategorie() == null || item.getCategorie().isBlank()
                    ? "Non classé" : item.getCategorie();
            byCategorie.computeIfAbsent(categorie, key -> new ArrayList<>()).add(item);
        }

        List<ChartResponseDTO.ChartSeries> barCharts = new ArrayList<>();
        byCategorie.forEach((categorie, kpis) -> {
            List<String> labels = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            for (KpiCalculatedDTO kpi : kpis) {
                labels.add(kpi.getKpiName() == null || kpi.getKpiName().isBlank() ? "KPI" : kpi.getKpiName());
                values.add(kpi.getVariationPercentage() == null ? 0d : kpi.getVariationPercentage());
            }
            barCharts.add(ChartResponseDTO.ChartSeries.builder()
                    .label("Variation % — " + categorie)
                    .categories(labels)
                    .values(values)
                    .build());
        });

        List<String> allLabels = new ArrayList<>();
        List<Double> valN1Series = new ArrayList<>();
        List<Double> valNSeries = new ArrayList<>();
        for (KpiCalculatedDTO kpi : calculatedData) {
            allLabels.add(kpi.getKpiName() == null || kpi.getKpiName().isBlank() ? "KPI" : kpi.getKpiName());
            valN1Series.add(kpi.getValeurN1() == null ? 0d : kpi.getValeurN1());
            valNSeries.add(kpi.getValeurN() == null ? 0d : kpi.getValeurN());
        }

        List<ChartResponseDTO.ChartSeries> lineCharts = List.of(
                ChartResponseDTO.ChartSeries.builder()
                        .label("Valeur N-1")
                        .categories(allLabels)
                        .values(valN1Series)
                        .build(),
                ChartResponseDTO.ChartSeries.builder()
                        .label("Valeur N")
                        .categories(allLabels)
                        .values(valNSeries)
                        .build()
        );

        List<ChartResponseDTO.ChartTableRow> tableRows = new ArrayList<>();
        for (KpiCalculatedDTO item : calculatedData) {
            tableRows.add(ChartResponseDTO.ChartTableRow.builder()
                    .kpi(item.getKpiName())
                    .categorie(item.getCategorie())
                    .unite(item.getUnite())
                    .valeurN(item.getValeurN())
                    .valeurN1(item.getValeurN1())
                    .variationPercentage(item.getVariationPercentage())
                    .classification(item.getClassification())
                    .tendance(item.getTendance())
                    .build());
        }

        return ChartResponseDTO.builder()
                .barCharts(barCharts)
                .lineCharts(lineCharts)
                .comparisonTable(tableRows)
                .build();
    }
}
