package com.QHSEAnalytics.importer.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile store(MultipartFile file, Long userId, int yearN);

    void delete(String storagePath);

    record StoredFile(
            String path,
            String bucket,
            long sizeBytes,
            String checksum
    ) {}
}
