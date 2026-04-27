package com.QHSEAnalytics.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MappingTemplateResponse {
    private Long id;
    private String templateName;
    private List<MappingItemResponse> mappings;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class MappingItemResponse {
        private Long id;
        private String excelColumn;
        private Long kpiId;
        private String kpiNom;
        private String categorieCode;
    }
}
