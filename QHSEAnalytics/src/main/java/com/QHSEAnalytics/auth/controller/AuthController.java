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
import com.QHSEAnalytics.auth.service.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentification", description = "Inscription, connexion OTP, refresh token, reset mot de passe")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieTokenService cookieTokenService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Inscription d'un nouvel analyste", description = "Crée un compte non vérifié et envoie un email de confirmation")
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

    @Operation(summary = "Initier la connexion", description = "Vérifie les identifiants et envoie un OTP par email")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Valider l'OTP et obtenir les tokens", description = "Vérifie l'OTP et retourne JWT + refresh token")
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse response) {
        String clientIp = httpRequest.getRemoteAddr();
        AuthResponse auth = authService.verifyOtpAndLogin(request, clientIp);
        cookieTokenService.setAccessTokenCookie(response, auth.getAccessToken());
        cookieTokenService.setRefreshTokenCookie(response, auth.getRefreshToken(), request.isRememberMe());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String email) {
        return ResponseEntity.ok(authService.resendOtp(email));
    }

    @Operation(summary = "Renouveler le JWT", description = "Rotation du refresh token et émission d'un nouveau JWT")
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

    @Operation(summary = "Demander un reset de mot de passe", description = "Envoie un lien de reset si l'email existe")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @Operation(summary = "Réinitialiser le mot de passe", description = "Valide le token de reset et met à jour le mot de passe")
    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        MessageResponse result = authService.logout(securityUtils.getCurrentUserEmail());
        cookieTokenService.clearAuthCookies(response);
        return ResponseEntity.ok(result);
    }
}