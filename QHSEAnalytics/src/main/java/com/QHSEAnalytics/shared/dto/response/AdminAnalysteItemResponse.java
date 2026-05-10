package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminAnalysteItemResponse {
    private Long userId;
    private String nom;
    private String prenom;
    private String email;
    private Long dernierImportId;
    private String dernierePeriode;
    private LocalDateTime dernierImportDate;
    private int nombreKpisCritiques;
    private String statut;
    private Integer confidenceScore;
}