package com.QHSEAnalytics.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagSearchTestRequest {

    @NotBlank(message = "La requête est obligatoire")
    private String query;

    private int topK = 5;

    private double threshold = 0.50;
}
