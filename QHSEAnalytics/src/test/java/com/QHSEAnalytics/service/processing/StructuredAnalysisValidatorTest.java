package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.response.*;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StructuredAnalysisValidatorTest {

    private final StructuredAnalysisValidator validator = new StructuredAnalysisValidator();

    @Test
    void validate_shouldReturnErrorsForNullResponse() {
        var errors = validator.validate(null, List.of());
        assertThat(errors).contains("Response is null.");
    }

    @Test
    void validate_shouldReturnErrorsForMissingMandatoryFields() {
        var response = AiAnalysisStructuredResponse.builder()
                .status("SUCCESS")
                .build();

        var errors = validator.validate(response, List.of());
        assertThat(errors).contains(
                "globalSummary is mandatory.",
                "confidence object is mandatory.",
                "probableCauses array is mandatory.",
                "recommendations array is mandatory.",
                "actionPlan array is mandatory.",
                "traceability object is mandatory.",
                "kpiInsights must contain at least one KPI item."
        );
    }

    @Test
    void validate_shouldReturnErrorsForInvalidConfidence() {
        var response = AiAnalysisStructuredResponse.builder()
                .globalSummary("Test summary")
                .confidence(AiConfidenceResponse.builder()
                        .overall(150.0) // Invalid: > 100
                        .sections(java.util.Map.of())
                        .build())
                .probableCauses(List.of())
                .recommendations(List.of())
                .actionPlan(List.of())
                .traceability(AiTraceabilityResponse.builder()
                        .modelName("test")
                        .generatedAt("2023-01-01T00:00:00Z")
                        .contextSourcesUsed(List.of())
                        .build())
                .kpiInsights(List.of(AiKpiInsightResponse.builder()
                        .kpiName("Test KPI")
                        .confidence(50.0)
                        .insight("Test insight")
                        .probableCauses(List.of())
                        .recommendations(List.of())
                        .actionImmediate("Test action")
                        .urgency("HIGH")
                        .ownerRole("Test owner")
                        .dueHorizon("Q1 2024")
                        .successMetric("Test metric")
                        .riskIfNotDone("Test risk")
                        .build()))
                .status("SUCCESS")
                .build();

        var errors = validator.validate(response, List.of());
        assertThat(errors).contains(
                "confidence.overall must be between 0 and 100.",
                "confidence.sections is mandatory and should contain section-level values."
        );
    }

    @Test
    void validate_shouldReturnErrorsForInvalidKpiInsight() {
        var response = AiAnalysisStructuredResponse.builder()
                .globalSummary("Test summary")
                .confidence(AiConfidenceResponse.builder()
                        .overall(85.0)
                        .sections(java.util.Map.of("summary", 80.0))
                        .build())
                .probableCauses(List.of())
                .recommendations(List.of())
                .actionPlan(List.of())
                .traceability(AiTraceabilityResponse.builder()
                        .modelName("test")
                        .generatedAt("2023-01-01T00:00:00Z")
                        .contextSourcesUsed(List.of(AiContextSourceResponse.builder()
                                .sourceName("test")
                                .relevanceScore(80.0)
                                .build()))
                        .build())
                .kpiInsights(List.of(AiKpiInsightResponse.builder()
                        .kpiName("") // Invalid: blank
                        .confidence(50.0)
                        .insight("") // Invalid: blank
                        .probableCauses(null) // Invalid: null
                        .recommendations(null) // Invalid: null
                        .actionImmediate("") // Invalid: blank
                        .urgency("") // Invalid: blank
                        .ownerRole("") // Invalid: blank
                        .dueHorizon("") // Invalid: blank
                        .successMetric("") // Invalid: blank
                        .riskIfNotDone("") // Invalid: blank
                        .build()))
                .status("SUCCESS")
                .build();

        var errors = validator.validate(response, List.of());
        assertThat(errors).contains(
                "kpiInsights[0].kpiName is mandatory.",
                "kpiInsights[0].insight is mandatory.",
                "kpiInsights[0].probableCauses is mandatory.",
                "kpiInsights[0].recommendations is mandatory.",
                "kpiInsights[0].actionImmediate is mandatory.",
                "kpiInsights[0].urgency is mandatory.",
                "kpiInsights[0].ownerRole is mandatory.",
                "kpiInsights[0].dueHorizon is mandatory.",
                "kpiInsights[0].successMetric is mandatory.",
                "kpiInsights[0].riskIfNotDone is mandatory."
        );
    }

    @Test
    void validate_shouldPassForValidResponse() {
        var response = AiAnalysisStructuredResponse.builder()
                .globalSummary("Test summary")
                .confidence(AiConfidenceResponse.builder()
                        .overall(85.0)
                        .sections(java.util.Map.of("summary", 80.0, "insights", 90.0))
                        .build())
                .probableCauses(List.of("Cause 1"))
                .recommendations(List.of(AiRecommendationResponse.builder()
                        .title("Test rec")
                        .rationale("Test rationale")
                        .expectedBenefit("Test benefit")
                        .urgency("HIGH")
                        .build()))
                .actionPlan(List.of(AiActionPlanItemResponse.builder()
                        .action("Test action")
                        .priority("HIGH")
                        .ownerRole("Test owner")
                        .dueHorizon("Q1 2024")
                        .successMetric("Test metric")
                        .riskIfNotDone("Test risk")
                        .build()))
                .traceability(AiTraceabilityResponse.builder()
                        .modelName("test-model")
                        .generatedAt("2023-01-01T00:00:00Z")
                        .contextSourcesUsed(List.of(AiContextSourceResponse.builder()
                                .sourceName("test-source")
                                .relevanceScore(85.0)
                                .build()))
                        .build())
                .kpiInsights(List.of(AiKpiInsightResponse.builder()
                        .kpiName("Test KPI")
                        .confidence(75.0)
                        .insight("Test insight")
                        .probableCauses(List.of("Cause 1"))
                        .recommendations(List.of("Rec 1"))
                        .actionImmediate("Test action")
                        .urgency("MEDIUM")
                        .ownerRole("Test owner")
                        .dueHorizon("Q2 2024")
                        .successMetric("Test metric")
                        .riskIfNotDone("Test risk")
                        .build()))
                .status("SUCCESS")
                .build();

        var availableKpis = List.of(KpiCalculatedDTO.builder()
                .kpiName("Test KPI")
                .build());

        var errors = validator.validate(response, availableKpis);
        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldFailWhenCoverageIsIncompleteForTenKpis() {
        var availableKpis = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> KpiCalculatedDTO.builder()
                        .matchedKpiId((long) index)
                        .kpiName("KPI " + index)
                        .build())
                .toList();

        var insights = IntStream.rangeClosed(1, 4)
                .mapToObj(index -> AiKpiInsightResponse.builder()
                        .kpiId((long) index)
                        .kpiName("KPI " + index)
                        .confidence(75.0)
                        .insight("Insight " + index)
                        .probableCauses(List.of("Cause " + index))
                        .recommendations(List.of("Rec " + index))
                        .actionImmediate("Action " + index)
                        .urgency("HIGH")
                        .ownerRole("Owner")
                        .dueHorizon("Q1 2026")
                        .successMetric("Metric")
                        .riskIfNotDone("Risk")
                        .build())
                .toList();

        var response = buildValidResponse(insights);

        var errors = validator.validate(response, availableKpis);
        assertThat(errors).anyMatch(error -> error.contains("coverage is incomplete"));
    }

    @Test
    void validate_shouldPassWhenAllTenKpisAreCovered() {
        var availableKpis = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> KpiCalculatedDTO.builder()
                        .matchedKpiId((long) index)
                        .kpiName("KPI " + index)
                        .build())
                .toList();

        var insights = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> AiKpiInsightResponse.builder()
                        .kpiId((long) index)
                        .kpiName("KPI " + index)
                        .confidence(75.0)
                        .insight("Insight " + index)
                        .probableCauses(List.of("Cause " + index))
                        .recommendations(List.of("Rec " + index))
                        .actionImmediate("Action " + index)
                        .urgency("HIGH")
                        .ownerRole("Owner")
                        .dueHorizon("Q1 2026")
                        .successMetric("Metric")
                        .riskIfNotDone("Risk")
                        .build())
                .toList();

        var response = buildValidResponse(insights);

        var errors = validator.validate(response, availableKpis);
        assertThat(errors).isEmpty();
    }

    private AiAnalysisStructuredResponse buildValidResponse(List<AiKpiInsightResponse> insights) {
        return AiAnalysisStructuredResponse.builder()
                .globalSummary("Test summary")
                .confidence(AiConfidenceResponse.builder()
                        .overall(85.0)
                        .sections(java.util.Map.of("summary", 80.0, "insights", 90.0))
                        .build())
                .probableCauses(List.of("Cause 1"))
                .recommendations(List.of(AiRecommendationResponse.builder()
                        .title("Test rec")
                        .rationale("Test rationale")
                        .expectedBenefit("Test benefit")
                        .urgency("HIGH")
                        .build()))
                .actionPlan(List.of(AiActionPlanItemResponse.builder()
                        .action("Test action")
                        .priority("HIGH")
                        .ownerRole("Test owner")
                        .dueHorizon("Q1 2024")
                        .successMetric("Test metric")
                        .riskIfNotDone("Test risk")
                        .build()))
                .traceability(AiTraceabilityResponse.builder()
                        .modelName("test-model")
                        .generatedAt("2023-01-01T00:00:00Z")
                        .contextSourcesUsed(List.of(AiContextSourceResponse.builder()
                                .sourceName("test-source")
                                .relevanceScore(85.0)
                                .build()))
                        .build())
                .kpiInsights(insights)
                .status("SUCCESS")
                .build();
    }
}