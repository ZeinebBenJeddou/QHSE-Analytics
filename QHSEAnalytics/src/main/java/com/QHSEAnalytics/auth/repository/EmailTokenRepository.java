package com.QHSEAnalytics.auth.repository;

import com.QHSEAnalytics.auth.entity.EmailToken;
import com.QHSEAnalytics.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    Optional<EmailToken> findByTokenAndTypeAndUsedFalse(String token, EmailToken.EmailTokenType type);

    Optional<EmailToken> findTopByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(User user, EmailToken.EmailTokenType type);

    @Modifying
    @Query("UPDATE EmailToken t SET t.used = true WHERE t.user = :user AND t.type = :type AND t.used = false")
    void invalidateAllForUser(User user, EmailToken.EmailTokenType type);
}
