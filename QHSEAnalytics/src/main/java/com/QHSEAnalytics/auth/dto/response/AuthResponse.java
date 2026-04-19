package com.QHSEAnalytics.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    private String email;
    private String nom;
    private String prenom;
    private String role;
}