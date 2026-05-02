package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.exception.ImportValidationException;
import com.QHSEAnalytics.exception.InvalidFileFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractionAgent {

    public static final String EXTRACTION_METHOD_CUSTOM = "CUSTOM";
    public static final String EXTRACTION_METHOD_TEMPLATE = "TEMPLATE";
    public static final String EXTRACTION_METHOD_AUTO = "AUTO";

    public static final String MAPPING_KPI_NAME = "kpiName";
    public static final String MAPPING_CATEGORY = "category";
    public static final String MAPPING_UNIT = "unit";
    public static final String MAPPING_VALUE_N = "valueN";
    public static final String MAPPING_VALUE_N_MINUS_1 = "valueNMinus1";
    public static final String MAPPING_KPI_NAME_INDEX = "kpiNameIndex";
    public static final String MAPPING_CATEGORY_INDEX = "categoryIndex";
    public static final String MAPPING_UNIT_INDEX = "unitIndex";
    public static final String MAPPING_VALUE_N_INDEX = "valueNIndex";
    public static final String MAPPING_VALUE_N1_INDEX = "valueN1Index";

    public ExtractionResult extract(MultipartFile file, Map<String, Integer> mapping) {
        validateFile(file);
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = selectDataSheet(workbook, formatter, evaluator)
                    .orElseThrow(() -> new ImportValidationException("Aucune feuille Excel disponible pour l'extraction."));

            boolean isTemplate = HeaderDetectionUtil.hasTemplateMarker(workbook, formatter, evaluator);
            ExtractionMethod method = determineExtractionMethod(mapping, isTemplate);
            Map<String, Integer> normalizedMapping = normalizeMapping(mapping);
            int headerRowIndex = determineHeaderRow(sheet, normalizedMapping, evaluator, formatter, method);
            Map<String, Integer> effectiveMapping = buildEffectiveMapping(sheet, normalizedMapping, headerRowIndex, evaluator, formatter, method);

            log.info("ExtractionAgent démarré : méthode={}, feuille={}, headerRow={}, mapping={}",
                    method, sheet.getSheetName(), headerRowIndex, effectiveMapping);

            List<ExtractionRow> extractedRows = extractRows(sheet, effectiveMapping, headerRowIndex, evaluator, formatter, method);
            List<KpiRawDataDTO> rows = extractedRows.stream()
                    .map(ExtractionRow::getDto)
                    .toList();
            List<String> detectedHeaders = buildDetectedHeaders(sheet, headerRowIndex, effectiveMapping, evaluator, formatter);

            log.info("ExtractionAgent terminé : totalRows={}, validRows={}",
                    rows.size(), rows.stream().filter(KpiRawDataDTO::isValid).count());

            return new ExtractionResult(rows, method.name(), detectedHeaders);
        } catch (ImportValidationException | InvalidFileFormatException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Erreur lors de l'extraction Excel : {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Impossible de traiter le fichier Excel.", ex);
        }
    }

    private Optional<Sheet> selectDataSheet(Workbook workbook, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (workbook == null) {
            return Optional.empty();
        }
        Optional<Sheet> templateSheet = HeaderDetectionUtil.findDataSheetForTemplate(workbook, formatter, evaluator);
        if (templateSheet.isPresent()) {
            return templateSheet;
        }
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (sheet != null && !ExcelParserUtil.isRowBlank(sheet.getRow(sheet.getFirstRowNum()), formatter, evaluator)) {
                return Optional.of(sheet);
            }
        }
        return Optional.ofNullable(workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null);
    }

    private ExtractionMethod determineExtractionMethod(Map<String, Integer> mapping, boolean isTemplate) {
        Map<String, Integer> normalized = normalizeMapping(mapping);
        if (hasRequiredIndexes(normalized)) {
            return ExtractionMethod.CUSTOM;
        }
        return isTemplate ? ExtractionMethod.TEMPLATE : ExtractionMethod.AUTO;
    }

    private boolean hasRequiredIndexes(Map<String, Integer> mapping) {
        return mapping.containsKey(MAPPING_KPI_NAME_INDEX)
                && mapping.containsKey(MAPPING_VALUE_N_INDEX)
                && mapping.containsKey(MAPPING_VALUE_N1_INDEX)
                && mapping.get(MAPPING_KPI_NAME_INDEX) != null
                && mapping.get(MAPPING_VALUE_N_INDEX) != null
                && mapping.get(MAPPING_VALUE_N1_INDEX) != null
                && mapping.get(MAPPING_KPI_NAME_INDEX) >= 0
                && mapping.get(MAPPING_VALUE_N_INDEX) >= 0
                && mapping.get(MAPPING_VALUE_N1_INDEX) >= 0;
    }

    private Map<String, Integer> normalizeMapping(Map<String, Integer> mapping) {
        Map<String, Integer> normalized = new HashMap<>();
        if (mapping == null) {
            return normalized;
        }
        normalized.put(MAPPING_KPI_NAME_INDEX, getIndex(mapping, MAPPING_KPI_NAME_INDEX, MAPPING_KPI_NAME));
        normalized.put(MAPPING_CATEGORY_INDEX, getIndex(mapping, MAPPING_CATEGORY_INDEX, MAPPING_CATEGORY));
        normalized.put(MAPPING_UNIT_INDEX, getIndex(mapping, MAPPING_UNIT_INDEX, MAPPING_UNIT));
        normalized.put(MAPPING_VALUE_N_INDEX, getIndex(mapping, MAPPING_VALUE_N_INDEX, MAPPING_VALUE_N));
        normalized.put(MAPPING_VALUE_N1_INDEX, getIndex(mapping, MAPPING_VALUE_N1_INDEX, MAPPING_VALUE_N_MINUS_1));
        return normalized;
    }

    private Integer getIndex(Map<String, Integer> mapping, String primaryKey, String fallbackKey) {
        if (mapping.containsKey(primaryKey)) {
            return mapping.get(primaryKey);
        }
        return mapping.get(fallbackKey);
    }

    private int determineHeaderRow(
            Sheet sheet,
            Map<String, Integer> mapping,
            FormulaEvaluator evaluator,
            DataFormatter formatter,
            ExtractionMethod method
    ) {
        if (method == ExtractionMethod.CUSTOM) {
            int headerRow = findHeaderRow(sheet, mapping, evaluator, formatter);
            log.info("ExtractionAgent CUSTOM headerRow détecté = {}", headerRow);
            return headerRow;
        }

        Optional<HeaderDetectionUtil.HeaderDetectionResult> headerResult = HeaderDetectionUtil.detectHeaderRow(sheet, formatter, evaluator);
        if (headerResult.isPresent()) {
            int headerRow = headerResult.get().getHeaderRowIndex();
            log.info("ExtractionAgent header détecté = {} via détecteur avancé", headerRow);
            return headerRow;
        }

        Optional<HeaderDetectionUtil.HeaderDetectionResult> fallbackHeader = HeaderDetectionUtil.buildFallbackHeader(sheet, formatter, evaluator);
        if (fallbackHeader.isPresent()) {
            int fallbackRow = fallbackHeader.get().getHeaderRowIndex();
            log.info("ExtractionAgent header fallback = {}", fallbackRow);
            return fallbackRow;
        }

        int headerRow = findHeaderRow(sheet, mapping, evaluator, formatter);
        log.warn("ExtractionAgent ne trouve pas d'en-tête reconnu, utilisation de la première ligne valide = {}", headerRow);
        return headerRow;
    }

    private Map<String, Integer> buildEffectiveMapping(
            Sheet sheet,
            Map<String, Integer> mapping,
            int headerRowIndex,
            FormulaEvaluator evaluator,
            DataFormatter formatter,
            ExtractionMethod method
    ) {
        Row headerRow = sheet.getRow(headerRowIndex);
        Map<String, Integer> effective = new HashMap<>(mapping);

        boolean useCustom = hasRequiredIndexes(mapping);
        if (!useCustom) {
            Optional<HeaderDetectionUtil.HeaderDetectionResult> headerResult = HeaderDetectionUtil.detectHeaderRow(sheet, formatter, evaluator);
            if (headerResult.isEmpty()) {
                headerResult = HeaderDetectionUtil.buildFallbackHeader(sheet, formatter, evaluator);
            }
            if (headerResult.isPresent()) {
                HeaderDetectionUtil.HeaderDetectionResult result = headerResult.get();
                effective.put(MAPPING_KPI_NAME_INDEX, result.getKpiColumnIndex());
                effective.put(MAPPING_VALUE_N_INDEX, result.getValeurNColumnIndex());
                effective.put(MAPPING_VALUE_N1_INDEX, result.getValeurN1ColumnIndex());
                if (result.getCategoryColumnIndex() != null) {
                    effective.put(MAPPING_CATEGORY_INDEX, result.getCategoryColumnIndex());
                }
            }
        }

        int kpiIndex = optionalIndex(effective, MAPPING_KPI_NAME_INDEX, 1);
        int valueNIndex = optionalIndex(effective, MAPPING_VALUE_N_INDEX, 4);
        int valueN1Index = optionalIndex(effective, MAPPING_VALUE_N1_INDEX, 3);
        int categoryIndex = optionalIndex(effective, MAPPING_CATEGORY_INDEX, -1);
        int unitIndex = optionalIndex(effective, MAPPING_UNIT_INDEX, -1);

        effective.put(MAPPING_KPI_NAME_INDEX, kpiIndex);
        effective.put(MAPPING_VALUE_N_INDEX, valueNIndex);
        effective.put(MAPPING_VALUE_N1_INDEX, valueN1Index);
        effective.put(MAPPING_CATEGORY_INDEX, categoryIndex);
        effective.put(MAPPING_UNIT_INDEX, unitIndex);

        return effective;
    }

    private int optionalIndex(Map<String, Integer> mapping, String key, int fallback) {
        return mapping.getOrDefault(key, fallback) == null ? fallback : mapping.get(key);
    }

    private int findHeaderRow(Sheet sheet, Map<String, Integer> mapping, FormulaEvaluator evaluator, DataFormatter formatter) {
        int firstRow = sheet.getFirstRowNum();
        int lastRow = Math.min(firstRow + 10, sheet.getLastRowNum());
        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            int nonEmpty = 0;
            for (Cell cell : row) {
                String value = ExcelParserUtil.getCellValue(cell, evaluator, formatter);
                if (value != null && !value.isBlank()) {
                    nonEmpty++;
                }
            }
            if (nonEmpty >= 3) {
                return rowIndex;
            }
        }
        return sheet.getFirstRowNum();
    }

    private List<ExtractionRow> extractRows(
            Sheet sheet,
            Map<String, Integer> mapping,
            int headerRowIndex,
            FormulaEvaluator evaluator,
            DataFormatter formatter,
            ExtractionMethod method
    ) {
        List<ExtractionRow> rows = new ArrayList<>();
        int startRow = headerRowIndex + 1;
        int lastRow = sheet.getLastRowNum();

        for (int rowIndex = startRow; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String kpiName = ExcelParserUtil.getCellValue(row.getCell(mapping.get(MAPPING_KPI_NAME_INDEX)), evaluator, formatter);
            String categorie = mapping.get(MAPPING_CATEGORY_INDEX) >= 0 ? ExcelParserUtil.getCellValue(row.getCell(mapping.get(MAPPING_CATEGORY_INDEX)), evaluator, formatter) : null;
            String unite = mapping.get(MAPPING_UNIT_INDEX) >= 0 ? ExcelParserUtil.getCellValue(row.getCell(mapping.get(MAPPING_UNIT_INDEX)), evaluator, formatter) : null;
            String valeurN1Raw = ExcelParserUtil.getCellValue(row.getCell(mapping.get(MAPPING_VALUE_N1_INDEX)), evaluator, formatter);
            String valeurNRaw = ExcelParserUtil.getCellValue(row.getCell(mapping.get(MAPPING_VALUE_N_INDEX)), evaluator, formatter);
            Double valeurN1 = parseNumber(valeurN1Raw);
            Double valeurN = parseNumber(valeurNRaw);

            if (isRowCompletelyEmpty(kpiName, categorie, unite, valeurN1Raw, valeurNRaw)) {
                continue;
            }

            boolean valid = kpiName != null && !kpiName.isBlank() && valeurN1 != null && valeurN != null;
            String validationMessage = null;
            if (!valid) {
                if (kpiName == null || kpiName.isBlank()) {
                    validationMessage = "Nom du KPI manquant.";
                } else if (valeurN1 == null) {
                    validationMessage = "Valeur N-1 manquante ou non numérique.";
                } else {
                    validationMessage = "Valeur N manquante ou non numérique.";
                }
                log.warn("ExtractionAgent ligne {} invalide : {}", rowIndex + 1, validationMessage);
            }

            String cleanedKpiName = safeTrim(kpiName);
            double confidence = KpiMatchingUtil.computeConfidence(cleanedKpiName);
            KpiRawDataDTO dto = KpiRawDataDTO.builder()
                    .rowIndex(rowIndex + 1)
                    .kpiName(cleanedKpiName)
                    .categorie(safeTrim(categorie))
                    .unite(safeTrim(unite))
                    .valeurN1(valeurN1)
                    .valeurN(valeurN)
                    .valeurN1Raw(safeTrim(valeurN1Raw))
                    .valeurNRaw(safeTrim(valeurNRaw))
                    .valid(valid)
                    .validationMessage(validationMessage)
                    .methodeExtraction(method.name())
                    .scoreConfiance(confidence)
                    .build();

            rows.add(new ExtractionRow(dto, safeTrim(valeurN1Raw), safeTrim(valeurNRaw)));
        }
        return rows;
    }

    private boolean isRowCompletelyEmpty(String kpiName, String categorie, String unite, String valeurN1Raw, String valeurNRaw) {
        return (kpiName == null || kpiName.isBlank())
                && (categorie == null || categorie.isBlank())
                && (unite == null || unite.isBlank())
                && (valeurN1Raw == null || valeurN1Raw.isBlank())
                && (valeurNRaw == null || valeurNRaw.isBlank());
    }


    private Double parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        
        String lower = value.trim().toLowerCase(Locale.ROOT);
        if (lower.equals("oui") || lower.equals("true") || lower.equals("vrai") || lower.equals("yes") || lower.equals("acquis")) {
            return 1.0;
        }
        if (lower.equals("non") || lower.equals("false") || lower.equals("faux") || lower.equals("no") || lower.equals("perdu")) {
            return 0.0;
        }

        String cleaned = value
                .replace("\u00A0", " ")
                .replace(" ", "")
                .replace(',', '.')
                .replaceAll("[^0-9.\\-]", "");
        if (cleaned.isBlank() || cleaned.equals("-") || cleaned.equals(".")) {
            return null;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            log.debug("Impossible de parser la valeur numérique '{}': {}", value, ex.getMessage());
            return null;
        }
    }

    private List<String> buildDetectedHeaders(Sheet sheet, int headerRowIndex, Map<String, Integer> mapping, FormulaEvaluator evaluator, DataFormatter formatter) {
        List<String> headers = new ArrayList<>();
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            return headers;
        }

        int maxCol = Math.max(5, headerRow.getLastCellNum());
        for (int columnIndex = 0; columnIndex < maxCol; columnIndex++) {
            String headerValue = ExcelParserUtil.getCellValue(headerRow.getCell(columnIndex), evaluator, formatter);
            if (headerValue != null && !headerValue.isBlank()) {
                headers.add(headerValue.trim());
            } else if (mapping.containsValue(columnIndex)) {
                headers.add("Colonne " + columnIndex);
            } else {
                headers.add("Colonne " + columnIndex);
            }
        }
        return headers;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileFormatException("Le fichier Excel est requis.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.toLowerCase(Locale.ROOT).endsWith(".xlsx") || filename.toLowerCase(Locale.ROOT).endsWith(".xls"))) {
            throw new InvalidFileFormatException("Seuls les fichiers Excel (.xlsx, .xls) sont autorisés.");
        }
    }

    private static final class ExtractionRow {
        private final KpiRawDataDTO dto;
        private final String valeurN1Raw;
        private final String valeurNRaw;

        public ExtractionRow(KpiRawDataDTO dto, String valeurN1Raw, String valeurNRaw) {
            this.dto = dto;
            this.valeurN1Raw = valeurN1Raw;
            this.valeurNRaw = valeurNRaw;
        }

        public KpiRawDataDTO getDto() {
            return dto;
        }

        public String getValeurN1Raw() {
            return valeurN1Raw;
        }

        public String getValeurNRaw() {
            return valeurNRaw;
        }
    }

    public static final class ExtractionResult {
        private final List<KpiRawDataDTO> rows;
        private final String extractionMethod;
        private final List<String> detectedHeaders;

        public ExtractionResult(List<KpiRawDataDTO> rows, String extractionMethod, List<String> detectedHeaders) {
            this.rows = rows;
            this.extractionMethod = extractionMethod;
            this.detectedHeaders = detectedHeaders;
        }

        public List<KpiRawDataDTO> getRows() {
            return rows;
        }

        public String getExtractionMethod() {
            return extractionMethod;
        }

        public List<String> getDetectedHeaders() {
            return detectedHeaders;
        }
    }

    private enum ExtractionMethod {
        CUSTOM,
        TEMPLATE,
        AUTO
    }
}

