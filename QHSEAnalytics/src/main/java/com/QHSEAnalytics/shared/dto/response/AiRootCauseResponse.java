package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRootCauseResponse {

    /** Nom du KPI concerné */
    private String kpiRef;

    /** Méthode utilisée : "5_whys" */
    private String method;

    /** Chaîne des 3 à 5 questions Pourquoi successives */
    private List<String> whyChain;

    /** Catégorie Ishikawa : Homme / Machine / Méthode / Milieu / Matière */
    private String ishikawaCategory;

    /** Cause racine identifiée */
    private String rootCause;
}
