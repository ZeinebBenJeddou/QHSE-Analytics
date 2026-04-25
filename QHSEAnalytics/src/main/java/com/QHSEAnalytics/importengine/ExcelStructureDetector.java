package com.QHSEAnalytics.importengine;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ExcelStructureDetector {

    private static final Set<String> KPI_HEADERS = Set.of("kpi", "indicateur", "libelle", "nom", "intitule", "mesure");
    private static final Set<String> N1_HEADERS = Set.of("n-1", "periode n-1", "valeur n-1", "n1", "periode n1");
    private static final Set<String> N_HEADERS = Set.of("n", "periode n", "valeur n", "n0");

    private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");

    public ExcelStructure detectStructure(Sheet sheet) {
        if (sheet == null) {
            throw new IllegalArgumentException("La feuille Excel ne peut pas être nulle.");
        }

        for (int rowIndex = 0; rowIndex <= Math.min(10, sheet.getLastRowNum()); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, Integer> headerPositions = findHeaderColumns(row);
            if (headerPositions.containsKey("kpi") && headerPositions.containsKey("n-1") && headerPositions.containsKey("n")) {
                return new ExcelStructure(false, rowIndex,
                        headerPositions.get("kpi"),
                        headerPositions.get("n-1"),
                        headerPositions.get("n"),
                        -1, -1);
            }
        }

        // Transposed candidate detection: first column contains N-1 and N labels.
        Integer n1Row = null;
        Integer nRow = null;
        int maxRow = Math.min(10, sheet.getLastRowNum());
        for (int rowIndex = 0; rowIndex <= maxRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String cellText = normalizeCellValue(row.getCell(0));
            if (isN1Header(cellText)) {
                n1Row = rowIndex;
            }
            if (isNHeader(cellText)) {
                nRow = rowIndex;
            }
        }

        if (n1Row != null && nRow != null && n1Row < nRow) {
            return new ExcelStructure(true, 0, 0, -1, -1, n1Row, nRow);
        }

        // Fallback row orientation with typical positions.
        return new ExcelStructure(false, 0, 0, 1, 2, -1, -1);
    }

    public List<RawKpiRow> extractRows(Sheet sheet, ExcelStructure structure) {
        List<RawKpiRow> rows = new ArrayList<>();
        if (structure.transposed()) {
            Row headerRow = sheet.getRow(structure.headerRow());
            if (headerRow == null) {
                return rows;
            }

            int lastColumn = headerRow.getLastCellNum();
            for (int colIndex = 1; colIndex < lastColumn; colIndex++) {
                String label = normalizeCellValue(headerRow.getCell(colIndex));
                String rawN1 = normalizeCellValue(sheet.getRow(structure.valueN1Row()).getCell(colIndex));
                String rawN = normalizeCellValue(sheet.getRow(structure.valueNRow()).getCell(colIndex));
                if (label == null || label.isBlank()) {
                    continue;
                }
                rows.add(new RawKpiRow(structure.valueN1Row(), label, rawN1, rawN));
            }
            return rows;
        }

        int startRow = structure.headerRow() + 1;
        int lastRow = sheet.getLastRowNum();
        for (int rowIndex = startRow; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String label = normalizeCellValue(row.getCell(structure.kpiColumn()));
            String rawN1 = normalizeCellValue(row.getCell(structure.valueN1Column()));
            String rawN = normalizeCellValue(row.getCell(structure.valueNColumn()));
            if ((label == null || label.isBlank()) && (rawN1 == null || rawN1.isBlank()) && (rawN == null || rawN.isBlank())) {
                continue;
            }
            rows.add(new RawKpiRow(rowIndex, label, rawN1, rawN));
        }
        return rows;
    }

    private Map<String, Integer> findHeaderColumns(Row row) {
        Map<String, Integer> positions = new HashMap<>();
        int maxCell = Math.min(10, row.getLastCellNum());
        for (int cellIndex = 0; cellIndex < maxCell; cellIndex++) {
            String value = normalizeCellValue(row.getCell(cellIndex));
            if (value == null) {
                continue;
            }
            if (isKpiHeader(value)) {
                positions.put("kpi", cellIndex);
            }
            if (isN1Header(value)) {
                positions.put("n-1", cellIndex);
            }
            if (isNHeader(value)) {
                positions.put("n", cellIndex);
            }
        }
        return positions;
    }

    private String normalizeCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        DataFormatter formatter = new DataFormatter();
        String value = formatter.formatCellValue(cell);
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        normalized = NON_ASCII.matcher(normalized).replaceAll("");
        normalized = normalized.replace("\u00A0", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isKpiHeader(String text) {
        return KPI_HEADERS.stream().anyMatch(text::contains);
    }

    private boolean isN1Header(String text) {
        return N1_HEADERS.stream().anyMatch(text::contains);
    }

    private boolean isNHeader(String text) {
        return N_HEADERS.stream().anyMatch(text::contains);
    }

    public record ExcelStructure(
            boolean transposed,
            int headerRow,
            int kpiColumn,
            int valueN1Column,
            int valueNColumn,
            int valueN1Row,
            int valueNRow
    ) {
    }

    public record RawKpiRow(int rowIndex, String label, String valeurBruteN1, String valeurBruteN) {
    }
}
