package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChartResponseDTO {
    private List<ChartSeries> barCharts;
    private List<ChartSeries> lineCharts;
    private List<ChartTableRow> comparisonTable;

    @Data
    @Builder
    public static class ChartSeries {
        private String label;
        private List<String> categories;
        private List<Double> values;
    }

    @Data
    @Builder
    public static class ChartTableRow {
        private String kpi;
        private String categorie;
        private String unite;
        private Double valeurN1;
        private Double valeurN;
        private Double variationPercentage;
        private String classification;
        private String tendance;
    }
}
