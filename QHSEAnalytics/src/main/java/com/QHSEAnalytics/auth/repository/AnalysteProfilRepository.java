package com.QHSEAnalytics.auth.repository;

import com.QHSEAnalytics.auth.entity.AnalysteProfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysteProfilRepository extends JpaRepository<AnalysteProfil, Long> {
    Optional<AnalysteProfil> findByUserId(Long userId);
}
