package com.QHSEAnalytics.dto.response;

import com.QHSEAnalytics.enums.ImportMode;
import com.QHSEAnalytics.enums.ImportStatut;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportSessionResponse {
    private Long id;
    private ImportMode mode;
    private String nomFichier;
    private Integer periodeN1;
    private Integer periodeN;
    private ImportStatut statut;
    private String messageErreur;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long nombreTotal;
    private long nombreOk;
    private long nombreCorrige;
    private long nombreManquant;
    private long nombreInvalide;
    private long nombreSuspect;
}
