package com.QHSEAnalytics.importer.service.processing;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HeaderDetectionUtil {

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)(\\d{2})\\b");
    private static final List<String> METADATA_MARKERS = List.of("qhse_analytics_template_v1", "template_v1", "template");

    private HeaderDetectionUtil() {
    }

    public static Optional<HeaderDetectionResult> detectHeaderRow(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (sheet == null) {
            return Optional.empty();
        }

        int firstRow = sheet.getFirstRowNum();
        int lastRow = Math.min(firstRow + 50, sheet.getLastRowNum());
        HeaderDetectionResult fallback = null;

        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || ExcelParserUtil.isRowBlank(row, formatter, evaluator)) {
                continue;
            }

            Map<Integer, ColumnRole> labels = classifyHeaderRow(row, formatter, evaluator);
            if (labels.containsValue(ColumnRole.KPI) && labels.containsValue(ColumnRole.VALUE_N)) {
                return Optional.of(buildResult(rowIndex, labels));
            }

            if (fallback == null && labels.containsValue(ColumnRole.KPI)) {
                fallback = buildResult(rowIndex, labels);
            }
        }

        return Optional.ofNullable(fallback);
    }

    public static Optional<HeaderDetectionResult> buildFallbackHeader(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (sheet == null) {
            return Optional.empty();
        }

        int firstRow = sheet.getFirstRowNum();
        int lastRow = Math.min(firstRow + 50, sheet.getLastRowNum());

        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || ExcelParserUtil.isRowBlank(row, formatter, evaluator)) {
                continue;
            }

            int nonBlank = ExcelParserUtil.countNonBlankCells(row, formatter, evaluator);
            if (nonBlank < 3) {
                continue;
            }

            int kpiIndex = findFirstNonEmptyCell(row, formatter, evaluator, 0);
            int firstValueIndex = findFirstNonEmptyCell(row, formatter, evaluator, kpiIndex + 1);
            int secondValueIndex = findFirstNonEmptyCell(row, formatter, evaluator, firstValueIndex + 1);

            if (kpiIndex >= 0 && firstValueIndex >= 0 && secondValueIndex >= 0) {
                return Optional.of(new HeaderDetectionResult(rowIndex, kpiIndex, secondValueIndex, firstValueIndex, null, true));
            }
        }

        return Optional.empty();
    }

    public static boolean hasTemplateMarker(Workbook workbook, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (workbook == null) {
            return false;
        }

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (sheet == null) {
                continue;
            }
            int maxRow = Math.min(sheet.getLastRowNum(), 20);
            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= maxRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                for (Cell cell : row) {
                    String value = ExcelParserUtil.getCellValue(cell, evaluator, formatter);
                    if (value == null) {
                        continue;
                    }
                    String normalized = ExcelParserUtil.normalizeText(value);
                    for (String marker : METADATA_MARKERS) {
                        if (normalized.contains(marker)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static Optional<Sheet> findDataSheetForTemplate(Workbook workbook, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (workbook == null) {
            return Optional.empty();
        }

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (sheet == null) {
                continue;
            }
            String sheetName = sheet.getSheetName();
            if (sheetName == null) {
                continue;
            }
            String normalized = sheetName.trim().toLowerCase(Locale.ROOT);
            if (normalized.contains("meta") || normalized.contains("template")) {
                continue;
            }
            if (!ExcelParserUtil.isRowBlank(sheet.getRow(sheet.getFirstRowNum()), formatter, evaluator)) {
                return Optional.of(sheet);
            }
        }

        return Optional.ofNullable(workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null);
    }

    private static Map<Integer, ColumnRole> classifyHeaderRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<Integer, ColumnRole> roles = new HashMap<>();
        for (Cell cell : row) {
            String value = ExcelParserUtil.getCellValue(cell, evaluator, formatter);
            String normalized = ExcelParserUtil.normalizeText(value);
            if (normalized.isBlank()) {
                continue;
            }

            ColumnRole role = classifyHeaderValue(normalized);
            if (role != ColumnRole.UNKNOWN) {
                roles.put(cell.getColumnIndex(), role);
            }
        }
        resolveYearRoles(roles, row, formatter, evaluator);
        return roles;
    }

    private static void resolveYearRoles(Map<Integer, ColumnRole> roles, Row row,
                                          DataFormatter formatter, FormulaEvaluator evaluator) {
        List<Map.Entry<Integer, Integer>> yearCols = new ArrayList<>();
        for (Cell cell : row) {
            String value = ExcelParserUtil.getCellValue(cell, evaluator, formatter);
            String normalized = ExcelParserUtil.normalizeText(value);
            if (normalized == null || normalized.isBlank()) continue;
            Matcher m = YEAR_PATTERN.matcher(normalized);
            if (m.find()) {
                int year = Integer.parseInt(m.group());
                yearCols.add(Map.entry(cell.getColumnIndex(), year));
            }
        }
        if (yearCols.size() < 2) return;
        yearCols.sort(Comparator.<Map.Entry<Integer, Integer>, Integer>comparing(Map.Entry::getValue).reversed());
        roles.put(yearCols.get(0).getKey(), ColumnRole.VALUE_N);
        roles.put(yearCols.get(1).getKey(), ColumnRole.VALUE_N1);
    }

    private static ColumnRole classifyHeaderValue(String normalizedValue) {
        return switch (ColumnSemanticResolver.resolve(normalizedValue).semantic()) {
            case KPI_NAME -> ColumnRole.KPI;
            case VALUE_N  -> ColumnRole.VALUE_N;
            case VALUE_N1 -> ColumnRole.VALUE_N1;
            default       -> ColumnRole.UNKNOWN;
        };
    }

    private static HeaderDetectionResult buildResult(int rowIndex, Map<Integer, ColumnRole> labels) {
        Integer kpiColumn = labels.entrySet().stream()
                .filter(entry -> entry.getValue() == ColumnRole.KPI)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);

        Integer valueNIndex = labels.entrySet().stream()
                .filter(entry -> entry.getValue() == ColumnRole.VALUE_N)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);

        Integer valueN1Index = labels.entrySet().stream()
                .filter(entry -> entry.getValue() == ColumnRole.VALUE_N1)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);

        return new HeaderDetectionResult(rowIndex, kpiColumn, valueNIndex, valueN1Index, null);
    }

    private static int findFirstNonEmptyCell(Row row, DataFormatter formatter, FormulaEvaluator evaluator, int start) {
        for (int columnIndex = start; columnIndex < row.getLastCellNum(); columnIndex++) {
            Cell cell = row.getCell(columnIndex);
            String value = ExcelParserUtil.getCellValue(cell, evaluator, formatter);
            if (value != null && !value.isBlank()) {
                return columnIndex;
            }
        }
        return -1;
    }

    public static final class HeaderDetectionResult {
        private final int headerRowIndex;
        private final int kpiColumnIndex;
        private final int valeurNColumnIndex;
        private final int valeurN1ColumnIndex;
        private final Integer categoryColumnIndex;
        private final boolean positionalFallback;

        public HeaderDetectionResult(int headerRowIndex,
                                     int kpiColumnIndex,
                                     int valeurNColumnIndex,
                                     int valeurN1ColumnIndex,
                                     Integer categoryColumnIndex) {
            this(headerRowIndex, kpiColumnIndex, valeurNColumnIndex, valeurN1ColumnIndex, categoryColumnIndex, false);
        }

        public HeaderDetectionResult(int headerRowIndex,
                                     int kpiColumnIndex,
                                     int valeurNColumnIndex,
                                     int valeurN1ColumnIndex,
                                     Integer categoryColumnIndex,
                                     boolean positionalFallback) {
            this.headerRowIndex = headerRowIndex;
            this.kpiColumnIndex = kpiColumnIndex;
            this.valeurNColumnIndex = valeurNColumnIndex;
            this.valeurN1ColumnIndex = valeurN1ColumnIndex;
            this.categoryColumnIndex = categoryColumnIndex;
            this.positionalFallback = positionalFallback;
        }

        public int getHeaderRowIndex() {
            return headerRowIndex;
        }

        public int getKpiColumnIndex() {
            return kpiColumnIndex;
        }

        public int getValeurNColumnIndex() {
            return valeurNColumnIndex;
        }

        public int getValeurN1ColumnIndex() {
            return valeurN1ColumnIndex;
        }

        public Integer getCategoryColumnIndex() {
            return categoryColumnIndex;
        }

        public boolean isPositionalFallback() {
            return positionalFallback;
        }
    }

    private enum ColumnRole {
        KPI,
        VALUE_N,
        VALUE_N1,
        CATEGORY,
        UNKNOWN
    }
}
