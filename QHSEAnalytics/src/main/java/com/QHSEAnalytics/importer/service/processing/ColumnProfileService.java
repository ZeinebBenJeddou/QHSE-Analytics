package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.response.ColumnProfileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ColumnProfileService {

    private final ExcelFileValidator excelFileValidator;

    private static final int MAX_SAMPLE = 5;


    private static final Set<String> KPI_NAME_SYNONYMS = Set.of(
            "kpi", "indicateur", "indicateur qhse", "libelle",
            "nom indicateur", "metric", "nom kpi", "designation");
    private static final Set<String> VALUE_N_SYNONYMS = Set.of(
            "n", "annee n", "valeur n", "resultat n", "current", "annee actuelle",
            "valeur actuelle", "n courant");
    private static final Set<String> VALUE_N1_SYNONYMS = Set.of(
            "n-1", "annee n-1", "valeur n-1", "resultat n-1", "previous",
            "annee precedente", "valeur precedente", "n1", "n moins 1");
    private static final Set<String> CATEGORY_SYNONYMS = Set.of(
            "categorie", "domaine", "famille", "axe", "qhse", "type");
    private static final Set<String> UNIT_SYNONYMS = Set.of(
            "unite", "unit", "mesure", "unites");

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\d{1,4}[./-]\\d{1,2}[./-]\\d{1,4}");
    private static final Set<String> BOOLEAN_VALUES = Set.of(
            "oui", "non", "true", "false", "vrai", "faux", "yes", "no", "acquis", "perdu", "1", "0");

    public List<ColumnProfileDTO> profile(MultipartFile file) {
        excelFileValidator.validate(file);
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = selectSheet(workbook);
            if (sheet == null) return List.of();

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();


            int headerRowIndex = HeaderDetectionUtil.detectHeaderRow(sheet, formatter, evaluator)
                    .map(HeaderDetectionUtil.HeaderDetectionResult::getHeaderRowIndex)
                    .orElseGet(() -> {
                        for (int i = 0; i <= Math.min(50, sheet.getLastRowNum()); i++) {
                            Row r = sheet.getRow(i);
                            if (r != null && ExcelParserUtil.countNonBlankCells(r, formatter, evaluator) >= 2) return i;
                        }
                        return 0;
                    });

            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) return List.of();

            int colCount = headerRow.getLastCellNum();
            List<ColumnProfileDTO> profiles = new ArrayList<>();

            for (int col = 0; col < colCount; col++) {
                String header = cellStr(headerRow.getCell(col), formatter, evaluator);
                if (header == null || header.isBlank()) continue;


                List<String> allValues = new ArrayList<>();
                for (int row = headerRowIndex + 1; row <= sheet.getLastRowNum(); row++) {
                    Row dataRow = sheet.getRow(row);
                    String val = cellStr(dataRow == null ? null : dataRow.getCell(col), formatter, evaluator);
                    allValues.add(val);
                }

                int totalRows = allValues.size();
                long nullCount = allValues.stream().filter(v -> v == null || v.isBlank()).count();
                List<String> nonNull = allValues.stream()
                        .filter(v -> v != null && !v.isBlank())
                        .toList();
                long uniqueCount = nonNull.stream().map(String::trim).distinct().count();


                String inferredType = inferType(nonNull);


                Double numMin = null, numMax = null, numMean = null;
                if ("NUMERIC".equals(inferredType)) {
                    List<Double> nums = nonNull.stream()
                            .map(this::parseDouble)
                            .filter(Objects::nonNull)
                            .toList();
                    if (!nums.isEmpty()) {
                        numMin = nums.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                        numMax = nums.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                        numMean = nums.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                        numMean = Math.round(numMean * 1000.0) / 1000.0;
                    }
                }


                List<String> samples = nonNull.stream()
                        .limit(MAX_SAMPLE)
                        .toList();


                String normalizedHeader = normalize(header);
                String[] semanticAndConf = inferSemantic(normalizedHeader);

                profiles.add(ColumnProfileDTO.builder()
                        .columnIndex(col)
                        .detectedHeader(header.trim())
                        .inferredType(inferredType)
                        .sampleValues(samples)
                        .totalRows(totalRows)
                        .nullCount((int) nullCount)
                        .uniqueCount((int) uniqueCount)
                        .numericMin(numMin)
                        .numericMax(numMax)
                        .numericMean(numMean)
                        .likelySemantic(semanticAndConf[0])
                        .semanticConfidence(Double.parseDouble(semanticAndConf[1]))
                        .build());
            }

            return profiles;

        } catch (Exception ex) {
            log.warn("Column profiling failed for file {}: {}", file.getOriginalFilename(), ex.getMessage());
            return List.of();
        }
    }



    private Sheet selectSheet(Workbook wb) {
        if (wb.getNumberOfSheets() == 0) return null;

        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            String name = normalize(wb.getSheetName(i));
            if (name.contains("donn") || name.contains("data") || name.contains("kpi") || name.contains("qhse")) {
                return wb.getSheetAt(i);
            }
        }
        return wb.getSheetAt(0);
    }

    private String cellStr(Cell cell, DataFormatter fmt, FormulaEvaluator eval) {
        return ExcelParserUtil.getCellValue(cell, eval, fmt);
    }

    private String inferType(List<String> values) {
        if (values.isEmpty()) return "TEXT";
        int numeric = 0, bool = 0, date = 0;
        for (String v : values) {
            String trimmed = v.trim().toLowerCase(Locale.ROOT);
            if (BOOLEAN_VALUES.contains(trimmed)) { bool++; continue; }
            if (DATE_PATTERN.matcher(trimmed).matches()) { date++; continue; }
            if (parseDouble(v) != null) { numeric++; }
        }
        int total = values.size();
        double numRatio  = numeric / (double) total;
        double boolRatio = bool   / (double) total;
        double dateRatio = date   / (double) total;

        if (numRatio >= 0.8) return "NUMERIC";
        if (boolRatio >= 0.8) return "BOOLEAN";
        if (dateRatio >= 0.8) return "DATE";
        if (numRatio > 0.1 || boolRatio > 0.1 || dateRatio > 0.1) return "MIXED";
        return "TEXT";
    }

    private String[] inferSemantic(String normalizedHeader) {
        if (KPI_NAME_SYNONYMS.contains(normalizedHeader)
                || normalizedHeader.contains("kpi") || normalizedHeader.contains("indicateur")) {
            return new String[]{"KPI_NAME", "0.95"};
        }
        if (VALUE_N1_SYNONYMS.contains(normalizedHeader)
                || normalizedHeader.contains("n1") || normalizedHeader.contains("n-1")
                || normalizedHeader.contains("precedent") || normalizedHeader.contains("previous")) {
            return new String[]{"VALUE_N1", "0.90"};
        }
        if (VALUE_N_SYNONYMS.contains(normalizedHeader)
                || normalizedHeader.equals("n") || normalizedHeader.contains("actuel")
                || normalizedHeader.contains("current")) {
            return new String[]{"VALUE_N", "0.85"};
        }
        if (CATEGORY_SYNONYMS.contains(normalizedHeader)
                || normalizedHeader.contains("categor") || normalizedHeader.contains("domain")) {
            return new String[]{"CATEGORY", "0.88"};
        }
        if (UNIT_SYNONYMS.contains(normalizedHeader) || normalizedHeader.contains("unit")) {
            return new String[]{"UNIT", "0.85"};
        }
        return new String[]{"UNKNOWN", "0.30"};
    }

    private Double parseDouble(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Double.parseDouble(v.trim().replace(',', '.').replace(" ", "").replace(" ", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
