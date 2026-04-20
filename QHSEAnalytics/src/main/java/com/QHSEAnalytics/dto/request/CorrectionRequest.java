package com.QHSEAnalytics.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CorrectionRequest {

    @NotNull(message = "L'identifiant de la donnée staging est obligatoire")
    private Long stagingDonneeId;

    private Double valeurN1;

    private Double valeurN;
}
