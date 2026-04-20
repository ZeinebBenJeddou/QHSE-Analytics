package com.QHSEAnalytics.dto.request;

import com.QHSEAnalytics.enums.TypeValeur;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ColonneMappingRequest {

    @NotBlank(message = "Le nom de colonne est obligatoire")
    private String nomColonne;

    @NotNull(message = "L'index de colonne est obligatoire")
    private Integer indexColonne;

    private Long kpiId;

    @NotNull(message = "Le type de valeur est obligatoire")
    private TypeValeur typeValeur;
}
