package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import com.QHSEAnalytics.importer.service.processing.ExtractionAgent;
import com.QHSEAnalytics.importer.service.processing.ExcelFileValidator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExtractionAgentTest {

    @Test
    void testParseNumberRobust() throws Exception {
        ExtractionAgent agent = new ExtractionAgent(new ExcelFileValidator());
        Method method = ExtractionAgent.class.getDeclaredMethod("parseNumberRobust", String.class, int.class, String.class, List.class);
        method.setAccessible(true);


        List<ImportIssue> issues1 = new ArrayList<>();
        Object result1 = method.invoke(agent, "12%", 1, "Val", issues1);
        Double value1 = getResultValue(result1);
        assertEquals(12.0, value1);


        List<ImportIssue> issues2 = new ArrayList<>();
        Object result2 = method.invoke(agent, "1 234,56", 2, "Val", issues2);
        Double value2 = getResultValue(result2);
        assertEquals(1234.56, value2);


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

    @Test
    void headersOpaques_avecMappingValide_emitLowConfidenceIssue() throws Exception {
        // Construit un fichier .xlsx avec headers "Donnée A | Donnée B | Donnée C"
        // et 3 lignes de données numériques — simule le cas 7 de l'audit.
        XSSFWorkbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet("Data");
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Donnée A");
        headerRow.createCell(1).setCellValue("Donnée B");
        headerRow.createCell(2).setCellValue("Donnée C");
        for (int i = 1; i <= 3; i++) {
            var row = sheet.createRow(i);
            row.createCell(0).setCellValue("KPI " + i);
            row.createCell(1).setCellValue(10.0 * i);
            row.createCell(2).setCellValue(8.0 * i);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        wb.close();

        MockMultipartFile file = new MockMultipartFile(
                "file", "cas7.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray());

        // Mapping valide issu du fallback NUMERIC de ColumnProfileService :
        // col 0 = KPI, col 2 = VALUE_N, col 1 = VALUE_N1
        Map<String, Integer> mapping = Map.of(
                ExtractionAgent.MAPPING_KPI_NAME_INDEX,  0,
                ExtractionAgent.MAPPING_VALUE_N_INDEX,   2,
                ExtractionAgent.MAPPING_VALUE_N1_INDEX,  1
        );

        ExtractionAgent agent = new ExtractionAgent(new ExcelFileValidator());
        ExtractionAgent.ExtractionResult result = agent.extract(file, mapping);

        assertTrue(
            result.getExtractionIssues().stream()
                .anyMatch(i -> ImportIssue.CODE_LOW_CONFIDENCE.equals(i.getCode())),
            "Une issue LOW_CONFIDENCE_EXTRACTION doit être émise pour des headers opaques " +
            "même quand le mapping frontend est valide"
        );
    }

    private Double getResultValue(Object parseResult) throws Exception {
        if (parseResult == null) return null;
        Field valueField = parseResult.getClass().getDeclaredField("value");
        valueField.setAccessible(true);
        return (Double) valueField.get(parseResult);
    }
}
