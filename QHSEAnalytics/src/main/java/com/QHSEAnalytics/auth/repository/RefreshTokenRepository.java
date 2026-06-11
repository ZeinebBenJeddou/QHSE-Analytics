package com.QHSEAnalytics.auth.repository;

import com.QHSEAnalytics.auth.entity.RefreshToken;
import com.QHSEAnalytics.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    @Query("SELECT r FROM RefreshToken r WHERE r.user.id = :userId AND r.revoked = false AND r.rememberMe = true ORDER BY r.expiresAt DESC")
    List<RefreshToken> findRememberMeTokensByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user AND r.revoked = false")
    void revokeAllForUser(User user);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}