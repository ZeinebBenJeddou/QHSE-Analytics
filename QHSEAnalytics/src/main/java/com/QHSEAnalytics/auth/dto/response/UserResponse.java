package com.QHSEAnalytics.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private boolean verified;
    private boolean active;
    private LocalDateTime createdAt;

    @JsonProperty("isSystemAdmin")
    private boolean systemAdmin;
}