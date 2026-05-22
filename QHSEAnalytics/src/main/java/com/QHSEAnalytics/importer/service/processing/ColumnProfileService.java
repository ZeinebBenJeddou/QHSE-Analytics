package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.response.ColumnProfileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ColumnProfileService {

    private final ExcelFileValidator excelFileValidator;

    private static final int MAX_COLUMNS_TO_PROFILE = 50;

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
            int profiledCount = 0;
            String truncationWarning = null;

            for (int col = 0; col < colCount; col++) {
                String header = cellStr(headerRow.getCell(col), formatter, evaluator);
                if (header == null || header.isBlank()) continue;
                if (profiledCount >= MAX_COLUMNS_TO_PROFILE) {
                    if (truncationWarning == null) {
                        truncationWarning = "Le fichier contient " + colCount +
                                " colonnes — seules les " + MAX_COLUMNS_TO_PROFILE +
                                " premières ont été analysées";
                    }
                    continue;
                }


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
                String inferredType = inferType(nonNull);


                String normalizedHeader = normalize(header);
                ColumnSemanticResolver.Result semanticResult = ColumnSemanticResolver.resolve(normalizedHeader);

                profiles.add(ColumnProfileDTO.builder()
                        .columnIndex(col)
                        .detectedHeader(header.trim())
                        .inferredType(inferredType)
                        .totalRows(totalRows)
                        .nullCount((int) nullCount)
                        .likelySemantic(semanticResult.semantic().name())
                        .confidenceScore(semanticResult.confidence())
                        .build());
                profiledCount++;
            }

            resolveYearColumns(profiles);

            String duplicateWarning = buildDuplicateHeaderWarning(headerRow, formatter, evaluator);
            String combinedWarning = combineWarnings(truncationWarning, duplicateWarning);

            if (combinedWarning != null && !profiles.isEmpty()) {
                ColumnProfileDTO first = profiles.get(0);
                profiles.set(0, ColumnProfileDTO.builder()
                        .columnIndex(first.getColumnIndex())
                        .detectedHeader(first.getDetectedHeader())
                        .inferredType(first.getInferredType())
                        .totalRows(first.getTotalRows())
                        .nullCount(first.getNullCount())
                        .likelySemantic(first.getLikelySemantic())
                        .confidenceScore(first.getConfidenceScore())
                        .warningMessage(combinedWarning)
                        .build());
            }

            return profiles;

        } catch (Exception ex) {
            log.warn("Column profiling failed for file {}: {}", file.getOriginalFilename(), ex.getMessage());
            return List.of();
        }
    }




    private String buildDuplicateHeaderWarning(Row headerRow, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String raw = cellStr(cell, formatter, evaluator);
            if (raw == null || raw.isBlank()) continue;
            String key = normalize(raw);
            counts.merge(key, 1L, Long::sum);
        }
        List<String> duplicates = counts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (duplicates.isEmpty()) return null;
        return "Headers dupliqués détectés : " + String.join(", ", duplicates) +
               ". Renommez les colonnes pour un mapping précis.";
    }

    private String combineWarnings(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return a + " | " + b;
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

    private Double parseDouble(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Double.parseDouble(v.trim().replace(',', '.').replace(" ", "").replace(" ", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalize(String value) {
        return ColumnSemanticResolver.normalize(value);
    }

    private static final Pattern YEAR_PATTERN = Pattern.compile("^(19|20)\\d{2}$");

    private void resolveYearColumns(List<ColumnProfileDTO> profiles) {
        // Pass 1 : colonnes dont le header est une année (ex: 2023, 2024)
        List<ColumnProfileDTO> yearCols = profiles.stream()
                .filter(p -> "UNKNOWN".equals(p.getLikelySemantic()))
                .filter(p -> YEAR_PATTERN.matcher(p.getDetectedHeader().trim()).matches())
                .sorted(Comparator.comparingInt(p -> Integer.parseInt(p.getDetectedHeader().trim())))
                .toList();

        if (yearCols.size() >= 2) {
            ColumnProfileDTO colN  = yearCols.get(yearCols.size() - 1);
            ColumnProfileDTO colN1 = yearCols.get(yearCols.size() - 2);
            setSemanticOnProfile(profiles, colN.getColumnIndex(),  "VALUE_N");
            setSemanticOnProfile(profiles, colN1.getColumnIndex(), "VALUE_N1");
            return;
        }

        // Pass 2 : fallback — si VALUE_N et VALUE_N1 toujours UNKNOWN,
        // prendre les 2 premières colonnes NUMERIC dans l'ordre du fichier
        boolean valueNFound  = profiles.stream().anyMatch(p -> "VALUE_N".equals(p.getLikelySemantic()));
        boolean valueN1Found = profiles.stream().anyMatch(p -> "VALUE_N1".equals(p.getLikelySemantic()));

        if (!valueNFound || !valueN1Found) {
            List<ColumnProfileDTO> numericCols = profiles.stream()
                    .filter(p -> "UNKNOWN".equals(p.getLikelySemantic()))
                    .filter(p -> "NUMERIC".equals(p.getInferredType()))
                    .sorted(Comparator.comparingInt(ColumnProfileDTO::getColumnIndex))
                    .toList();

            if (numericCols.size() >= 2) {
                // premier NUMERIC → VALUE_N1, deuxième → VALUE_N (ordre naturel du fichier)
                setSemanticOnProfile(profiles, numericCols.get(0).getColumnIndex(), "VALUE_N1");
                setSemanticOnProfile(profiles, numericCols.get(1).getColumnIndex(), "VALUE_N");
            } else if (numericCols.size() == 1) {
                setSemanticOnProfile(profiles, numericCols.get(0).getColumnIndex(), "VALUE_N");
            }
        }
    }

    private void setSemanticOnProfile(List<ColumnProfileDTO> profiles, int colIndex, String semantic) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getColumnIndex() == colIndex) {
                ColumnProfileDTO existing = profiles.get(i);
                double score = existing.getConfidenceScore() >= 0.50
                        ? existing.getConfidenceScore()
                        : 0.50;
                profiles.set(i, ColumnProfileDTO.builder()
                        .columnIndex(existing.getColumnIndex())
                        .detectedHeader(existing.getDetectedHeader())
                        .inferredType(existing.getInferredType())
                        .totalRows(existing.getTotalRows())
                        .nullCount(existing.getNullCount())
                        .likelySemantic(semantic)
                        .confidenceScore(score)
                        .build());
                return;
            }
        }
    }
}
