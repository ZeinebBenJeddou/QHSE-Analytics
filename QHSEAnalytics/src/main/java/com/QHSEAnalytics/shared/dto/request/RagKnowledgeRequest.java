package com.QHSEAnalytics.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RagKnowledgeRequest {

    @NotBlank(message = "Le nom du KPI est obligatoire")
    @Size(max = 255)
    private String kpiName;

    private String definition;

    private String thresholds;

    @Size(max = 50)
    private String category;
}
