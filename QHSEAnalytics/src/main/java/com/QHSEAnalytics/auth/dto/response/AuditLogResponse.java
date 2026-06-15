package com.QHSEAnalytics.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private String adminEmail;
    private String action;
    private Long targetUserId;
    private String targetEmail;
    private String targetLabel;
    private String details;
    private LocalDateTime timestamp;
}
