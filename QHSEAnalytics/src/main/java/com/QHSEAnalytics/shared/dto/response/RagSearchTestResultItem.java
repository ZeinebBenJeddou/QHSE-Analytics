package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RagSearchTestResultItem {
    private Long id;
    private String kpiName;
    private String category;
    private String definition;
}
