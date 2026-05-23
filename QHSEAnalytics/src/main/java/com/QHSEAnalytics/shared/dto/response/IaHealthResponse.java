package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class IaHealthResponse {
    private List<ProviderStatusResponse> providers;
    private IaMetricsResponse metrics;
    private boolean llmCacheEnabled;
}
