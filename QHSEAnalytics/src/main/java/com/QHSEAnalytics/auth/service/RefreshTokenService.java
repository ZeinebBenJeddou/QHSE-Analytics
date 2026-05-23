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
    private long refreshTokenExpiration;

    @Value("${app.jwt.refresh-token-remember-expiration}")
    private long refreshTokenRememberExpiration;


    @Transactional
    public RefreshToken createRefreshToken(User user, boolean rememberMe) {

        refreshTokenRepository.revokeAllForUser(user);

        long expMs = rememberMe ? refreshTokenRememberExpiration : refreshTokenExpiration;

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(expMs / 1000))
                .build();

        return refreshTokenRepository.save(token);
    }


    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenValue) {
        RefreshToken old = refreshTokenRepository
                .findByTokenAndRevokedFalse(oldTokenValue)
                .filter(rt -> !rt.isExpired())
                .orElseThrow(() -> new RefreshTokenInvalidException("Refresh token invalide ou expiré"));

        old.setRevoked(true);
        refreshTokenRepository.save(old);

        boolean wasRememberMe = old.getExpiresAt()
                .isAfter(LocalDateTime.now().plusHours(2));
        long expMs = wasRememberMe ? refreshTokenRememberExpiration : refreshTokenExpiration;

        RefreshToken newToken = RefreshToken.builder()
                .user(old.getUser())
                .expiresAt(LocalDateTime.now().plusSeconds(expMs / 1000))
                .build();

        return refreshTokenRepository.save(newToken);
    }


    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllForUser(user);
    }
}

