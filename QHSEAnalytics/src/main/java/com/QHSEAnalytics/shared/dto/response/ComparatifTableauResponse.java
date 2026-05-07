package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComparatifTableauResponse {
    private Long importId;
    private Integer periodeN1;
    private Integer periodeN;
    private LocalDateTime dateAnalyse;
    private List<LigneComparatifResponse> lignes;
    private int nombreCritiques;
    private int nombreModeres;
    private int nombreFaibles;
}