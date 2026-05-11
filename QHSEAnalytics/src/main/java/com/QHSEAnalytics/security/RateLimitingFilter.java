package com.QHSEAnalytics.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;


@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final InMemoryRateLimiter rateLimiter;

    @Value("${app.rate-limit.trust-proxy:false}")
    private boolean trustProxy;

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/verify-otp",
            "/api/auth/resend-otp",
            "/api/auth/forgot-password",
            "/api/auth/reset-password"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!isProtected(path)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        if (!rateLimiter.allowRequest(ip, path)) {
            log.warn("[RateLimit] Seuil dépassé — ip={} path={}", ip, path);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\"," +
                "\"message\":\"Trop de tentatives. Veuillez réessayer dans quelques minutes.\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isProtected(String path) {
        return PROTECTED_PATHS.stream().anyMatch(path::startsWith);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
