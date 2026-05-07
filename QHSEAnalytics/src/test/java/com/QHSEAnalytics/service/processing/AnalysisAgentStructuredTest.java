package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.response.AiAnalysisStructuredResponse;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.service.LlmProviderChain;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisAgentStructuredTest {

    @Mock
    private LlmProviderChain llmProviderChain;

    @Mock
    private StructuredAnalysisPromptBuilder structuredAnalysisPromptBuilder;

    @Mock
    private StructuredAnalysisValidator structuredAnalysisValidator;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private Timer timer;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AnalysisAgent analysisAgent;

    @BeforeEach
    void setUp() {
        lenient().when(meterRegistry.counter(anyString(), any(Tags.class))).thenReturn(counter);
        lenient().when(meterRegistry.timer(anyString(), any(Tags.class))).thenReturn(timer);
        lenient().when(structuredAnalysisPromptBuilder.getPromptVersion()).thenReturn("structured-qhse-v4");
    }

    @Test
    void analyzeStructured_shouldReturnFailedForEmptyData() {
        var result = analysisAgent.analyzeStructured(List.of());

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getFallbackReason()).contains("Aucune donnée KPI disponible");
    }

    @Test
    void analyzeStructured_shouldReturnFailedForNullResponse() {
        var kpiData = List.of(KpiCalculatedDTO.builder()
                .kpiName("Test KPI")
                .variationPercentage(10.0)
                .build());

        when(structuredAnalysisPromptBuilder.buildPrompt(any())).thenReturn("test prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false))).thenReturn(null);

        var result = analysisAgent.analyzeStructured(kpiData);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getFallbackReason()).contains("Aucune réponse du provider IA");
    }

    @Test
    void analyzeStructured_shouldReturnFailedForInvalidJson() {
        var kpiData = List.of(KpiCalculatedDTO.builder()
                .kpiName("Test KPI")
                .variationPercentage(10.0)
                .build());

        when(structuredAnalysisPromptBuilder.buildPrompt(any())).thenReturn("test prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false))).thenReturn(new LlmProviderChain.ProviderResult("invalid json", "groq"));

        var result = analysisAgent.analyzeStructured(kpiData);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getFallbackReason()).contains("Impossible de parser la réponse IA");
    }

    @Test
    void analyzeStructured_shouldReturnSuccessForValidResponse() {
        var kpiData = List.of(KpiCalculatedDTO.builder()
                .kpiName("Test KPI")
                .variationPercentage(10.0)
                .build());

        String validJson = """
                {
                    "globalSummary": "Test summary",
                    "confidence": {"overall": 85.0, "sections": {"summary": 80.0}},
                    "probableCauses": ["Cause 1"],
                    "recommendations": [{"title": "Rec 1", "rationale": "Rationale", "expectedBenefit": "Benefit", "urgency": "HIGH"}],
                    "actionPlan": [{"action": "Action 1", "priority": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}],
                    "traceability": {"modelName": "test", "generatedAt": "2023-01-01T00:00:00Z", "contextSourcesUsed": [{"sourceName": "source", "relevanceScore": 80}]},
                    "kpiInsights": [{"kpiName": "Test KPI", "confidence": 75.0, "insight": "Insight", "probableCauses": [], "recommendations": [], "actionImmediate": "Action", "urgency": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}]
                }
                """;

        when(structuredAnalysisPromptBuilder.buildPrompt(any())).thenReturn("test prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false))).thenReturn(new LlmProviderChain.ProviderResult(validJson, "groq"));
        when(structuredAnalysisValidator.validate(any(), any())).thenReturn(List.of());

        var result = analysisAgent.analyzeStructured(kpiData);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getGlobalSummary()).isEqualTo("Test summary");
        assertThat(result.getSchemaVersion()).isEqualTo("1.1");
        assertThat(result.getPromptVersion()).isEqualTo("structured-qhse-v4");
    }

    @Test
    void analyzeStructured_shouldRetryOnValidationFailure() {
        var kpiData = List.of(KpiCalculatedDTO.builder()
                .kpiName("Test KPI")
                .variationPercentage(10.0)
                .build());

        String invalidJson = """
                {
                    "globalSummary": "",
                    "confidence": {"overall": 85.0, "sections": {"summary": 80.0}},
                    "probableCauses": [],
                    "recommendations": [],
                    "actionPlan": [],
                    "traceability": {"modelName": "test", "generatedAt": "2023-01-01T00:00:00Z", "contextSourcesUsed": []},
                    "kpiInsights": []
                }
                """;

        String validJson = """
                {
                    "globalSummary": "Test summary after retry",
                    "confidence": {"overall": 85.0, "sections": {"summary": 80.0}},
                    "probableCauses": ["Cause 1"],
                    "recommendations": [{"title": "Rec 1", "rationale": "Rationale", "expectedBenefit": "Benefit", "urgency": "HIGH"}],
                    "actionPlan": [{"action": "Action 1", "priority": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}],
                    "traceability": {"modelName": "test", "generatedAt": "2023-01-01T00:00:00Z", "contextSourcesUsed": [{"sourceName": "source", "relevanceScore": 80}]},
                    "kpiInsights": [{"kpiName": "Test KPI", "confidence": 75.0, "insight": "Insight", "probableCauses": [], "recommendations": [], "actionImmediate": "Action", "urgency": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}]
                }
                """;

        when(structuredAnalysisPromptBuilder.buildPrompt(any())).thenReturn("test prompt");
        when(structuredAnalysisPromptBuilder.buildRetryPrompt("test prompt", "validation errors")).thenReturn("retry prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false))).thenReturn(new LlmProviderChain.ProviderResult(invalidJson, "groq"));
        when(llmProviderChain.generate(eq("retry prompt"), anyString(), eq(false))).thenReturn(new LlmProviderChain.ProviderResult(validJson, "groq"));
        when(structuredAnalysisValidator.validate(any(), any())).thenReturn(List.of("validation errors"), List.of());

        var result = analysisAgent.analyzeStructured(kpiData);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getGlobalSummary()).isEqualTo("Test summary after retry");
    }

    @Test
    void analyzeStructured_shouldReturnPartialOnRetryFailure() {
        var kpiData = List.of(KpiCalculatedDTO.builder()
                .kpiName("Test KPI")
                .variationPercentage(10.0)
                .build());

        String invalidJson = """
                {
                    "globalSummary": "",
                    "confidence": {"overall": 85.0, "sections": {"summary": 80.0}},
                    "probableCauses": [],
                    "recommendations": [],
                    "actionPlan": [],
                    "traceability": {"modelName": "test", "generatedAt": "2023-01-01T00:00:00Z", "contextSourcesUsed": []},
                    "kpiInsights": []
                }
                """;

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList())).thenReturn("test prompt");
        when(structuredAnalysisPromptBuilder.buildRetryPrompt("test prompt", "validation errors")).thenReturn("retry prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false))).thenReturn(new LlmProviderChain.ProviderResult(invalidJson, "groq"));
        when(llmProviderChain.generate(eq("retry prompt"), anyString(), eq(false))).thenReturn(new LlmProviderChain.ProviderResult("invalid json after retry", "groq"));
        when(structuredAnalysisValidator.validate(any(), any())).thenReturn(List.of("validation errors"));

        var result = analysisAgent.analyzeStructured(kpiData);

        assertThat(result.getStatus()).isEqualTo("PARTIAL");
        assertThat(result.getFallbackReason()).contains("Impossible de parser la réponse IA après retry");
    }

    @Test
    void analyzeStructured_shouldOverwriteTraceabilityWithServerMetadata() {
        var kpiData = List.of(KpiCalculatedDTO.builder()
                .kpiName("Test KPI")
                .variationPercentage(10.0)
                .build());

        String validJson = """
                {
                    "globalSummary": "Test summary",
                    "confidence": {"overall": 85.0, "sections": {"summary": 80.0}},
                    "probableCauses": ["Cause 1"],
                    "recommendations": [{"title": "Rec 1", "rationale": "Rationale", "expectedBenefit": "Benefit", "urgency": "HIGH"}],
                    "actionPlan": [{"action": "Action 1", "priority": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}],
                    "traceability": {"modelName": "fake-llm", "generatedAt": "2024-01-01T00:00:00Z", "contextSourcesUsed": [{"sourceName": "source", "relevanceScore": 80}]},
                    "kpiInsights": [{"kpiId": 42, "kpiName": "Test KPI", "confidence": 75.0, "insight": "Insight", "probableCauses": [], "recommendations": [], "actionImmediate": "Action", "urgency": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}]
                }
                """;

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList())).thenReturn("test prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(validJson, "gemini"));
        when(structuredAnalysisValidator.validate(any(), any())).thenReturn(List.of());

        var before = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1);
        var result = analysisAgent.analyzeStructured(kpiData, 987L);

        assertThat(result.getTraceability().getModelName()).isEqualTo("gemini");
        assertThat(java.time.OffsetDateTime.parse(result.getTraceability().getGeneratedAt())).isAfterOrEqualTo(before);
        assertThat(result.getTraceability().getSchemaVersion()).isEqualTo("1.1");
        assertThat(result.getTraceability().getPromptVersion()).isEqualTo("structured-qhse-v4");
        assertThat(result.getTraceability().getImportSessionId()).isEqualTo(987L);
    }

        @Test
        void analyzeStructured_shouldRetryUntilTenKpisAreCovered() {
        var kpiData = IntStream.rangeClosed(1, 10)
            .mapToObj(index -> KpiCalculatedDTO.builder()
                .kpiName("KPI " + index)
                .variationPercentage((double) index)
                .build())
            .toList();

        String firstJson = buildStructuredJson(4, "Test summary initial");
        String retryJson = buildStructuredJson(10, "Test summary retry");

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList())).thenReturn("test prompt");
        when(structuredAnalysisPromptBuilder.buildRetryPrompt("test prompt", "coverage incomplete"))
            .thenReturn("retry prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false)))
            .thenReturn(new LlmProviderChain.ProviderResult(firstJson, "groq"));
        when(llmProviderChain.generate(eq("retry prompt"), anyString(), eq(false)))
            .thenReturn(new LlmProviderChain.ProviderResult(retryJson, "gemini"));
        when(structuredAnalysisValidator.validate(any(), any()))
            .thenReturn(List.of("coverage incomplete"), List.of());

        var result = analysisAgent.analyzeStructured(kpiData, 321L);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getKpiInsights()).hasSize(10);
        assertThat(result.getTraceability().getModelName()).isEqualTo("gemini");
        assertThat(result.getTraceability().getImportSessionId()).isEqualTo(321L);
        }

        @Test
        void analyzeStructured_shouldReturnPartialWhenCoverageStaysIncompleteAfterRetry() {
        var kpiData = IntStream.rangeClosed(1, 10)
            .mapToObj(index -> KpiCalculatedDTO.builder()
                .kpiName("KPI " + index)
                .variationPercentage((double) index)
                .build())
            .toList();

        String firstJson = buildStructuredJson(4, "Test summary initial");
        String retryJson = buildStructuredJson(4, "Test summary retry still partial");

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList())).thenReturn("test prompt");
        when(structuredAnalysisPromptBuilder.buildRetryPrompt("test prompt", "coverage incomplete"))
            .thenReturn("retry prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false)))
            .thenReturn(new LlmProviderChain.ProviderResult(firstJson, "groq"));
        when(llmProviderChain.generate(eq("retry prompt"), anyString(), eq(false)))
            .thenReturn(new LlmProviderChain.ProviderResult(retryJson, "groq"));
        when(structuredAnalysisValidator.validate(any(), any()))
            .thenReturn(List.of("coverage incomplete"), List.of("coverage incomplete"));

        var result = analysisAgent.analyzeStructured(kpiData, 322L);

        assertThat(result.getStatus()).isEqualTo("PARTIAL");
        assertThat(result.getFallbackReason()).contains("coverage incomplete");
        }

        private String buildStructuredJson(int insightCount, String summary) {
        StringBuilder insights = new StringBuilder();
        for (int index = 1; index <= insightCount; index++) {
            if (index > 1) {
            insights.append(",");
            }
            insights.append(String.format("""
                {"kpiId": %d, "kpiName": "KPI %d", "confidence": 75.0, "insight": "Insight %d", "probableCauses": ["Cause %d"], "recommendations": ["Rec %d"], "actionImmediate": "Action %d", "urgency": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}
                """, index, index, index, index, index, index).trim());
        }

        return String.format("""
            {
                "globalSummary": "%s",
                "confidence": {"overall": 85.0, "sections": {"summary": 80.0, "probableCauses": 80.0, "recommendations": 80.0, "actionPlan": 80.0}},
                "probableCauses": ["Cause 1"],
                "recommendations": [{"title": "Rec 1", "rationale": "Rationale", "expectedBenefit": "Benefit", "urgency": "HIGH"}],
                "actionPlan": [{"action": "Action 1", "priority": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}],
                "traceability": {"modelName": "fake", "generatedAt": "2024-01-01T00:00:00Z", "contextSourcesUsed": [{"sourceName": "source", "relevanceScore": 80}]},
                "kpiInsights": [%s]
            }
            """, summary, insights.toString());
        }
}