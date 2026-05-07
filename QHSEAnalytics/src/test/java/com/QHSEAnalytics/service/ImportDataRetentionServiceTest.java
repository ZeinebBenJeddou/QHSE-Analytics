package com.QHSEAnalytics.service;

import com.QHSEAnalytics.config.ImportRetentionProperties;
import com.QHSEAnalytics.enums.ImportStatut;
import com.QHSEAnalytics.repository.ImportSessionRepository;
import com.QHSEAnalytics.repository.KpiImportPreviewRepository;
import com.QHSEAnalytics.repository.KpiRawDataRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

class ImportDataRetentionServiceTest {

    @Test
    void purgeExpiredRowsForTerminalImports() {
        ImportRetentionProperties props = new ImportRetentionProperties();
        props.setRawDays(15);
        props.setPreviewDays(15);
        props.setPurgeCron("0 30 2 * * *");

        ImportSessionRepository sessionRepository = Mockito.mock(ImportSessionRepository.class);
        KpiRawDataRepository rawRepository = Mockito.mock(KpiRawDataRepository.class);
        KpiImportPreviewRepository previewRepository = Mockito.mock(KpiImportPreviewRepository.class);

        Mockito.when(sessionRepository.findIdsByCreatedAtBeforeAndStatutIn(any(LocalDateTime.class), anyList()))
                .thenReturn(List.of(101L, 102L));
        Mockito.when(rawRepository.countByImportSessionIdIn(List.of(101L, 102L))).thenReturn(12L);
        Mockito.when(previewRepository.countByImportSessionIdIn(List.of(101L, 102L))).thenReturn(7L);

        ImportDataRetentionService service = new ImportDataRetentionService(props, sessionRepository, rawRepository, previewRepository);
        service.purgeExpiredImportArtifacts();

        Mockito.verify(rawRepository).deleteByImportSessionIds(List.of(101L, 102L));
        Mockito.verify(previewRepository).deleteByImportSessionIds(List.of(101L, 102L));

        ImportDataRetentionService.PurgeSummary summary = service.getLastSummary();
        assertNotNull(summary);
        assertEquals(2, summary.getScannedImportCount());
        assertEquals(12L, summary.getRawRowsDeleted());
        assertEquals(7L, summary.getPreviewRowsDeleted());
    }

    @Test
    void keepRecentDataWhenNoExpiredImportFound() {
        ImportRetentionProperties props = new ImportRetentionProperties();
        ImportSessionRepository sessionRepository = Mockito.mock(ImportSessionRepository.class);
        KpiRawDataRepository rawRepository = Mockito.mock(KpiRawDataRepository.class);
        KpiImportPreviewRepository previewRepository = Mockito.mock(KpiImportPreviewRepository.class);

        Mockito.when(sessionRepository.findIdsByCreatedAtBeforeAndStatutIn(any(LocalDateTime.class), anyList()))
                .thenReturn(List.of());

        ImportDataRetentionService service = new ImportDataRetentionService(props, sessionRepository, rawRepository, previewRepository);
        service.purgeExpiredImportArtifacts();

        Mockito.verify(rawRepository, Mockito.never()).deleteByImportSessionIds(anyList());
        Mockito.verify(previewRepository, Mockito.never()).deleteByImportSessionIds(anyList());
        assertEquals(0, service.getLastSummary().getScannedImportCount());
    }

    @Test
    void queryOnlyTerminalStatuses() {
        ImportRetentionProperties props = new ImportRetentionProperties();
        ImportSessionRepository sessionRepository = Mockito.mock(ImportSessionRepository.class);
        KpiRawDataRepository rawRepository = Mockito.mock(KpiRawDataRepository.class);
        KpiImportPreviewRepository previewRepository = Mockito.mock(KpiImportPreviewRepository.class);

        Mockito.when(sessionRepository.findIdsByCreatedAtBeforeAndStatutIn(any(LocalDateTime.class), anyList()))
                .thenReturn(List.of());

        ImportDataRetentionService service = new ImportDataRetentionService(props, sessionRepository, rawRepository, previewRepository);
        service.purgeExpiredImportArtifacts();

        List<ImportStatut> terminal = List.of(ImportStatut.READY_FOR_AI, ImportStatut.TRAITE, ImportStatut.ERREUR);
        Mockito.verify(sessionRepository, Mockito.times(2))
                .findIdsByCreatedAtBeforeAndStatutIn(any(LocalDateTime.class), eq(terminal));
    }
}
