package com.QHSEAnalytics.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.import.retention")
@Getter
@Setter
public class ImportRetentionProperties {

    @Min(1)
    private int rawDays = 15;

    @Min(1)
    private int previewDays = 15;

    @NotBlank
    private String purgeCron = "0 30 2 * * *";
}
