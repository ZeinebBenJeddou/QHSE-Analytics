package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RagKnowledgeResponse {
    private Long id;
    private String kpiName;
    private String definition;
    private String thresholds;
    private String category;
    private boolean hasEmbedding;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
