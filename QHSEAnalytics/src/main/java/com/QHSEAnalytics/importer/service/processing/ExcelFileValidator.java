package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.exception.FileTooLargeException;
import com.QHSEAnalytics.shared.exception.InvalidFileFormatException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;


@Component
public class ExcelFileValidator {

    private static final long MAX_SIZE_BYTES = 15L * 1024 * 1024; // 15 MB


    private static final byte[] XLSX_MAGIC = {0x50, 0x4B};
    private static final byte[] XLS_MAGIC  = {(byte)0xD0, (byte)0xCF, 0x11, (byte)0xE0};

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/octet-stream"
    );

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileFormatException("Aucun fichier fourni.");
        }


        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new FileTooLargeException(
                String.format("Fichier trop volumineux : %.1f MB (max 15 MB)",
                    file.getSize() / (1024.0 * 1024.0)));
        }


        String originalName = file.getOriginalFilename();
        if (originalName == null ||
            !(originalName.toLowerCase().endsWith(".xlsx") || originalName.toLowerCase().endsWith(".xls"))) {
            throw new InvalidFileFormatException("Seuls les fichiers .xlsx et .xls sont acceptés.");
        }


        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new InvalidFileFormatException(
                "Type MIME non autorisé : " + contentType + ". Seuls les fichiers Excel sont acceptés.");
        }
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(8);
            if (!matchesMagic(header, XLSX_MAGIC) && !matchesMagic(header, XLS_MAGIC)) {
                throw new InvalidFileFormatException(
                    "Le fichier n'est pas un fichier Excel valide (signature incorrecte).");
            }
        } catch (IOException e) {
            throw new InvalidFileFormatException("Impossible de lire le fichier uploadé.");
        }
    }

    private boolean matchesMagic(byte[] header, byte[] magic) {
        if (header.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) return false;
        }
        return true;
    }
}
