package com.QHSEAnalytics.auth.service;


import com.QHSEAnalytics.auth.exception.RefreshTokenInvalidException;
import com.QHSEAnalytics.auth.entity.RefreshToken;
import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;           // 1h

    @Value("${app.jwt.refresh-token-remember-expiration}")
    private long refreshTokenRememberExpiration;   // 7j

    // crée refresh token
    @Transactional
    public RefreshToken createRefreshToken(User user, boolean rememberMe) {
        // révoquer anciens tokens
        refreshTokenRepository.revokeAllForUser(user);

        long expMs = rememberMe ? refreshTokenRememberExpiration : refreshTokenExpiration;

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(expMs / 1000))
                .build();

        return refreshTokenRepository.save(token);
    }

    // valide & retourne refresh token
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        return refreshTokenRepository
                .findByTokenAndRevokedFalse(token)
                .filter(rt -> !rt.isExpired())
                .orElseThrow(() -> new RefreshTokenInvalidException("Refresh token invalide ou expiré"));
    }

    // révoque tt tokens d'un user
    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllForUser(user);
    }
}

