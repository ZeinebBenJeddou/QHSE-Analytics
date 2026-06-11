package com.QHSEAnalytics.importer.service.processing;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;

import java.text.Normalizer;
import java.util.Locale;

public final class ExcelParserUtil {

    private ExcelParserUtil() {
    }

    public static String getCellValue(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }

        //parsing en cas de texte en cellule de valeur numerique
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = evaluator.evaluateFormulaCell(cell);
        }

        return switch (cellType) {
            case STRING -> formatter.formatCellValue(cell);
            case NUMERIC -> formatter.formatCellValue(cell);
            case BOOLEAN -> formatter.formatCellValue(cell);
            case BLANK, ERROR, _NONE -> null;
            default -> null;
        };
    }

    public static boolean isRowBlank(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }

        for (Cell cell : row) {
            String value = getCellValue(cell, evaluator, formatter);
            if (value != null && !value.isBlank()) {
                return false;
            }
        }

        return true;
    }

    public static int countNonBlankCells(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return 0;
        }

        int count = 0;
        for (Cell cell : row) {
            String value = getCellValue(cell, evaluator, formatter);
            if (value != null && !value.isBlank()) {
                count++;
            }
        }
        return count;
    }

    public static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^a-z0-9\\s-]", " ").replaceAll("\\s+", " ").trim();
    }
}
