package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.response.ImportIssue;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExtractionAgentTest {

    @Test
    void testParseNumberRobust() throws Exception {
        ExtractionAgent agent = new ExtractionAgent();
        Method method = ExtractionAgent.class.getDeclaredMethod("parseNumberRobust", String.class, int.class, String.class, List.class);
        method.setAccessible(true);

        // "12%"
        List<ImportIssue> issues1 = new ArrayList<>();
        Object result1 = method.invoke(agent, "12%", 1, "Val", issues1);
        Double value1 = getResultValue(result1);
        assertEquals(12.0, value1);

        // "1 234,56"
        List<ImportIssue> issues2 = new ArrayList<>();
        Object result2 = method.invoke(agent, "1 234,56", 2, "Val", issues2);
        Double value2 = getResultValue(result2);
        assertEquals(1234.56, value2);

        // "10/20" ambigu
        List<ImportIssue> issues3 = new ArrayList<>();
        Object result3 = method.invoke(agent, "10/20", 3, "Val", issues3);
        Double value3 = getResultValue(result3);
        assertNull(value3);
        assertTrue(issues3.stream().anyMatch(i -> i.getCode().equals(ImportIssue.CODE_AMBIGUOUS_NUMBER)));
    }

    @Test
    void testExtractionResultCarriesGlobalIssues() {
        ExtractionAgent.ExtractionResult result = new ExtractionAgent.ExtractionResult(
                List.of(),
                "AUTO",
                List.of("A", "B"),
                List.of(ImportIssue.builder().code("GLOBAL").severity(ImportIssue.Severity.WARNING).build())
        );

        assertEquals(1, result.getExtractionIssues().size());
        assertEquals("GLOBAL", result.getExtractionIssues().get(0).getCode());
    }

    private Double getResultValue(Object parseResult) throws Exception {
        if (parseResult == null) return null;
        Field valueField = parseResult.getClass().getDeclaredField("value");
        valueField.setAccessible(true);
        return (Double) valueField.get(parseResult);
    }
}
