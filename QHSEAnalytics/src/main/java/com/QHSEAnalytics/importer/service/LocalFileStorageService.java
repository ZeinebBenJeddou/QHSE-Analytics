package com.QHSEAnalytics.importer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.QHSEAnalytics.shared.exception.FileStorageException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private static final String BUCKET = "local";

    @Value("${qhse.storage.local.base-dir:./storage/imports}")
    private String baseDir;

    @Override
    public StoredFile store(MultipartFile file, Long userId, int yearN) {
        String filename = sanitize(file.getOriginalFilename());
        String relativePath = String.format("imports/%d/user-%d/%s-%s", yearN, userId, UUID.randomUUID(), filename);
        Path target = Paths.get(baseDir).resolve(relativePath).normalize();

        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String checksum = HexFormat.of().formatHex(digest.digest());
            log.info("[FileStorage] Stored {} ({} bytes) at {}", filename, file.getSize(), relativePath);
            return new StoredFile(relativePath, BUCKET, file.getSize(), checksum);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new FileStorageException("Impossible de stocker le fichier : " + filename, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) return;
        Path target = Paths.get(baseDir).resolve(storagePath).normalize();
        try {
            Files.deleteIfExists(target);
            log.info("[FileStorage] Deleted {}", storagePath);
        } catch (IOException e) {
            log.warn("[FileStorage] Could not delete {}: {}", storagePath, e.getMessage());
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) return "fichier";
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
