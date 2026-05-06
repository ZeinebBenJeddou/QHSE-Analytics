package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.dto.response.ImportIssue;
import com.QHSEAnalytics.dto.response.ImportQualityReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QualityReportBuilderTest {

    private final QualityReportBuilder builder = new QualityReportBuilder();

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
