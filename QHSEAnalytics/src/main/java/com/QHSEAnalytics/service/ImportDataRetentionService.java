package com.QHSEAnalytics.service;

import com.QHSEAnalytics.config.ImportRetentionProperties;
import com.QHSEAnalytics.enums.ImportStatut;
import com.QHSEAnalytics.repository.ImportSessionRepository;
import com.QHSEAnalytics.repository.KpiImportPreviewRepository;
import com.QHSEAnalytics.repository.KpiRawDataRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportDataRetentionService {

    private static final List<ImportStatut> TERMINAL_STATUSES = List.of(
            ImportStatut.READY_FOR_AI,
            ImportStatut.TRAITE,
            ImportStatut.ERREUR
    );

    /** Safety guard: refuse purge if more than this many import sessions would be deleted in one execution. */
    private static final int MAX_PURGE_SESSIONS_PER_RUN = 10000;

    /** Safety guard: refuse purge if more than this many rows would be deleted in one execution. */
    private static final long MAX_ROWS_DELETE_PER_RUN = 50000;

    private final ImportRetentionProperties retentionProperties;
    private final ImportSessionRepository importSessionRepository;
    private final KpiRawDataRepository kpiRawDataRepository;
    private final KpiImportPreviewRepository kpiImportPreviewRepository;

    private volatile PurgeSummary lastSummary;

    @Scheduled(cron = "${app.import.retention.purge-cron:0 30 2 * * *}")
    @Transactional
    public void purgeExpiredImportArtifacts() {
        LocalDateTime startedAt = LocalDateTime.now();

        LocalDateTime rawCutoff = startedAt.minusDays(retentionProperties.getRawDays());
        LocalDateTime previewCutoff = startedAt.minusDays(retentionProperties.getPreviewDays());

        List<Long> rawImportIds = importSessionRepository.findIdsByCreatedAtBeforeAndStatutIn(rawCutoff, TERMINAL_STATUSES);
        List<Long> previewImportIds = importSessionRepository.findIdsByCreatedAtBeforeAndStatutIn(previewCutoff, TERMINAL_STATUSES);

        Set<Long> scanned = new HashSet<>();
        scanned.addAll(rawImportIds);
        scanned.addAll(previewImportIds);

        // Safety guard: refuse if excessive number of sessions would be purged
        if (scanned.size() > MAX_PURGE_SESSIONS_PER_RUN) {
            log.warn("Purge guard triggered (sessions): {} sessions exceed max({}). Aborting to prevent accidental mass deletion.",
                    scanned.size(), MAX_PURGE_SESSIONS_PER_RUN);
            this.lastSummary = PurgeSummary.builder()
                    .executedAt(startedAt)
                    .scannedImportCount(scanned.size())
                    .aborted(true)
                    .durationMs(Duration.between(startedAt, LocalDateTime.now()).toMillis())
                    .build();
            return;
        }

        // Estimate total rows to delete (safety guard for row count)
        long estimatedRawRows = !rawImportIds.isEmpty() ? kpiRawDataRepository.countByImportSessionIdIn(rawImportIds) : 0;
        long estimatedPreviewRows = !previewImportIds.isEmpty() ? kpiImportPreviewRepository.countByImportSessionIdIn(previewImportIds) : 0;
        long totalRowsEstimate = estimatedRawRows + estimatedPreviewRows;

        if (totalRowsEstimate > MAX_ROWS_DELETE_PER_RUN) {
            log.warn("Purge guard triggered (rows): estimated {} rows exceed max({}). Aborting to prevent accidental mass deletion.",
                    totalRowsEstimate, MAX_ROWS_DELETE_PER_RUN);
            this.lastSummary = PurgeSummary.builder()
                    .executedAt(startedAt)
                    .scannedImportCount(scanned.size())
                    .aborted(true)
                    .durationMs(Duration.between(startedAt, LocalDateTime.now()).toMillis())
                    .build();
            return;
        }

        long rawDeleted = 0;
        long previewDeleted = 0;

        if (!rawImportIds.isEmpty()) {
            if (estimatedRawRows > 0) {
                kpiRawDataRepository.deleteByImportSessionIds(rawImportIds);
                rawDeleted = estimatedRawRows;
                log.info("Purge: deleted {} raw data rows from {} import sessions", rawDeleted, rawImportIds.size());
            }
        }

        if (!previewImportIds.isEmpty()) {
            if (estimatedPreviewRows > 0) {
                kpiImportPreviewRepository.deleteByImportSessionIds(previewImportIds);
                previewDeleted = estimatedPreviewRows;
                log.info("Purge: deleted {} preview rows from {} import sessions", previewDeleted, previewImportIds.size());
            }
        }

        long durationMs = Duration.between(startedAt, LocalDateTime.now()).toMillis();
        PurgeSummary summary = PurgeSummary.builder()
                .executedAt(startedAt)
                .scannedImportCount(scanned.size())
                .rawRowsDeleted(rawDeleted)
                .previewRowsDeleted(previewDeleted)
                .durationMs(durationMs)
                .aborted(false)
                .build();
        this.lastSummary = summary;

        log.info("Import retention purge completed: scannedImports={}, rawRows={}, previewRows={}, totalDeleted={}, durationMs={}",
                summary.getScannedImportCount(), summary.getRawRowsDeleted(), summary.getPreviewRowsDeleted(),
                summary.getRawRowsDeleted() + summary.getPreviewRowsDeleted(), summary.getDurationMs());
    }

    public PurgeSummary getLastSummary() {
        return lastSummary;
    }

    public List<String> terminalStatusesForPurge() {
        List<String> statuses = new ArrayList<>();
        for (ImportStatut status : TERMINAL_STATUSES) {
            statuses.add(status.name());
        }
        return statuses;
    }

    @Getter
    @Builder
    public static class PurgeSummary {
        private LocalDateTime executedAt;
        private int scannedImportCount;
        private long rawRowsDeleted;
        private long previewRowsDeleted;
        private long durationMs;
        @Builder.Default
        private boolean aborted = false;
    }
}
