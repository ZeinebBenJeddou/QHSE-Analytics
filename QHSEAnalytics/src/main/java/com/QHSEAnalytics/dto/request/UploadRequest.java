package com.QHSEAnalytics.dto.request;

import com.QHSEAnalytics.enums.ImportMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UploadRequest {

    @NotNull(message = "Le mode d'import est obligatoire")
    private ImportMode mode;

    private Long mappingTemplateId;

    @NotNull(message = "La période N-1 est obligatoire")
    @Min(value = 2000, message = "La période N-1 doit être >= 2000")
    @Max(value = 2100, message = "La période N-1 doit être <= 2100")
    private Integer periodeN1;

    @NotNull(message = "La période N est obligatoire")
    @Min(value = 2000, message = "La période N doit être >= 2000")
    @Max(value = 2100, message = "La période N doit être <= 2100")
    private Integer periodeN;
}
