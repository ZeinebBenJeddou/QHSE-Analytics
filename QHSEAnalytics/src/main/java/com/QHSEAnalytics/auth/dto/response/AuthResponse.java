package com.QHSEAnalytics.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    @JsonIgnore
    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    private String email;
    private String nom;
    private String prenom;
    private String role;
}