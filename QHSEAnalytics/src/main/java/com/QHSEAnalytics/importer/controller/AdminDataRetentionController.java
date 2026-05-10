package com.QHSEAnalytics.importer.controller;

import com.QHSEAnalytics.config.ImportRetentionProperties;
import com.QHSEAnalytics.shared.dto.response.DataRetentionPolicyResponse;
import com.QHSEAnalytics.importer.service.ImportDataRetentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDataRetentionController {

    private final ImportRetentionProperties retentionProperties;
    private final ImportDataRetentionService retentionService;

    @GetMapping("/data-retention")
    public ResponseEntity<DataRetentionPolicyResponse> getDataRetentionPolicy() {
        ImportDataRetentionService.PurgeSummary summary = retentionService.getLastSummary();

        DataRetentionPolicyResponse.DataRetentionPolicyResponseBuilder builder = DataRetentionPolicyResponse.builder()
                .rawRetentionDays(retentionProperties.getRawDays())
                .previewRetentionDays(retentionProperties.getPreviewDays())
                .purgeCron(retentionProperties.getPurgeCron())
                .terminalStatuses(retentionService.terminalStatusesForPurge());

        // Null-safe population of summary fields
        if (summary != null) {
            builder.lastExecutedAt(summary.getExecutedAt())
                   .lastDurationMs(summary.getDurationMs())
                   .lastScannedImports(summary.getScannedImportCount())
                   .lastRawRowsDeleted(summary.getRawRowsDeleted())
                   .lastPreviewRowsDeleted(summary.getPreviewRowsDeleted());
        }

        return ResponseEntity.ok(builder.build());
    }

    @PostMapping("/data-retention/purge")
    public ResponseEntity<DataRetentionPolicyResponse> triggerPurge() {
        retentionService.purgeExpiredImportArtifacts();
        return getDataRetentionPolicy();
    }
}
