package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.ImportSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ImportSessionRepository extends JpaRepository<ImportSession, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<ImportSession> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<ImportSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ImportSession> findAllByOrderByCreatedAtDesc();

    Optional<ImportSession> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}
