package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class CleaningAgent {

    //seuil val abberantes configurable
    @Value("${import.cleaning.outlier.threshold.percent:500.0}")
    private double outlierThresholdPercent;


    public List<KpiRawDataDTO> clean(List<KpiRawDataDTO> rawData) {
        if (rawData == null || rawData.isEmpty()) return new ArrayList<>();

        log.info("CleaningAgent: démarrage pour {} lignes", rawData.size());

        List<KpiRawDataDTO> sanitized = rawData.stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeRow)
                .collect(Collectors.toList());

        List<KpiRawDataDTO> cleaned = deduplicateWithIssues(sanitized);


        long uppercaseConverted = rawData.stream()
                .filter(Objects::nonNull)
                .filter(r -> {
                    String orig = r.getKpiName();
                    if (orig == null || orig.isBlank()) return false;
                    String letters = orig.replaceAll("[^a-zA-ZÀ-ÿ]", "");
                    return !letters.isEmpty() && letters.equals(letters.toUpperCase());
                }).count();

        long exactDuplicates = cleaned.stream()
                .flatMap(r -> (r.getIssues() == null ? List.<com.QHSEAnalytics.shared.dto.response.ImportIssue>of() : r.getIssues()).stream())
                .filter(i -> com.QHSEAnalytics.shared.dto.response.ImportIssue.CODE_DUPLICATE_KPI.equals(i.getCode()))
                .count();

        long conflictDuplicates = cleaned.stream()
                .flatMap(r -> (r.getIssues() == null ? List.<com.QHSEAnalytics.shared.dto.response.ImportIssue>of() : r.getIssues()).stream())
                .filter(i -> com.QHSEAnalytics.shared.dto.response.ImportIssue.CODE_DUPLICATE_CONFLICT.equals(i.getCode()))
                .count();

        long outliers = cleaned.stream()
                .flatMap(r -> (r.getIssues() == null ? List.<com.QHSEAnalytics.shared.dto.response.ImportIssue>of() : r.getIssues()).stream())
                .filter(i -> com.QHSEAnalytics.shared.dto.response.ImportIssue.CODE_OUTLIER_VARIATION.equals(i.getCode()))
                .count();


        java.util.Set<String> nullTokens = java.util.Set.of(
                "n/a", "na", "nd", "nr", "nc", "–", "—", "/", "?", "x",
                "néant", "neant", "aucun", "none", "null", "empty");
        java.util.Map<String, Long> nullByToken = cleaned.stream()
                .flatMap(r -> {
                    List<String> raws = new ArrayList<>();
                    if (r.getValeurN1Raw() != null) raws.add(r.getValeurN1Raw().trim().toLowerCase(java.util.Locale.ROOT));
                    if (r.getValeurNRaw()  != null) raws.add(r.getValeurNRaw().trim().toLowerCase(java.util.Locale.ROOT));
                    return raws.stream();
                })
                .filter(nullTokens::contains)
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        long totalNulls = nullByToken.values().stream().mapToLong(Long::longValue).sum();
        String nullDetail = nullByToken.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(", "));

        log.info("\n[CLEANING]" +
                        "\n  Majuscules converties    : {}" +
                        "\n  Doublons exacts          : {} | Conflictuels : {}" +
                        "\n  Valeurs aberrantes       : {}" +
                        "\n  Valeurs nulles/manquantes: {} ({})" +
                        "\n  Lignes résultantes       : {}",
                uppercaseConverted,
                exactDuplicates, conflictDuplicates,
                outliers,
                totalNulls, nullDetail.isEmpty() ? "aucune" : nullDetail,
                cleaned.size());

        return cleaned;
    }


    private KpiRawDataDTO sanitizeRow(KpiRawDataDTO row) {

        String cleanedName     = toTitleCaseIfAllCaps(trimAndNormalize(row.getKpiName()));
        String cleanedCategorie = trimAndNormalize(row.getCategorie());
        String cleanedUnite    = trimAndNormalize(row.getUnite());


        String normalizedName = (row.getNormalizedKpiName() != null)
                ? row.getNormalizedKpiName()
                : (cleanedName != null ? ExtractionAgent.normalizeForMatching(cleanedName) : null);


        List<ImportIssue> issues = new ArrayList<>(
                row.getIssues() == null ? List.of() : row.getIssues());

        String validationMessage = row.getValidationMessage();

        // lignes valides ==> ref stric non null
        if (row.isValid() && row.getValeurN1() != null && row.getValeurN() != null) {
            double v1 = row.getValeurN1();
            double v  = row.getValeurN();
            if (v1 != 0) {
                // val aberantes
                double variation = Math.abs((v - v1) / v1) * 100;
                if (variation > outlierThresholdPercent) {
                    String msg = String.format(
                            "Variation extrême détectée pour '%s' (%.1f%%). Vérifiez les données.",
                            cleanedName, variation);
                    log.warn("CleaningAgent: outlier détecté — {}", msg);

                    issues.add(ImportIssue.builder()
                            .rowIndex(row.getRowIndex())
                            .column("Valeur N / Valeur N-1")
                            .code(ImportIssue.CODE_OUTLIER_VARIATION)
                            .severity(ImportIssue.Severity.WARNING)
                            .message(msg)
                            .build());

                    validationMessage = msg;
                }
            }
        }

        return KpiRawDataDTO.builder()
                .rowIndex(row.getRowIndex())
                .kpiName(cleanedName)
                .originalKpiName(row.getOriginalKpiName())
                .normalizedKpiName(normalizedName)
                .categorie(cleanedCategorie)
                .unite(cleanedUnite)
                .valeurN1(row.getValeurN1())
                .valeurN(row.getValeurN())
                .valeurN1Raw(row.getValeurN1Raw())
                .valeurNRaw(row.getValeurNRaw())
                .valid(row.isValid())
                .validationMessage(validationMessage)
                .methodeExtraction(row.getMethodeExtraction())
                .scoreConfiance(row.getScoreConfiance())
                .issues(issues)
                .build();
    }


    private List<KpiRawDataDTO> deduplicateWithIssues(List<KpiRawDataDTO> rows) {

        Map<String, KpiRawDataDTO> seen = new LinkedHashMap<>();
        List<KpiRawDataDTO> result = new ArrayList<>();

        for (KpiRawDataDTO row : rows) {
            String key = buildDedupeKey(row);

            // premiere occ conservée
            if (!seen.containsKey(key)) {
                seen.put(key, row);
                result.add(row);
                continue;
            }

            KpiRawDataDTO existing = seen.get(key);
            boolean exactDuplicate = isExactDuplicate(existing, row);

            if (exactDuplicate) {

                log.info("CleaningAgent: doublon exact ignoré pour '{}' (ligne {})",
                        row.getKpiName(), row.getRowIndex());
                addIssueToRow(existing,
                        ImportIssue.builder()
                                .rowIndex(row.getRowIndex())
                                .column("KPI")
                                .code(ImportIssue.CODE_DUPLICATE_KPI)
                                .severity(ImportIssue.Severity.INFO)  // doublon exacte ==>information
                                .message(String.format(
                                        "Doublon exact ignoré : '%s' (ligne %d est identique à la ligne %d)",
                                        row.getKpiName(), row.getRowIndex(), existing.getRowIndex()))
                                .build());
            } else {

                log.warn("CleaningAgent: doublon conflictuel pour '{}' (lignes {} et {})",
                        row.getKpiName(), existing.getRowIndex(), row.getRowIndex());
                addIssueToRow(existing,
                        ImportIssue.builder()
                                .rowIndex(row.getRowIndex())
                                .column("KPI")
                                .code(ImportIssue.CODE_DUPLICATE_CONFLICT)
                                .severity(ImportIssue.Severity.WARNING) //doublon conflictuel ==> avertissement
                                .message(String.format(
                                        "Doublon conflictuel : '%s' présent en lignes %d et %d avec des valeurs différentes. Première occurrence conservée.",
                                        row.getKpiName(), existing.getRowIndex(), row.getRowIndex()))
                                .build());
            }

        }
        return result;
    }


    // construction d cle de deduplication a partir du nom normalisé
    private String buildDedupeKey(KpiRawDataDTO row) {
        String norm = row.getNormalizedKpiName();
        if (norm == null || norm.isBlank()) {
            return "UNKNOWN_" + row.getRowIndex();
        }
        return norm;
    }

    private boolean isExactDuplicate(KpiRawDataDTO a, KpiRawDataDTO b) {
        return Objects.equals(a.getValeurN(),  b.getValeurN())
                && Objects.equals(a.getValeurN1(), b.getValeurN1());
    }


    private void addIssueToRow(KpiRawDataDTO row, ImportIssue issue) {
        List<ImportIssue> mutable = new ArrayList<>(
                row.getIssues() == null ? List.of() : row.getIssues());
        mutable.add(issue);
        row.setIssues(mutable);
    }

    private String trimAndNormalize(String value) {
        if (value == null) return null;

        // debarassement des espaces superflus
        return value.trim().replaceAll("\\s+", " ");
    }

    // majuscules converties en casse de titre
    private String toTitleCaseIfAllCaps(String value) {
        if (value == null || value.isBlank()) return value;
        String letters = value.replaceAll("[^a-zA-ZÀ-ÿ]", "");
        if (letters.isEmpty()) return value;
        if (!letters.equals(letters.toUpperCase())) return value;
        return Arrays.stream(value.split("\\s+"))
                .map(word -> word.isEmpty() ? word :
                        Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
