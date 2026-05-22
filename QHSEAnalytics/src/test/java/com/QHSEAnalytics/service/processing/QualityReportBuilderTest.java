package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import com.QHSEAnalytics.shared.dto.response.ImportQualityReport;
import com.QHSEAnalytics.importer.service.processing.QualityReportBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class QualityReportBuilderTest {

    private final QualityReportBuilder builder = new QualityReportBuilder();

    @BeforeEach
    void injectValues() {
        ReflectionTestUtils.setField(builder, "scoreValid",            100);
        ReflectionTestUtils.setField(builder, "scoreWarning",           70);
        ReflectionTestUtils.setField(builder, "scoreInvalid",            0);
        ReflectionTestUtils.setField(builder, "maxIssuesPerSeverity",  200);
    }

    @Test
    void testBuild_strictMode_withDuplicatesAndWarnings() {
        KpiRawDataDTO row1 = KpiRawDataDTO.builder()
                .rowIndex(1)
                .valid(true)
                .issues(List.of(
                        ImportIssue.builder().code(ImportIssue.CODE_DUPLICATE_KPI).severity(ImportIssue.Severity.INFO).build(),
                        ImportIssue.builder().code(ImportIssue.CODE_DUPLICATE_CONFLICT).severity(ImportIssue.Severity.WARNING).build()
                ))
                .build();

        ImportQualityReport report = builder.build(List.of(row1), false);

        assertEquals("STRICT", report.getImportMode());
        assertEquals(2, report.getDuplicateRows(), "duplicateRows devrait compter les occurrences écartées");
        assertEquals(3, report.getTotalRows(), "totalRows devrait être uniqueRows + duplicateRows");
        assertEquals(1, report.getValidRows());
        assertEquals(70.0, report.getQualityScore());
        assertFalse(report.isBlocking());
        assertFalse(report.isHardBlocking());
        assertFalse(report.isSoftBlocking());
        assertEquals(1, report.getImportedRowsCount());
        assertEquals(0, report.getRejectedRowsCount());
        assertTrue(report.getRejectedRowIndexes().isEmpty());
        assertTrue(report.getRejectedReasons().isEmpty());
    }

    @Test
    void testBuild_partialMode_withRowError() {
        KpiRawDataDTO row1 = KpiRawDataDTO.builder()
                .rowIndex(2)
                .valid(false)
                .issues(List.of(
                        ImportIssue.builder().rowIndex(2).code("SOME_ERROR").severity(ImportIssue.Severity.ERROR).build()
                ))
                .build();

        ImportQualityReport report = builder.build(List.of(row1), true);

        assertEquals("PARTIAL", report.getImportMode());
        assertEquals(0.0, report.getQualityScore(), "ERROR => 0");
        assertFalse(report.isHardBlocking());
        assertTrue(report.isSoftBlocking());
        assertFalse(report.isBlocking());
        assertEquals(0, report.getImportedRowsCount());
        assertEquals(1, report.getRejectedRowsCount());
        assertEquals(List.of(2), report.getRejectedRowIndexes());
        assertEquals(1, report.getRejectedReasons().size());
        assertEquals("SOME_ERROR", report.getRejectedReasons().get(0).getCode());
        assertEquals(1, report.getRejectedReasons().get(0).getCount());
    }

    @Test
    void testBuild_partialMode_withHardBlocking() {
        KpiRawDataDTO row1 = KpiRawDataDTO.builder()
                .rowIndex(3)
                .valid(true)
                .issues(List.of(
                        ImportIssue.builder().rowIndex(null).code(ImportIssue.CODE_EMPTY_FILE).severity(ImportIssue.Severity.ERROR).build()
                ))
                .build();

        ImportQualityReport report = builder.build(List.of(row1), true);

        assertEquals("PARTIAL", report.getImportMode());
        assertEquals(0.0, report.getQualityScore(), "Une issue ERROR fait chuter le score de la ligne");
        assertTrue(report.isHardBlocking());
        assertTrue(report.isBlocking());
        assertEquals(0, report.getImportedRowsCount());
        assertEquals(1, report.getRejectedRowsCount());
                assertEquals(List.of(3), report.getRejectedRowIndexes());
        assertEquals(1, report.getErrors().size());
        assertEquals(ImportIssue.CODE_EMPTY_FILE, report.getErrors().get(0).getCode());
    }

    // ── Truncation tests ──────────────────────────────────────────────────────

    private static List<KpiRawDataDTO> buildInvalidRows(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> KpiRawDataDTO.builder()
                        .rowIndex(i)
                        .valid(false)
                        .issues(List.of(ImportIssue.builder()
                                .rowIndex(i)
                                .code("SOME_ERROR")
                                .severity(ImportIssue.Severity.ERROR)
                                .build()))
                        .build())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static List<KpiRawDataDTO> buildWarningRows(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> KpiRawDataDTO.builder()
                        .rowIndex(i)
                        .valid(true)
                        .issues(List.of(ImportIssue.builder()
                                .rowIndex(i)
                                .code("SOME_WARNING")
                                .severity(ImportIssue.Severity.WARNING)
                                .build()))
                        .build())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    @Test
    void testBuild_150InvalidRows_noTruncation() {
        ImportQualityReport report = builder.build(buildInvalidRows(150), true);

        assertEquals(150, report.getErrors().size(), "Sous la limite : toutes les errors retournées");
        assertFalse(report.isIssuesTruncated(), "issuesTruncated doit être false");
        assertTrue(report.getInfos().stream().noneMatch(i -> "ISSUES_TRUNCATED".equals(i.getCode())),
                "Aucune issue ISSUES_TRUNCATED attendue");
    }

    @Test
    void testBuild_250InvalidRows_errorsCapped() {
        ImportQualityReport report = builder.build(buildInvalidRows(250), true);

        assertEquals(200, report.getErrors().size(), "Cap à 200 errors");
        assertTrue(report.isIssuesTruncated(), "issuesTruncated doit être true");
        assertTrue(report.getInfos().stream().anyMatch(i -> "ISSUES_TRUNCATED".equals(i.getCode())),
                "Une issue INFO ISSUES_TRUNCATED doit être présente");
    }

    @Test
    void testBuild_250Errors_250Warnings_bothCapped() {
        List<KpiRawDataDTO> rows = new ArrayList<>(buildInvalidRows(250));
        rows.addAll(buildWarningRows(250));

        ImportQualityReport report = builder.build(rows, true);

        assertEquals(200, report.getErrors().size(),   "Errors cappées à 200");
        assertEquals(200, report.getWarnings().size(), "Warnings cappées à 200");
        assertTrue(report.isIssuesTruncated(), "issuesTruncated doit être true");
    }

    // ── Edge case tests ───────────────────────────────────────────────────────

    private static KpiRawDataDTO buildInvalidRow(int rowIndex) {
        return KpiRawDataDTO.builder()
                .rowIndex(rowIndex)
                .kpiName("KPI " + rowIndex)
                .valid(false)
                .validationMessage("Erreur test")
                .issues(List.of(ImportIssue.builder()
                        .code(ImportIssue.CODE_MISSING_VALUE_N)
                        .severity(ImportIssue.Severity.ERROR)
                        .message("Valeur N manquante")
                        .rowIndex(rowIndex)
                        .build()))
                .build();
    }

    @Test
    void testBuild_emptyRawData_returnsZeroScoreNoBlocking() {
        ImportQualityReport report = builder.build(Collections.emptyList(), false);

        assertEquals(0.0,  report.getQualityScore(),        "Score 0 sur liste vide");
        assertEquals(0,    report.getTotalRows(),           "Aucune ligne");
        assertEquals(0,    report.getValidRows());
        assertEquals(0,    report.getInvalidRows());
        assertFalse(report.isBlocking(),                    "Pas de blocking sans données");
        assertTrue(report.getErrors().isEmpty(),            "Pas d'errors");
        assertTrue(report.getWarnings().isEmpty(),          "Pas de warnings");
        assertTrue(report.getInfos().isEmpty(),             "Pas d'infos");
    }

    @Test
    void testBuild_allRowsInvalid_strictBlockingPartialNot() {
        List<KpiRawDataDTO> rows = IntStream.rangeClosed(1, 5)
                .mapToObj(QualityReportBuilderTest::buildInvalidRow)
                .toList();

        // Mode STRICT : softBlocking bloque
        ImportQualityReport strict = builder.build(new ArrayList<>(rows), false);
        assertEquals(0.0, strict.getQualityScore(),   "Toutes invalides → score 0");
        assertEquals(0,   strict.getValidRows());
        assertEquals(5,   strict.getInvalidRows());
        assertTrue(strict.isSoftBlocking(),            "softBlocking doit être true");
        assertTrue(strict.isBlocking(),                "Bloquant en mode STRICT");

        // Mode PARTIAL : softBlocking ignoré, seul hardBlocking bloque
        ImportQualityReport partial = builder.build(new ArrayList<>(rows), true);
        assertFalse(partial.isBlocking(),              "Non bloquant en mode PARTIAL (pas de hardBlocking)");
        assertEquals(0,   partial.getImportedRowsCount(), "0 ligne importable");
        assertEquals(5,   partial.getRejectedRowsCount(), "5 lignes rejetées");
    }

    @Test
    void testBuild_hardBlockingBeyondCap_detectedOnFullList() {
        // 200 softBlocking rows + 1 hardBlocking row au-delà du cap
        List<KpiRawDataDTO> rows = new ArrayList<>(buildInvalidRows(200));
        rows.add(KpiRawDataDTO.builder()
                .rowIndex(201)
                .valid(false)
                .issues(List.of(ImportIssue.builder()
                        .rowIndex(null)                          // rowIndex null → hardBlocking
                        .code(ImportIssue.CODE_EMPTY_FILE)
                        .severity(ImportIssue.Severity.ERROR)
                        .build()))
                .build());

        ImportQualityReport report = builder.build(rows, true);

        assertEquals(200, report.getErrors().size(),   "Errors cappées à 200 dans la réponse");
        assertTrue(report.isIssuesTruncated(),         "issuesTruncated = true");
        assertTrue(report.isHardBlocking(),            "hardBlocking détecté sur la 201ème issue, hors cap");
        assertTrue(report.isBlocking(),                "Bloquant même en mode PARTIAL car hardBlocking");
    }

    @Test
    void testBuild_rowWithNullIssues_noNullPointerException() {
        KpiRawDataDTO rowWithNullIssues = KpiRawDataDTO.builder()
                .rowIndex(1)
                .kpiName("KPI null-issues")
                .valid(true)
                .build();

        assertDoesNotThrow(() -> {
            ImportQualityReport report = builder.build(List.of(rowWithNullIssues), false);
            assertEquals(100.0, report.getQualityScore(), "Ligne valide sans issues → score 100");
            assertEquals(1,     report.getValidRows());
            assertTrue(report.getErrors().isEmpty());
        });
    }

    @Test
    void testBuild_infoOnlyKeepsImportNonBlocking() {
        KpiRawDataDTO row1 = KpiRawDataDTO.builder()
                .rowIndex(4)
                .valid(true)
                .issues(List.of(
                        ImportIssue.builder().code("SOME_INFO").severity(ImportIssue.Severity.INFO).build()
                ))
                .build();

        ImportQualityReport report = builder.build(List.of(row1), false);

        assertEquals("STRICT", report.getImportMode());
        assertEquals(100.0, report.getQualityScore(), "INFO seul => 100");
        assertFalse(report.isBlocking());
        assertEquals(1, report.getImportedRowsCount());
        assertEquals(0, report.getRejectedRowsCount());
    }
}
