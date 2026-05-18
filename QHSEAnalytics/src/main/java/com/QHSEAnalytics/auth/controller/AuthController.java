package com.QHSEAnalytics.auth.controller;

import com.QHSEAnalytics.auth.dto.request.ForgotPasswordRequest;
import com.QHSEAnalytics.auth.dto.request.LoginRequest;
import com.QHSEAnalytics.auth.dto.request.RegisterRequest;
import com.QHSEAnalytics.auth.dto.request.ResetPasswordRequest;
import com.QHSEAnalytics.auth.dto.request.VerifyOtpRequest;
import com.QHSEAnalytics.auth.dto.response.AuthResponse;
import com.QHSEAnalytics.auth.dto.response.MessageResponse;
import com.QHSEAnalytics.auth.service.AuthService;
import com.QHSEAnalytics.auth.service.CookieTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieTokenService cookieTokenService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyAccount(token));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resend(@RequestParam String email) {
        return ResponseEntity.ok(authService.resendVerification(email));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
                                       HttpServletResponse response) {
        AuthResponse auth = authService.verifyOtpAndLogin(request);
        cookieTokenService.setAccessTokenCookie(response, auth.getAccessToken());
        cookieTokenService.setRefreshTokenCookie(response, auth.getRefreshToken(), request.isRememberMe());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String email) {
        return ResponseEntity.ok(authService.resendOtp(email));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieTokenService.extractFromCookie(request, "refresh_token");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Session expirée. Veuillez vous reconnecter."));
        }
        AuthResponse auth = authService.refreshToken(refreshToken);
        cookieTokenService.setAccessTokenCookie(response, auth.getAccessToken());
        cookieTokenService.setRefreshTokenCookie(response, auth.getRefreshToken(), false);
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        MessageResponse result = authService.logout(email);
        cookieTokenService.clearAuthCookies(response);
        return ResponseEntity.ok(result);
    }
}