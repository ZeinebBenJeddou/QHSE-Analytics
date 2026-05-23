package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IaMetricsResponse {
    private double successCount;
    private double failedCount;
    private double retryCount;
    private double cacheHits;
    private double cacheMisses;
    private double avgLatencyMs;
    private double parseErrors;
    private double validationErrors;
    private double providerGroqCount;
}
