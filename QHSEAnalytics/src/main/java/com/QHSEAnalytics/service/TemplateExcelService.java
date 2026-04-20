package com.QHSEAnalytics.service;

import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.exception.InvalidTemplateException;
import com.QHSEAnalytics.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateExcelService {

    public static final String TEMPLATE_SIGNATURE = "QHSE_ANALYTICS_TEMPLATE_V1";

    private final KpiRepository kpiRepository;

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet dataSheet = workbook.createSheet("Données QHSE");
            createHeaders(dataSheet);

            List<Kpi> kpis = kpiRepository.findByIsActiveTrueOrderByOrdreAsc();
            for (int i = 0; i < kpis.size(); i++) {
                Kpi kpi = kpis.get(i);
                Row row = dataSheet.createRow(i + 1);
                row.createCell(0).setCellValue(kpi.getCategorieKpi().getCode());
                row.createCell(1).setCellValue(kpi.getNom());
                row.createCell(2).setCellValue(kpi.getUnite().name());
                row.createCell(3).setCellValue("");
                row.createCell(4).setCellValue("");
            }

            for (int i = 0; i < 5; i++) {
                dataSheet.autoSizeColumn(i);
            }

            protectTemplateSheet(dataSheet);
            createMetadataSheet(workbook);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new InvalidTemplateException("Impossible de générer le template officiel.");
        }
    }

    public void validateTemplateSignature(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet metadata = workbook.getSheet("METADATA");
            if (metadata == null) {
                throw new InvalidTemplateException("Fichier non reconnu. Veuillez utiliser le template officiel.");
            }
            Row row = metadata.getRow(0);
            Cell cell = row != null ? row.getCell(0) : null;
            if (cell == null || cell.getCellType() != CellType.STRING) {
                throw new InvalidTemplateException("Fichier non reconnu. Veuillez utiliser le template officiel.");
            }
            String signature = cell.getStringCellValue();
            if (!TEMPLATE_SIGNATURE.equals(signature)) {
                throw new InvalidTemplateException("Fichier non reconnu. Veuillez utiliser le template officiel.");
            }
        } catch (Exception ex) {
            throw new InvalidTemplateException("Fichier non reconnu. Veuillez utiliser le template officiel.");
        }
    }

    private void createHeaders(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Catégorie");
        header.createCell(1).setCellValue("KPI");
        header.createCell(2).setCellValue("Unité");
        header.createCell(3).setCellValue("Valeur N-1");
        header.createCell(4).setCellValue("Valeur N");
    }

    private void protectTemplateSheet(Sheet dataSheet) {
        dataSheet.protectSheet("qhse-template");

        CellStyle unlockedStyle = dataSheet.getWorkbook().createCellStyle();
        unlockedStyle.setLocked(false);

        // CORRECTION : déverrouillage sur toutes les lignes KPI générées, pas seulement une plage fixe.
        for (int rowIndex = 1; rowIndex <= dataSheet.getLastRowNum(); rowIndex++) {
            Row row = dataSheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Cell n1Cell = row.getCell(3);
            Cell nCell = row.getCell(4);
            if (n1Cell != null) {
                n1Cell.setCellStyle(unlockedStyle);
            }
            if (nCell != null) {
                nCell.setCellStyle(unlockedStyle);
            }
        }
    }

    private void createMetadataSheet(Workbook workbook) {
        Sheet metadata = workbook.createSheet("METADATA");
        Row row = metadata.createRow(0);
        row.createCell(0).setCellValue(TEMPLATE_SIGNATURE);
        int index = workbook.getSheetIndex(metadata);
        workbook.setSheetHidden(index, true);
    }
}
