package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.analytics.service.LlmProviderChain;
import com.QHSEAnalytics.analytics.service.processing.AnalysisAgent;
import com.QHSEAnalytics.analytics.service.processing.StructuredAnalysisPromptBuilder;
import com.QHSEAnalytics.analytics.service.processing.StructuredAnalysisValidator;
import com.QHSEAnalytics.shared.dto.response.AiAnalysisStructuredResponse;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.entity.ImportSession;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList(), anyInt(), anyList(), nullable(ImportSession.class))).thenReturn("test prompt");
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

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList(), anyInt(), anyList(), nullable(ImportSession.class))).thenReturn("test prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult("invalid json", "groq"));

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
                    "traceability": {"modelName": "test", "generatedAt": "2023-01-01T00:00:00Z"},
                    "kpiInsights": [{"kpiName": "Test KPI", "confidence": 75.0, "insight": "Insight detaille sur la variation observee avec impact operationnel concret.", "probableCauses": [], "recommendations": [], "actionImmediate": "Action immediate detaillee a lancer avec responsable et delai clairs.", "urgency": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}]
                }
                """;

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList(), anyInt(), anyList(), nullable(ImportSession.class))).thenReturn("test prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(validJson, "groq"));
        when(structuredAnalysisValidator.validate(any(), any())).thenReturn(List.of());

        var result = analysisAgent.analyzeStructured(kpiData);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getGlobalSummary()).isEqualTo("Test summary");
        assertThat(result.getSchemaVersion()).isEqualTo("1.2");
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
                    "traceability": {"modelName": "test", "generatedAt": "2023-01-01T00:00:00Z"},
                    "kpiInsights": [{"kpiName": "Test KPI", "confidence": 75.0, "insight": "Insight detaille mais volontairement invalide pour declencher le retry metier.", "probableCauses": [], "recommendations": [], "actionImmediate": "Action immediate detaillee pour eviter un classement generique du resultat.", "urgency": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}]
                }
                """;

        String validJson = """
                {
                    "globalSummary": "Test summary after retry",
                    "confidence": {"overall": 85.0, "sections": {"summary": 80.0}},
                    "probableCauses": ["Cause 1"],
                    "recommendations": [{"title": "Rec 1", "rationale": "Rationale", "expectedBenefit": "Benefit", "urgency": "HIGH"}],
                    "actionPlan": [{"action": "Action 1", "priority": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}],
                    "traceability": {"modelName": "test", "generatedAt": "2023-01-01T00:00:00Z"},
                    "kpiInsights": [{"kpiName": "Test KPI", "confidence": 75.0, "insight": "Insight detaille apres retry avec cause probable et consequence metier explicites.", "probableCauses": [], "recommendations": [], "actionImmediate": "Action immediate detaillee apres retry avec etapes concretes a suivre.", "urgency": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}]
                }
                """;

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList(), anyInt(), anyList(), nullable(ImportSession.class))).thenReturn("test prompt");
        when(structuredAnalysisPromptBuilder.buildRetryPrompt(eq("test prompt"), anyString())).thenReturn("retry prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(invalidJson, "groq"));
        when(llmProviderChain.generate(eq("retry prompt"), anyString(), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(validJson, "groq"));
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
                    "traceability": {"modelName": "test", "generatedAt": "2023-01-01T00:00:00Z"},
                    "kpiInsights": [{"kpiName": "Test KPI", "confidence": 75.0, "insight": "Insight detaille mais conserve un contenu invalide pour le validateur metier.", "probableCauses": [], "recommendations": [], "actionImmediate": "Action immediate detaillee afin de passer le filtre de genericite du service.", "urgency": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}]
                }
                """;

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList(), anyInt(), anyList(), nullable(ImportSession.class))).thenReturn("test prompt");
        when(structuredAnalysisPromptBuilder.buildRetryPrompt(eq("test prompt"), anyString())).thenReturn("retry prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(invalidJson, "groq"));
        when(llmProviderChain.generate(eq("retry prompt"), anyString(), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult("invalid json after retry", "groq"));
        when(structuredAnalysisValidator.validate(any(), any())).thenReturn(List.of("validation errors"));

        var result = analysisAgent.analyzeStructured(kpiData);

        assertThat(result.getStatus()).isEqualTo("FAILED");
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
                    "traceability": {"modelName": "fake-llm", "generatedAt": "2024-01-01T00:00:00Z"},
                    "kpiInsights": [{"kpiId": 42, "kpiName": "Test KPI", "confidence": 75.0, "insight": "Insight detaille qui doit etre conserve mais avec une traceabilite ecrasee par le serveur.", "probableCauses": [], "recommendations": [], "actionImmediate": "Action immediate detaillee qui satisfait les controles de richesse du contenu.", "urgency": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}]
                }
                """;

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList(), anyInt(), anyList(), nullable(ImportSession.class))).thenReturn("test prompt");
        when(llmProviderChain.generate(eq("test prompt"), anyString(), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(validJson, "groq"));
        when(structuredAnalysisValidator.validate(any(), any())).thenReturn(List.of());

        var before = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1);
        var result = analysisAgent.analyzeStructured(kpiData, 987L);

        assertThat(result.getTraceability().getModelName()).isEqualTo("groq");
        assertThat(java.time.OffsetDateTime.parse(result.getTraceability().getGeneratedAt())).isAfterOrEqualTo(before);
        assertThat(result.getTraceability().getSchemaVersion()).isEqualTo("1.2");
        assertThat(result.getTraceability().getPromptVersion()).isEqualTo("structured-qhse-v4");
        assertThat(result.getTraceability().getImportSessionId()).isEqualTo(987L);
    }

    @Test
    void analyzeStructured_shouldProcessEighteenKpisInFourChunksAndMergeCoverage() {
        var kpiData = IntStream.rangeClosed(1, 18)
                .mapToObj(index -> KpiCalculatedDTO.builder()
                        .kpiName("KPI " + index)
                        .variationPercentage(100.0 - index)
                        .build())
                .toList();

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList(), anyInt(), anyList(), nullable(ImportSession.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<KpiCalculatedDTO> chunk = invocation.getArgument(0);
            return "prompt-" + chunk.get(0).getKpiName();
        });
        when(llmProviderChain.generate(eq("prompt-KPI 1"), contains("chunk=1"), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(buildStructuredJsonRange(1, 5, "Summary 1"), "groq"));
        when(llmProviderChain.generate(eq("prompt-KPI 6"), contains("chunk=2"), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(buildStructuredJsonRange(6, 10, "Summary 2"), "groq"));
        when(llmProviderChain.generate(eq("prompt-KPI 11"), contains("chunk=3"), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(buildStructuredJsonRange(11, 15, "Summary 3"), "groq"));
        when(llmProviderChain.generate(eq("prompt-KPI 16"), contains("chunk=4"), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(buildStructuredJsonRange(16, 18, "Summary 4"), "groq"));
        when(structuredAnalysisValidator.validate(any(), any())).thenReturn(List.of());

        var result = analysisAgent.analyzeStructured(kpiData, 321L);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getKpiInsights()).hasSize(18);
        assertThat(result.getGlobalSummary()).isNotBlank();
        assertThat(result.getTraceability().getModelName()).isEqualTo("groq");
        assertThat(result.getTraceability().getImportSessionId()).isEqualTo(321L);
        verify(structuredAnalysisPromptBuilder, times(4)).buildPrompt(anyList(), anyInt(), anyList(), nullable(ImportSession.class));
        verify(llmProviderChain).generate(eq("prompt-KPI 1"), contains("chunk=1"), eq(false));
        verify(llmProviderChain).generate(eq("prompt-KPI 6"), contains("chunk=2"), eq(false));
        verify(llmProviderChain).generate(eq("prompt-KPI 11"), contains("chunk=3"), eq(false));
        verify(llmProviderChain).generate(eq("prompt-KPI 16"), contains("chunk=4"), eq(false));
    }

    @Test
    void analyzeStructured_shouldReturnPartialWhenChunkCoverageIsIncomplete() {
        var kpiData = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> KpiCalculatedDTO.builder()
                        .kpiName("KPI " + index)
                        .variationPercentage(100.0 - index)
                        .build())
                .toList();

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList(), anyInt(), anyList(), nullable(ImportSession.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<KpiCalculatedDTO> chunk = invocation.getArgument(0);
            return "prompt-" + chunk.get(0).getKpiName();
        });
        when(llmProviderChain.generate(eq("prompt-KPI 1"), contains("chunk=1"), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(buildStructuredJsonRange(1, 4, "Summary 1 partial"), "groq"));
        when(llmProviderChain.generate(eq("prompt-KPI 6"), contains("chunk=2"), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(buildStructuredJsonRange(6, 10, "Summary 2"), "groq"));
        when(structuredAnalysisValidator.validate(any(), any())).thenReturn(List.of());

        var result = analysisAgent.analyzeStructured(kpiData, 322L);

        assertThat(result.getStatus()).isEqualTo("PARTIAL");
        assertThat(result.getFallbackReason()).contains("coverage incomplete after chunk merge: missing 1 KPI(s)");
        assertThat(result.getKpiInsights()).hasSize(9);
    }

    @Test
    void analyzeStructured_shouldReturnPartialWhenChunkCoverageRemainsIncomplete() {
        var kpiData = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> KpiCalculatedDTO.builder()
                        .kpiName("KPI " + index)
                        .variationPercentage(100.0 - index)
                        .build())
                .toList();

        when(structuredAnalysisPromptBuilder.buildPrompt(anyList(), anyInt(), anyList(), nullable(ImportSession.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<KpiCalculatedDTO> chunk = invocation.getArgument(0);
            return "prompt-" + chunk.get(0).getKpiName();
        });
        when(llmProviderChain.generate(eq("prompt-KPI 1"), contains("chunk=1"), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(buildStructuredJsonRange(1, 4, "Summary 1 partial"), "groq"));
        when(llmProviderChain.generate(eq("prompt-KPI 6"), contains("chunk=2"), eq(false)))
                .thenReturn(new LlmProviderChain.ProviderResult(buildStructuredJsonRange(6, 10, "Summary 2"), "groq"));
        when(structuredAnalysisValidator.validate(any(), any())).thenReturn(List.of());

        var result = analysisAgent.analyzeStructured(kpiData, 323L);

        assertThat(result.getStatus()).isEqualTo("PARTIAL");
        assertThat(result.getFallbackReason()).contains("coverage incomplete after chunk merge: missing 1 KPI(s)");
        assertThat(result.getKpiInsights()).hasSize(9);
    }

    private String buildStructuredJsonRange(int startIndex, int endIndex, String summary) {
        StringBuilder insights = new StringBuilder();
        for (int index = startIndex; index <= endIndex; index++) {
            if (index > startIndex) {
                insights.append(",");
            }
            insights.append(String.format("""
                {"kpiId": %d, "kpiName": "KPI %d", "confidence": 75.0, "insight": "Insight %d detaille avec impact operationnel concret et causes plausibles explicites.", "probableCauses": ["Cause %d"], "recommendations": ["Rec %d"], "actionImmediate": "Action %d detaillee a executer avec suivi, delai et responsable identifies.", "urgency": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}
                """, index, index, index, index, index, index).trim());
        }

        return String.format("""
            {
                "globalSummary": "%s",
                "confidence": {"overall": 85.0, "sections": {"summary": 80.0, "probableCauses": 80.0, "recommendations": 80.0, "actionPlan": 80.0}},
                "probableCauses": ["Cause 1"],
                "recommendations": [{"title": "Rec 1", "rationale": "Rationale", "expectedBenefit": "Benefit", "urgency": "HIGH"}],
                "actionPlan": [{"action": "Action 1", "priority": "HIGH", "ownerRole": "Owner", "dueHorizon": "Q1", "successMetric": "Metric", "riskIfNotDone": "Risk"}],
                "traceability": {"modelName": "fake", "generatedAt": "2024-01-01T00:00:00Z"},
                "kpiInsights": [%s]
            }
            """, summary, insights.toString());
    }
}
