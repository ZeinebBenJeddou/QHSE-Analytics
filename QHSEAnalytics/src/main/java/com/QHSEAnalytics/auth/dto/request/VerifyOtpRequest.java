package com.QHSEAnalytics.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Data;


@Data
public class VerifyOtpRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "Le code OTP doit contenir 6 chiffres")
    private String code;

    private boolean rememberMe;
}