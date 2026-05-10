package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ProviderStatusResponse {
    private String name;
    private boolean available;
    private Instant cooldownUntil;
}
