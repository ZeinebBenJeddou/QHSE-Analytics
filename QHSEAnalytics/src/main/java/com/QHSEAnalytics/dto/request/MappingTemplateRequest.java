package com.QHSEAnalytics.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MappingTemplateRequest {

    @NotBlank(message = "Le nom du template est obligatoire")
    private String templateName;

    @NotEmpty(message = "Au moins un mapping est requis")
    @Valid
    private List<MappingConfigItemRequest> mappings;

    @Data
    public static class MappingConfigItemRequest {
        @NotBlank(message = "La colonne Excel est obligatoire")
        private String excelColumn;
        private Long kpiId;
        private String categorieCode;
    }
}
