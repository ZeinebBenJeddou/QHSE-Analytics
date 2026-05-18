package com.QHSEAnalytics.importer.service;

import com.QHSEAnalytics.importer.service.processing.ExcelParserUtil;
import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DualFileImportService {

    /**
     * Merges two single-year Excel files into a list of KpiRawDataDTO usable by the existing pipeline.
     *
     * mapping keys expected: "kpiNameIndex" (int), "valueIndex" (int)
     */
    public MergeResult mergeFiles(
            MultipartFile fileN1,
            MultipartFile fileN,
            int anneeN1,
            int anneeN,
            Map<String, Integer> mappingN1,
            Map<String, Integer> mappingN
    ) {
        Map<String, Double> valeursN1 = extraireValeurs(fileN1, mappingN1);
        Map<String, Double> valeursN  = extraireValeurs(fileN,  mappingN);

        Set<String> tousLesKpis = new LinkedHashSet<>();
        tousLesKpis.addAll(valeursN1.keySet());
        tousLesKpis.addAll(valeursN.keySet());

        List<KpiRawDataDTO> result   = new ArrayList<>();
        List<String>        warnings = new ArrayList<>();
        int rowIndex = 1;

        for (String kpiName : tousLesKpis) {
            Double valN1 = valeursN1.get(kpiName);
            Double valN  = valeursN.get(kpiName);

            if (valN1 == null) {
                warnings.add("KPI '" + kpiName + "' absent du fichier N-1 (" + anneeN1 + ")");
                log.warn("[DualFile] KPI '{}' absent du fichier N-1 ({})", kpiName, anneeN1);
            }
            if (valN == null) {
                warnings.add("KPI '" + kpiName + "' absent du fichier N (" + anneeN + ")");
                log.warn("[DualFile] KPI '{}' absent du fichier N ({})", kpiName, anneeN);
            }

            boolean valid = valN1 != null && valN != null;

            List<ImportIssue> issues = new ArrayList<>();
            if (!valid) {
                String missingCol = valN1 == null ? "Valeur N-1" : "Valeur N";
                issues.add(ImportIssue.builder()
                        .rowIndex(rowIndex)
                        .column(missingCol)
                        .code("DUAL_FILE_ABSENT")
                        .severity(ImportIssue.Severity.WARNING)
                        .message("KPI '" + kpiName + "' absent d'un fichier — classé INDETERMINE")
                        .build());
            }

            KpiRawDataDTO dto = KpiRawDataDTO.builder()
                    .rowIndex(rowIndex++)
                    .kpiName(kpiName)
                    .originalKpiName(kpiName)
                    .normalizedKpiName(kpiName)
                    .categorie(null)
                    .unite(null)
                    .valeurN1(valN1)
                    .valeurN(valN)
                    .valeurN1Raw(valN1 != null ? valN1.toString() : null)
                    .valeurNRaw(valN  != null ? valN.toString()  : null)
                    .valid(valid)
                    .validationMessage(valid ? null : "KPI absent d'un des fichiers — INDETERMINE")
                    .methodeExtraction("DUAL_FILE")
                    .scoreConfiance(valid ? 0.85 : 0.0)
                    .issues(issues)
                    .build();

            result.add(dto);
        }

        log.info("[DualFile] Fusion terminée : {} KPIs, {} avertissements", result.size(), warnings.size());
        return new MergeResult(result, warnings);
    }

    private Map<String, Double> extraireValeurs(MultipartFile file, Map<String, Integer> mapping) {
        int kpiIdx   = mapping.getOrDefault("kpiNameIndex", 0);
        int valueIdx = mapping.getOrDefault("valueIndex",   1);

        Map<String, Double> valeurs = new LinkedHashMap<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = selectSheet(workbook);
            if (sheet == null) {
                log.warn("[DualFile] Aucune feuille trouvée dans {}", file.getOriginalFilename());
                return valeurs;
            }

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            int headerRow = detectHeaderRow(sheet, formatter, evaluator);

            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String kpiRaw   = ExcelParserUtil.getCellValue(row.getCell(kpiIdx),   evaluator, formatter);
                String valueRaw = ExcelParserUtil.getCellValue(row.getCell(valueIdx),  evaluator, formatter);

                if (kpiRaw == null || kpiRaw.isBlank()) continue;

                String kpiNorm = normaliserNom(kpiRaw);
                Double val     = parseDouble(valueRaw);

                if (val != null) {
                    valeurs.put(kpiNorm, val);
                }
            }

        } catch (Exception ex) {
            log.error("[DualFile] Erreur lecture fichier {}: {}", file.getOriginalFilename(), ex.getMessage(), ex);
        }

        return valeurs;
    }

    private Sheet selectSheet(Workbook wb) {
        if (wb.getNumberOfSheets() == 0) return null;
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            String name = wb.getSheetName(i).toLowerCase(Locale.ROOT);
            if (name.contains("donn") || name.contains("data") || name.contains("kpi") || name.contains("qhse")) {
                return wb.getSheetAt(i);
            }
        }
        return wb.getSheetAt(0);
    }

    private int detectHeaderRow(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (int i = 0; i <= Math.min(10, sheet.getLastRowNum()); i++) {
            Row r = sheet.getRow(i);
            if (r != null && ExcelParserUtil.countNonBlankCells(r, formatter, evaluator) >= 2) {
                return i;
            }
        }
        return 0;
    }

    static String normaliserNom(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}", "");
        s = s.toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9\\s]", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String cleaned = raw.trim()
                    .replace(" ", "")
                    .replace(" ", "")
                    .replace(',', '.');
            if (cleaned.endsWith("%")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static final class MergeResult {
        private final List<KpiRawDataDTO> rows;
        private final List<String>        warnings;

        public MergeResult(List<KpiRawDataDTO> rows, List<String> warnings) {
            this.rows     = rows;
            this.warnings = warnings;
        }

        public List<KpiRawDataDTO> getRows()     { return rows; }
        public List<String>        getWarnings()  { return warnings; }
    }
}
