package com.QHSEAnalytics.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysteProfilDTO {

    @Size(max = 100)
    private String secteurActivite;

    @Size(max = 50)
    private String tailleSite;

    @Size(max = 200)
    private String certifications;

    @Size(max = 500)
    private String objectifsQhse;

    @Size(max = 200)
    private String reglementation;

    @Size(max = 500)
    private String contexteSpecifique;
}
