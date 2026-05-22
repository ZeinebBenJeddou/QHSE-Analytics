package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import com.QHSEAnalytics.importer.service.processing.ExtractionAgent;
import com.QHSEAnalytics.importer.service.processing.ExcelFileValidator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
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
        ReflectionTestUtils.setField(agent, "mappingGoodThreshold", 0.70);
        ReflectionTestUtils.setField(agent, "lowConfidenceThreshold", 0.4);
        ExtractionAgent.ExtractionResult result = agent.extract(file, mapping);

        assertTrue(
            result.getExtractionIssues().stream()
                .anyMatch(i -> ImportIssue.CODE_LOW_CONFIDENCE.equals(i.getCode())),
            "Une issue LOW_CONFIDENCE_EXTRACTION doit être émise pour des headers opaques " +
            "même quand le mapping frontend est valide"
        );
    }

    // ── Tests MISSING_VALUE_TOKENS ──────────────────────────────────────────

    @Test
    void parseNumberRobust_NA_retourneNullSansIssue() throws Exception {
        ExtractionAgent agent = new ExtractionAgent(new ExcelFileValidator());
        Method method = ExtractionAgent.class.getDeclaredMethod(
                "parseNumberRobust", String.class, int.class, String.class, List.class);
        method.setAccessible(true);

        List<ImportIssue> issues = new ArrayList<>();
        Object result = method.invoke(agent, "N/A", 1, "Valeur N-1", issues);

        assertNull(getResultValue(result), "N/A doit retourner null");
        assertFalse(getResultIssueAdded(result), "N/A ne doit pas marquer issueAdded");
        assertTrue(issues.isEmpty(), "N/A ne doit produire aucune issue CODE_INVALID_NUMBER");
    }

    @Test
    void parseNumberRobust_tiretLong_retourneNullSansIssue() throws Exception {
        ExtractionAgent agent = new ExtractionAgent(new ExcelFileValidator());
        Method method = ExtractionAgent.class.getDeclaredMethod(
                "parseNumberRobust", String.class, int.class, String.class, List.class);
        method.setAccessible(true);

        List<ImportIssue> issues = new ArrayList<>();
        Object result = method.invoke(agent, "—", 1, "Valeur N-1", issues); // U+2014 —

        assertNull(getResultValue(result), "— (tiret long) doit retourner null");
        assertFalse(getResultIssueAdded(result), "— ne doit pas marquer issueAdded");
        assertTrue(issues.isEmpty(), "— ne doit produire aucune issue");
    }

    @Test
    void parseNumberRobust_nd_retourneNullSansIssue() throws Exception {
        ExtractionAgent agent = new ExtractionAgent(new ExcelFileValidator());
        Method method = ExtractionAgent.class.getDeclaredMethod(
                "parseNumberRobust", String.class, int.class, String.class, List.class);
        method.setAccessible(true);

        List<ImportIssue> issues = new ArrayList<>();
        Object result = method.invoke(agent, "nd", 2, "Valeur N", issues);

        assertNull(getResultValue(result), "nd doit retourner null");
        assertFalse(getResultIssueAdded(result), "nd ne doit pas marquer issueAdded");
        assertTrue(issues.isEmpty(), "nd ne doit produire aucune issue");
    }

    @Test
    void parseNumberRobust_neant_retourneNullSansIssue() throws Exception {
        ExtractionAgent agent = new ExtractionAgent(new ExcelFileValidator());
        Method method = ExtractionAgent.class.getDeclaredMethod(
                "parseNumberRobust", String.class, int.class, String.class, List.class);
        method.setAccessible(true);

        List<ImportIssue> issues = new ArrayList<>();
        Object result = method.invoke(agent, "néant", 3, "Valeur N-1", issues);

        assertNull(getResultValue(result), "néant doit retourner null");
        assertFalse(getResultIssueAdded(result), "néant ne doit pas marquer issueAdded");
        assertTrue(issues.isEmpty(), "néant ne doit produire aucune issue");
    }

    @Test
    void parseNumberRobust_texteInconnu_retourneNullAvecIssueInvalidNumber() throws Exception {
        ExtractionAgent agent = new ExtractionAgent(new ExcelFileValidator());
        Method method = ExtractionAgent.class.getDeclaredMethod(
                "parseNumberRobust", String.class, int.class, String.class, List.class);
        method.setAccessible(true);

        List<ImportIssue> issues = new ArrayList<>();
        Object result = method.invoke(agent, "abc123", 4, "Valeur N", issues);

        assertNull(getResultValue(result), "abc123 doit retourner null");
        assertTrue(getResultIssueAdded(result), "abc123 doit marquer issueAdded");
        assertFalse(issues.isEmpty(), "abc123 doit produire une issue d'erreur");
    }

    @Test
    void parseNumberRobust_virguleDecimale_retourne8point5() throws Exception {
        ExtractionAgent agent = new ExtractionAgent(new ExcelFileValidator());
        Method method = ExtractionAgent.class.getDeclaredMethod(
                "parseNumberRobust", String.class, int.class, String.class, List.class);
        method.setAccessible(true);

        List<ImportIssue> issues = new ArrayList<>();
        Object result = method.invoke(agent, "8,5", 5, "Valeur N", issues);

        assertEquals(8.5, getResultValue(result), "8,5 (virgule française) doit retourner 8.5");
        assertFalse(getResultIssueAdded(result));
    }

    @Test
    void parseNumberRobust_tiretCourt_retourneNullSansIssue() throws Exception {
        ExtractionAgent agent = new ExtractionAgent(new ExcelFileValidator());
        Method method = ExtractionAgent.class.getDeclaredMethod(
                "parseNumberRobust", String.class, int.class, String.class, List.class);
        method.setAccessible(true);

        List<ImportIssue> issues = new ArrayList<>();
        Object result = method.invoke(agent, "-", 6, "Valeur N-1", issues);

        assertNull(getResultValue(result), "- (tiret court seul) doit retourner null");
        assertFalse(getResultIssueAdded(result), "- ne doit pas marquer issueAdded");
        assertTrue(issues.isEmpty(), "- ne doit produire aucune issue");
    }

    // ── Helpers réflexion ────────────────────────────────────────────────────

    private Double getResultValue(Object parseResult) throws Exception {
        if (parseResult == null) return null;
        Field valueField = parseResult.getClass().getDeclaredField("value");
        valueField.setAccessible(true);
        return (Double) valueField.get(parseResult);
    }

    private boolean getResultIssueAdded(Object parseResult) throws Exception {
        Field field = parseResult.getClass().getDeclaredField("issueAdded");
        field.setAccessible(true);
        return (boolean) field.get(parseResult);
    }
}
