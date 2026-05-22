package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import com.QHSEAnalytics.shared.exception.ImportValidationException;
import com.QHSEAnalytics.shared.exception.InvalidFileFormatException;
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
public class ExtractionAgent {

    private final ExcelFileValidator excelFileValidator;


    public static final String MAPPING_KPI_NAME        = "kpiName";
    public static final String MAPPING_CATEGORY        = "category";
    public static final String MAPPING_UNIT            = "unit";
    public static final String MAPPING_VALUE_N         = "valueN";
    public static final String MAPPING_VALUE_N_MINUS_1 = "valueNMinus1";
    public static final String MAPPING_KPI_NAME_INDEX  = "kpiNameIndex";
    public static final String MAPPING_CATEGORY_INDEX  = "categoryIndex";
    public static final String MAPPING_UNIT_INDEX      = "unitIndex";
    public static final String MAPPING_VALUE_N_INDEX   = "valueNIndex";
    public static final String MAPPING_VALUE_N1_INDEX  = "valueN1Index";

    private static final double LOW_CONFIDENCE_THRESHOLD = 0.4;




    private static final Pattern AMBIGUOUS_PATTERN = Pattern.compile(
            "^-?\\d[\\d\\s.,]*[a-zA-ZÀ-ÿ%°/]+.*$");

    private static final Pattern FRACTION_PATTERN = Pattern.compile("^\\d+/\\d+$");


    public ExtractionResult extract(MultipartFile file, Map<String, Integer> mapping) {
        excelFileValidator.validate(file);
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            if (workbook.getNumberOfSheets() == 0) {
                throw new ImportValidationException("Le fichier Excel ne contient aucune feuille.");
            }

            Sheet sheet = selectDataSheet(workbook, formatter, evaluator)
                    .orElseThrow(() -> new ImportValidationException("Aucune feuille exploitable trouvée."));

            ExtractionMethod method = determineExtractionMethod();
            Map<String, Integer> normalizedMapping = normalizeMapping(mapping);
            int headerRowIndex = determineHeaderRow(sheet, normalizedMapping, evaluator, formatter, method);
            MappingResult mappingResult = buildEffectiveMapping(
                    sheet, normalizedMapping, headerRowIndex, evaluator, formatter, method);

            if (mappingResult.hasMissingCritical) {
                throw new ImportValidationException(
                    "Colonnes non reconnues\n" +
                    "L'application n'a pas réussi à identifier les colonnes de votre fichier. " +
                    "Vérifiez que votre fichier contient au moins 3 colonnes avec des en-têtes clairs.\n" +
                    "Exemples d'en-têtes reconnus :\n" +
                    "  - Noms des KPIs : Indicateur, Libellé, KPI\n" +
                    "  - Valeur actuelle : Réalisé N, Valeur 2026, N\n" +
                    "  - Valeur précédente : Réalisé N-1, Valeur 2025, N-1");
            }

            Map<String, Integer> effectiveMapping = mappingResult.effectiveMapping;
            List<ImportIssue> extractionIssues = new ArrayList<>(mappingResult.mappingIssues);

            log.info("ExtractionAgent démarré : méthode={}, feuille={}, headerRow={}, mapping={}",
                    method, sheet.getSheetName(), headerRowIndex, effectiveMapping);

            int lastDataRow = sheet.getLastRowNum();
            if (lastDataRow <= headerRowIndex) {
                throw new ImportValidationException("Le fichier ne contient aucune ligne de données après l'en-tête.");
            }

            List<KpiRawDataDTO> rows = extractRows(sheet, effectiveMapping, headerRowIndex, evaluator, formatter, method);
            List<String> detectedHeaders = buildDetectedHeaders(sheet, headerRowIndex, effectiveMapping, evaluator, formatter);

            log.info("ExtractionAgent terminé : totalRows={}, validRows={}",
                    rows.size(), rows.stream().filter(KpiRawDataDTO::isValid).count());

            return new ExtractionResult(rows, method.name(), detectedHeaders, extractionIssues);

        } catch (ImportValidationException | InvalidFileFormatException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Erreur lors de l'extraction Excel : {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Impossible de traiter le fichier Excel.", ex);
        }
    }


    private Optional<Sheet> selectDataSheet(Workbook workbook, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (workbook == null) return Optional.empty();
        Optional<Sheet> templateSheet = HeaderDetectionUtil.findDataSheetForTemplate(workbook, formatter, evaluator);
        if (templateSheet.isPresent()) return templateSheet;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet != null && !ExcelParserUtil.isRowBlank(sheet.getRow(sheet.getFirstRowNum()), formatter, evaluator)) {
                return Optional.of(sheet);
            }
        }
        return Optional.ofNullable(workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null);
    }


    private ExtractionMethod determineExtractionMethod() {
        return ExtractionMethod.CUSTOM;
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
        if (mapping == null) return normalized;
        normalized.put(MAPPING_KPI_NAME_INDEX,  getIndex(mapping, MAPPING_KPI_NAME_INDEX,  MAPPING_KPI_NAME));
        normalized.put(MAPPING_CATEGORY_INDEX,  getIndex(mapping, MAPPING_CATEGORY_INDEX,  MAPPING_CATEGORY));
        normalized.put(MAPPING_UNIT_INDEX,      getIndex(mapping, MAPPING_UNIT_INDEX,      MAPPING_UNIT));
        normalized.put(MAPPING_VALUE_N_INDEX,   getIndex(mapping, MAPPING_VALUE_N_INDEX,   MAPPING_VALUE_N));
        normalized.put(MAPPING_VALUE_N1_INDEX,  getIndex(mapping, MAPPING_VALUE_N1_INDEX,  MAPPING_VALUE_N_MINUS_1));
        return normalized;
    }

    private Integer getIndex(Map<String, Integer> mapping, String primary, String fallback) {
        return mapping.containsKey(primary) ? mapping.get(primary) : mapping.get(fallback);
    }


    private int determineHeaderRow(Sheet sheet, Map<String, Integer> mapping,
                                   FormulaEvaluator evaluator, DataFormatter formatter, ExtractionMethod method) {
        if (method == ExtractionMethod.CUSTOM) {
            int row = findHeaderRow(sheet, mapping, evaluator, formatter);
            log.info("ExtractionAgent CUSTOM headerRow détecté = {}", row);
            return row;
        }
        Optional<HeaderDetectionUtil.HeaderDetectionResult> result =
                HeaderDetectionUtil.detectHeaderRow(sheet, formatter, evaluator);
        if (result.isPresent()) {
            log.info("ExtractionAgent header détecté = {} via détecteur avancé", result.get().getHeaderRowIndex());
            return result.get().getHeaderRowIndex();
        }
        Optional<HeaderDetectionUtil.HeaderDetectionResult> fallback =
                HeaderDetectionUtil.buildFallbackHeader(sheet, formatter, evaluator);
        if (fallback.isPresent()) {
            log.info("ExtractionAgent header fallback = {}", fallback.get().getHeaderRowIndex());
            return fallback.get().getHeaderRowIndex();
        }
        int row = findHeaderRow(sheet, mapping, evaluator, formatter);
        log.warn("ExtractionAgent header non reconnu, utilisation première ligne valide = {}", row);
        return row;
    }

    private static class MappingResult {
        Map<String, Integer> effectiveMapping;
        List<ImportIssue> mappingIssues;
        boolean hasMissingCritical;
        public MappingResult(Map<String, Integer> map, List<ImportIssue> issues, boolean missing) {
            this.effectiveMapping = map;
            this.mappingIssues = issues;
            this.hasMissingCritical = missing;
        }
    }

    private MappingResult buildEffectiveMapping(Sheet sheet, Map<String, Integer> mapping,
                                                int headerRowIndex, FormulaEvaluator evaluator,
                                                DataFormatter formatter, ExtractionMethod method) {
        Map<String, Integer> effective = new HashMap<>(mapping);
        List<ImportIssue> issues = new ArrayList<>();

        Row headerRow = sheet.getRow(headerRowIndex);
        Map<Integer, String> normalizedHeaders = new HashMap<>();
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                String val = ExcelParserUtil.getCellValue(cell, evaluator, formatter);
                if (val != null && !val.isBlank()) {
                    normalizedHeaders.put(cell.getColumnIndex(), normalizeHeader(val));
                }
            }
        }

        if (!hasRequiredIndexes(mapping)) {
            Optional<HeaderDetectionUtil.HeaderDetectionResult> result =
                    HeaderDetectionUtil.detectHeaderRow(sheet, formatter, evaluator);
            if (result.isEmpty()) result = HeaderDetectionUtil.buildFallbackHeader(sheet, formatter, evaluator);

            if (result.isPresent()) {
                HeaderDetectionUtil.HeaderDetectionResult r = result.get();
                if (effective.get(MAPPING_KPI_NAME_INDEX) == null || effective.get(MAPPING_KPI_NAME_INDEX) < 0) effective.put(MAPPING_KPI_NAME_INDEX,  r.getKpiColumnIndex());
                if (effective.get(MAPPING_VALUE_N_INDEX) == null || effective.get(MAPPING_VALUE_N_INDEX) < 0) effective.put(MAPPING_VALUE_N_INDEX,   r.getValeurNColumnIndex());
                if (effective.get(MAPPING_VALUE_N1_INDEX) == null || effective.get(MAPPING_VALUE_N1_INDEX) < 0) effective.put(MAPPING_VALUE_N1_INDEX,  r.getValeurN1ColumnIndex());
                if (r.getCategoryColumnIndex() != null && (effective.get(MAPPING_CATEGORY_INDEX) == null || effective.get(MAPPING_CATEGORY_INDEX) < 0))
                    effective.put(MAPPING_CATEGORY_INDEX, r.getCategoryColumnIndex());
                if (r.isPositionalFallback()) {
                    issues.add(ImportIssue.builder()
                            .rowIndex(headerRowIndex + 1)
                            .column("Valeur N / Valeur N-1")
                            .code(ImportIssue.CODE_LOW_CONFIDENCE)
                            .severity(ImportIssue.Severity.WARNING)
                            .message("Ordre des colonnes incertain — vérifiez le mapping manuellement")
                            .build());
                }
            }

            findColumnBySynonyms(effective, MAPPING_KPI_NAME_INDEX,  ColumnSemanticResolver.synonymsFor(ColumnSemanticResolver.Semantic.KPI_NAME),  normalizedHeaders, issues, headerRowIndex);
            findColumnBySynonyms(effective, MAPPING_VALUE_N_INDEX,   ColumnSemanticResolver.synonymsFor(ColumnSemanticResolver.Semantic.VALUE_N),   normalizedHeaders, issues, headerRowIndex);
            findColumnBySynonyms(effective, MAPPING_VALUE_N1_INDEX,  ColumnSemanticResolver.synonymsFor(ColumnSemanticResolver.Semantic.VALUE_N1),  normalizedHeaders, issues, headerRowIndex);
            findColumnBySynonyms(effective, MAPPING_CATEGORY_INDEX,  ColumnSemanticResolver.synonymsFor(ColumnSemanticResolver.Semantic.CATEGORY),  normalizedHeaders, issues, headerRowIndex);
            findColumnBySynonyms(effective, MAPPING_UNIT_INDEX,      ColumnSemanticResolver.synonymsFor(ColumnSemanticResolver.Semantic.UNIT),      normalizedHeaders, issues, headerRowIndex);
        }

        if (hasRequiredIndexes(mapping) && issues.isEmpty()) {
            emitLowConfidenceIfNeeded(effective, normalizedHeaders, headerRowIndex, issues);
        }

        boolean kpiFound = effective.get(MAPPING_KPI_NAME_INDEX) != null && effective.get(MAPPING_KPI_NAME_INDEX) >= 0;
        boolean nFound = effective.get(MAPPING_VALUE_N_INDEX) != null && effective.get(MAPPING_VALUE_N_INDEX) >= 0;
        boolean n1Found = effective.get(MAPPING_VALUE_N1_INDEX) != null && effective.get(MAPPING_VALUE_N1_INDEX) >= 0;

        boolean hasMissingCritical = (!kpiFound || !nFound || !n1Found);

        if (hasMissingCritical) {
            int maxCol = headerRow != null ? headerRow.getLastCellNum() : (sheet.getRow(sheet.getFirstRowNum()) != null ? sheet.getRow(sheet.getFirstRowNum()).getLastCellNum() : 0);
            if (maxCol >= 5) {
                if (!kpiFound) {
                    effective.put(MAPPING_KPI_NAME_INDEX, 1);
                    issues.add(ImportIssue.builder().rowIndex(headerRowIndex + 1).column("KPI").code(ImportIssue.CODE_LOW_CONFIDENCE).severity(ImportIssue.Severity.WARNING).message("Mapping KPI par défaut (faible confiance)").build());
                }
                if (!nFound) {
                    effective.put(MAPPING_VALUE_N_INDEX, 4);
                    issues.add(ImportIssue.builder().rowIndex(headerRowIndex + 1).column("Valeur N").code(ImportIssue.CODE_LOW_CONFIDENCE).severity(ImportIssue.Severity.WARNING).message("Mapping Valeur N par défaut (faible confiance)").build());
                }
                if (!n1Found) {
                    effective.put(MAPPING_VALUE_N1_INDEX, 3);
                    issues.add(ImportIssue.builder().rowIndex(headerRowIndex + 1).column("Valeur N-1").code(ImportIssue.CODE_LOW_CONFIDENCE).severity(ImportIssue.Severity.WARNING).message("Mapping Valeur N-1 par défaut (faible confiance)").build());
                }
                hasMissingCritical = false;
            }
        }

        effective.put(MAPPING_CATEGORY_INDEX,  optionalIndex(effective, MAPPING_CATEGORY_INDEX, -1));
        effective.put(MAPPING_UNIT_INDEX,      optionalIndex(effective, MAPPING_UNIT_INDEX,     -1));

        return new MappingResult(effective, issues, hasMissingCritical);
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        String s = Normalizer.normalize(header, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}", "");
        s = s.toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9]", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private void findColumnBySynonyms(Map<String, Integer> effective, String mappingKey, Set<String> synonyms, Map<Integer, String> headers, List<ImportIssue> issues, int headerRowIndex) {
        if (effective.get(mappingKey) != null && effective.get(mappingKey) >= 0) return;

        int bestIndex = -1;
        double bestScore = -1.0;

        for (Map.Entry<Integer, String> entry : headers.entrySet()) {
            int col = entry.getKey();
            String header = entry.getValue();
            if (synonyms.contains(header)) {
                bestIndex = col;
                bestScore = 1.0;
                break;
            } else {
                for (String syn : synonyms) {
                    if (header.contains(syn) || syn.contains(header)) {
                        if (0.5 > bestScore) {
                            bestIndex = col;
                            bestScore = 0.5;
                        }
                    }
                }
            }
        }

        if (bestIndex >= 0) {
            effective.put(mappingKey, bestIndex);
            if (bestScore < 1.0 && (mappingKey.equals(MAPPING_KPI_NAME_INDEX) || mappingKey.equals(MAPPING_VALUE_N_INDEX) || mappingKey.equals(MAPPING_VALUE_N1_INDEX))) {
                issues.add(ImportIssue.builder()
                        .rowIndex(headerRowIndex + 1)
                        .column(mappingKey)
                        .code(ImportIssue.CODE_LOW_CONFIDENCE)
                        .severity(ImportIssue.Severity.WARNING)
                        .message("Mapping de colonne détecté avec confiance moyenne pour " + mappingKey)
                        .build());
            }
        }
    }

    private void emitLowConfidenceIfNeeded(Map<String, Integer> effective,
                                            Map<Integer, String> normalizedHeaders,
                                            int headerRowIndex,
                                            List<ImportIssue> issues) {
        int[] critical = {
            effective.getOrDefault(MAPPING_KPI_NAME_INDEX, -1),
            effective.getOrDefault(MAPPING_VALUE_N_INDEX,  -1),
            effective.getOrDefault(MAPPING_VALUE_N1_INDEX, -1)
        };
        for (int colIdx : critical) {
            if (colIdx < 0) return;
            String header = normalizedHeaders.get(colIdx);
            if (header == null) return;
            if (ColumnSemanticResolver.resolve(header).confidence() >= 0.70) return;
        }
        issues.add(ImportIssue.builder()
                .rowIndex(headerRowIndex + 1)
                .column("Valeur N / Valeur N-1")
                .code(ImportIssue.CODE_LOW_CONFIDENCE)
                .severity(ImportIssue.Severity.WARNING)
                .message("Mapping incertain — en-têtes non reconnus, vérifiez la correspondance des colonnes")
                .build());
    }

    private int optionalIndex(Map<String, Integer> mapping, String key, int fallback) {
        Integer v = mapping.get(key);
        return v == null ? fallback : v;
    }

    private int findHeaderRow(Sheet sheet, Map<String, Integer> mapping,
                               FormulaEvaluator evaluator, DataFormatter formatter) {
        int first = sheet.getFirstRowNum();
        int last  = Math.min(first + 50, sheet.getLastRowNum());
        for (int i = first; i <= last; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            int nonEmpty = 0;
            for (Cell cell : row) {
                String v = ExcelParserUtil.getCellValue(cell, evaluator, formatter);
                if (v != null && !v.isBlank()) nonEmpty++;
            }
            if (nonEmpty >= 3) return i;
        }
        return sheet.getFirstRowNum();
    }


    private List<KpiRawDataDTO> extractRows(Sheet sheet, Map<String, Integer> mapping,
                                             int headerRowIndex, FormulaEvaluator evaluator,
                                             DataFormatter formatter, ExtractionMethod method) {
        List<KpiRawDataDTO> rows = new ArrayList<>();
        for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String kpiName   = ExcelParserUtil.getCellValue(row.getCell(mapping.get(MAPPING_KPI_NAME_INDEX)), evaluator, formatter);
            String categorie = mapping.get(MAPPING_CATEGORY_INDEX) >= 0
                    ? ExcelParserUtil.getCellValue(row.getCell(mapping.get(MAPPING_CATEGORY_INDEX)), evaluator, formatter) : null;
            String unite     = mapping.get(MAPPING_UNIT_INDEX) >= 0
                    ? ExcelParserUtil.getCellValue(row.getCell(mapping.get(MAPPING_UNIT_INDEX)), evaluator, formatter) : null;
            String n1Raw     = ExcelParserUtil.getCellValue(row.getCell(mapping.get(MAPPING_VALUE_N1_INDEX)), evaluator, formatter);
            String nRaw      = ExcelParserUtil.getCellValue(row.getCell(mapping.get(MAPPING_VALUE_N_INDEX)),  evaluator, formatter);

            if (isRowCompletelyEmpty(kpiName, categorie, unite, n1Raw, nRaw)) continue;

            List<ImportIssue> issues = new ArrayList<>();
            int displayRow = rowIndex + 1;


            ParseResult prN1 = parseNumberRobust(n1Raw, displayRow, "Valeur N-1", issues);
            ParseResult prN  = parseNumberRobust(nRaw,  displayRow, "Valeur N",   issues);


            boolean valid = true;
            String validationMessage = null;

            if (kpiName == null || kpiName.isBlank()) {
                valid = false;
                validationMessage = "Nom du KPI manquant.";
                issues.add(ImportIssue.builder()
                        .rowIndex(displayRow).column("KPI")
                        .code(ImportIssue.CODE_MISSING_KPI_NAME)
                        .severity(ImportIssue.Severity.ERROR)
                        .message("Nom du KPI manquant en ligne " + displayRow)
                        .build());
            } else if (prN1.value == null) {
                valid = false;
                validationMessage = "Valeur N-1 manquante ou non numérique.";
                if (!prN1.issueAdded) {
                    issues.add(ImportIssue.builder()
                            .rowIndex(displayRow).column("Valeur N-1")
                            .code(ImportIssue.CODE_MISSING_VALUE_N1)
                            .severity(ImportIssue.Severity.ERROR)
                            .message("Valeur N-1 manquante en ligne " + displayRow)
                            .originalValue(n1Raw).build());
                }
            } else if (prN.value == null) {
                valid = false;
                validationMessage = "Valeur N manquante ou non numérique.";
                if (!prN.issueAdded) {
                    issues.add(ImportIssue.builder()
                            .rowIndex(displayRow).column("Valeur N")
                            .code(ImportIssue.CODE_MISSING_VALUE_N)
                            .severity(ImportIssue.Severity.ERROR)
                            .message("Valeur N manquante en ligne " + displayRow)
                            .originalValue(nRaw).build());
                }
            }

            if (!valid) log.warn("ExtractionAgent ligne {} invalide : {}", displayRow, validationMessage);


            String cleanedKpiName = safeTrim(kpiName);
            double confidence = KpiMatchingUtil.computeConfidence(cleanedKpiName);
            if (valid && confidence < LOW_CONFIDENCE_THRESHOLD) {
                issues.add(ImportIssue.builder()
                        .rowIndex(displayRow).column("KPI")
                        .code(ImportIssue.CODE_LOW_CONFIDENCE)
                        .severity(ImportIssue.Severity.WARNING)
                        .message(String.format("Confiance faible (%.0f%%) pour le KPI '%s'", confidence * 100, cleanedKpiName))
                        .build());
            }

            String normalizedKpi = normalizeForMatching(cleanedKpiName);

            KpiRawDataDTO dto = KpiRawDataDTO.builder()
                    .rowIndex(displayRow)
                    .kpiName(cleanedKpiName)
                    .originalKpiName(safeTrim(kpiName))
                    .normalizedKpiName(normalizedKpi)
                    .categorie(safeTrim(categorie))
                    .unite(safeTrim(unite))
                    .valeurN1(prN1.value)
                    .valeurN(prN.value)
                    .valeurN1Raw(safeTrim(n1Raw))
                    .valeurNRaw(safeTrim(nRaw))
                    .valid(valid)
                    .validationMessage(validationMessage)
                    .methodeExtraction(method.name())
                    .scoreConfiance(confidence)
                    .issues(issues)
                    .build();

            rows.add(dto);
        }
        return rows;
    }


    private static class ParseResult {
        final Double  value;
        final boolean issueAdded;
        ParseResult(Double value, boolean issueAdded) { this.value = value; this.issueAdded = issueAdded; }
    }

    private ParseResult parseNumberRobust(String raw, int rowIndex, String column, List<ImportIssue> issues) {
        if (raw == null || raw.isBlank()) {
            return new ParseResult(null, false);
        }
        String trimmed = raw.trim();
        String lower   = trimmed.toLowerCase(Locale.ROOT);


        if (Set.of("oui","true","vrai","yes","acquis").contains(lower)) return new ParseResult(1.0, false);
        if (Set.of("non","false","faux","no","perdu").contains(lower))   return new ParseResult(0.0, false);


        if (FRACTION_PATTERN.matcher(trimmed).matches()) {
            issues.add(ImportIssue.builder()
                    .rowIndex(rowIndex).column(column)
                    .code(ImportIssue.CODE_AMBIGUOUS_NUMBER)
                    .severity(ImportIssue.Severity.ERROR)
                    .message("Valeur ambiguë non supportée (fraction) en ligne " + rowIndex + " : '" + raw + "'")
                    .originalValue(raw).build());
            return new ParseResult(null, true);
        }


        if (trimmed.endsWith("%")) {
            String withoutPct = trimmed.substring(0, trimmed.length() - 1).trim();
            Double parsed = tryParseDouble(withoutPct);
            if (parsed != null) {

                if (!withoutPct.equals(trimmed)) {
                    issues.add(ImportIssue.builder()
                            .rowIndex(rowIndex).column(column)
                            .code(ImportIssue.CODE_AUTO_CORRECTED)
                            .severity(ImportIssue.Severity.INFO)
                            .message("Valeur pourcentage convertie en ligne " + rowIndex)
                            .originalValue(raw).cleanedValue(parsed.toString()).build());
                }
                return new ParseResult(parsed, false);
            }
        }


        if (AMBIGUOUS_PATTERN.matcher(trimmed).matches()) {

            String numericPart = trimmed.replaceAll("[^0-9.,\\-].*$", "").trim();
            Double parsed = tryParseDouble(numericPart);
            if (parsed != null) {
                issues.add(ImportIssue.builder()
                        .rowIndex(rowIndex).column(column)
                        .code(ImportIssue.CODE_AUTO_CORRECTED)
                        .severity(ImportIssue.Severity.WARNING)
                        .message("Valeur ambiguë corrigée automatiquement en ligne " + rowIndex + " : '" + raw + "'")
                        .originalValue(raw).cleanedValue(parsed.toString()).build());
                return new ParseResult(parsed, true);
            }
            issues.add(ImportIssue.builder()
                    .rowIndex(rowIndex).column(column)
                    .code(ImportIssue.CODE_AMBIGUOUS_NUMBER)
                    .severity(ImportIssue.Severity.ERROR)
                    .message("Valeur non numérique en ligne " + rowIndex + " : '" + raw + "'")
                    .originalValue(raw).build());
            return new ParseResult(null, true);
        }


        String cleaned = trimmed
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(',', '.');


        int lastDot = cleaned.lastIndexOf('.');
        if (lastDot >= 0) {
            String intPart = cleaned.substring(0, lastDot).replace(".", "");
            cleaned = intPart + cleaned.substring(lastDot);
        }

        if (cleaned.isBlank() || cleaned.equals("-") || cleaned.equals(".")) {
            return new ParseResult(null, false);
        }

        Double result = tryParseDouble(cleaned);
        if (result != null) {

            if (!cleaned.equals(trimmed)) {
                issues.add(ImportIssue.builder()
                        .rowIndex(rowIndex).column(column)
                        .code(ImportIssue.CODE_AUTO_CORRECTED)
                        .severity(ImportIssue.Severity.INFO)
                        .message("Valeur normalisée automatiquement en ligne " + rowIndex)
                        .originalValue(raw).cleanedValue(result.toString()).build());
            }
            return new ParseResult(result, false);
        }

        issues.add(ImportIssue.builder()
                .rowIndex(rowIndex).column(column)
                .code(ImportIssue.CODE_INVALID_NUMBER)
                .severity(ImportIssue.Severity.ERROR)
                .message("Valeur non numérique en ligne " + rowIndex + " : '" + raw + "'")
                .originalValue(raw).build());
        return new ParseResult(null, true);
    }

    private Double tryParseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return null; }
    }


    public static String normalizeForMatching(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}", "");      // supprime les diacritiques
        s = s.toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9\\s]", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }


    private boolean isRowCompletelyEmpty(String kpiName, String categorie, String unite, String n1Raw, String nRaw) {
        return (kpiName == null || kpiName.isBlank())
                && (categorie == null || categorie.isBlank())
                && (unite == null || unite.isBlank())
                && (n1Raw == null || n1Raw.isBlank())
                && (nRaw == null || nRaw.isBlank());
    }

    private List<String> buildDetectedHeaders(Sheet sheet, int headerRowIndex,
                                               Map<String, Integer> mapping,
                                               FormulaEvaluator evaluator, DataFormatter formatter) {
        List<String> headers = new ArrayList<>();
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) return headers;
        int maxCol = Math.max(5, headerRow.getLastCellNum());
        for (int c = 0; c < maxCol; c++) {
            String v = ExcelParserUtil.getCellValue(headerRow.getCell(c), evaluator, formatter);
            headers.add(v != null && !v.isBlank() ? v.trim() : "Colonne " + c);
        }
        return headers;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }


    public static final class ExtractionResult {
        private final List<KpiRawDataDTO> rows;
        private final String extractionMethod;
        private final List<String> detectedHeaders;
        private final List<ImportIssue> extractionIssues;

        public ExtractionResult(List<KpiRawDataDTO> rows, String extractionMethod, List<String> detectedHeaders, List<ImportIssue> extractionIssues) {
            this.rows = rows;
            this.extractionMethod = extractionMethod;
            this.detectedHeaders = detectedHeaders;
            this.extractionIssues = extractionIssues;
        }
        public List<KpiRawDataDTO> getRows()             { return rows; }
        public String getExtractionMethod()              { return extractionMethod; }
        public List<String> getDetectedHeaders()         { return detectedHeaders; }
        public List<ImportIssue> getExtractionIssues()   { return extractionIssues; }
    }

    private enum ExtractionMethod { CUSTOM }
}
