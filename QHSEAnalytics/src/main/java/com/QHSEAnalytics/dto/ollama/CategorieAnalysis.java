package com.QHSEAnalytics.dto.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorieAnalysis {
    @JsonProperty("categorie_code")
    private String categorieCode;

    @JsonProperty("categorie_libelle")
    private String categorieLibelle;

    private String contenu;
}
