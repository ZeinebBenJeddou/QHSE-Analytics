package com.QHSEAnalytics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminRepartitionResponse {

    private List<RepartitionCategorieItem> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepartitionCategorieItem {
        private String categorieCode;
        private String categorieLibelle;
        private int nombreFaibles;
        private int nombreModeres;
        private int nombreCritiques;
        private int total;
    }
}
