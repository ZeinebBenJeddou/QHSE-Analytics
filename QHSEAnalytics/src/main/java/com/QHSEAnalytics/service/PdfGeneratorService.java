package com.QHSEAnalytics.service;

import com.QHSEAnalytics.exception.PdfGenerationException;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGeneratorService {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public byte[] generatePdf(String htmlContent) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            HtmlConverter.convertToPdf(htmlContent, outputStream, new ConverterProperties());
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Erreur generation PDF : {}", e.getMessage(), e);
            throw new PdfGenerationException("Impossible de generer le rapport PDF.");
        }
    }

    public String generateFileName(String type, int periodeN1, int periodeN) {
        String timestamp = LocalDate.now().format(FILE_DATE_FORMAT);
        return String.format("rapport_qhse_%s_%d_%d_%s.pdf", type, periodeN1, periodeN, timestamp);
    }
}
