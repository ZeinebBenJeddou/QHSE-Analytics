package com.QHSEAnalytics.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DataRetentionPolicyResponse {
    private int rawRetentionDays;
    private int previewRetentionDays;
    private String purgeCron;
    private List<String> terminalStatuses;
    private LocalDateTime lastExecutedAt;
    private Long lastDurationMs;
    private Integer lastScannedImports;
    private Long lastRawRowsDeleted;
    private Long lastPreviewRowsDeleted;
}
