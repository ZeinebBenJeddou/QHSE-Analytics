package com.QHSEAnalytics.service;

import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.exception.ImportValidationException;
import com.QHSEAnalytics.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExcelParserService {

    private final KpiRepository kpiRepository;

    public List<DonneeExtraite> parseTemplateOfficiel(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheet("Données QHSE");
            if (sheet == null) {
                throw new ImportValidationException("Feuille 'Données QHSE' introuvable.");
            }

            Map<String, Kpi> kpiParNomNormalise = kpiRepository.findAllByOrderByOrdreAsc().stream()
                    .collect(Collectors.toMap(
                            kpi -> normaliser(kpi.getNom()),
                            kpi -> kpi,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));

            List<DonneeExtraite> result = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String kpiNom = readCellAsString(row.getCell(1));
                if (kpiNom == null || kpiNom.isBlank()) {
                    continue;
                }

                Kpi kpi = kpiParNomNormalise.get(normaliser(kpiNom.trim()));
                if (kpi == null) {
                    continue;
                }

                String valeurBruteN1 = readCellAsString(row.getCell(3));
                String valeurBruteN = readCellAsString(row.getCell(4));
                result.add(new DonneeExtraite(kpi.getId(), kpiNom.trim(), valeurBruteN1, valeurBruteN));
            }

            return result;
        } catch (Exception ex) {
            throw new ImportValidationException("Lecture du fichier impossible.");
        }
    }


    private String readCellAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        DataFormatter formatter = new DataFormatter();
        String value = formatter.formatCellValue(cell);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        String lowered = texte.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD);
        return java.util.regex.Pattern.compile("[^\\p{ASCII}]").matcher(normalized).replaceAll("").trim();
    }

    public record DonneeExtraite(
            Long kpiId,
            String kpiNom,
            String valeurBruteN1,
            String valeurBruteN
    ) {}
}
