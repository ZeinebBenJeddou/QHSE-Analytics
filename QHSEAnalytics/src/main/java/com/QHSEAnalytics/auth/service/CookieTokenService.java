package com.QHSEAnalytics.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class CookieTokenService {

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.jwt.access-token-expiration:1800000}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration:3600000}")
    private long refreshTokenExpirationMs;

    @Value("${app.jwt.refresh-token-remember-expiration:604800000}")
    private long rememberMeExpirationMs;

    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .maxAge(Duration.ofMillis(accessTokenExpirationMs))
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token, boolean rememberMe) {
        long expirationMs = rememberMe ? rememberMeExpirationMs : refreshTokenExpirationMs;
        ResponseCookie cookie = ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .maxAge(Duration.ofMillis(expirationMs))
                .path("/api/auth/refresh")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, "access_token", "/");
        clearCookie(response, "refresh_token", "/api/auth/refresh");
    }

    public String extractFromCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        ResponseCookie expired = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .maxAge(0)
                .path(path)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
    }
}
