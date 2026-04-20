package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.ImportSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportSessionRepository extends JpaRepository<ImportSession, Long> {

    List<ImportSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ImportSession> findAllByOrderByCreatedAtDesc();

    Optional<ImportSession> findByIdAndUserId(Long id, Long userId);
}
