package com.QHSEAnalytics.auth.repository;

import com.QHSEAnalytics.auth.entity.OtpCode;
import com.QHSEAnalytics.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("UPDATE OtpCode o SET o.used = true WHERE o.user = :user AND o.used = false")
    void invalidateAllForUser(User user);
}