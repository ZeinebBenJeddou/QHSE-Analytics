package com.QHSEAnalytics.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SaveMappingRequest {

    @NotBlank(message = "Le nom du mapping est obligatoire")
    private String nom;

    @NotNull(message = "La ligne d'entête est obligatoire")
    @Min(value = 1, message = "La ligne d'entête doit être >= 1")
    private Integer ligneEntete;

    @NotEmpty(message = "La liste des colonnes est obligatoire")
    @Valid
    private List<ColonneMappingRequest> colonnes;
}
