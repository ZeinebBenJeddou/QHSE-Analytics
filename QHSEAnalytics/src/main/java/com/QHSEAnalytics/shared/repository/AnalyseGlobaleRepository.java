package com.QHSEAnalytics.shared.repository;

import com.QHSEAnalytics.shared.entity.AnalyseGlobale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AnalyseGlobaleRepository extends JpaRepository<AnalyseGlobale, Long> {


    @EntityGraph(attributePaths = {"importSession", "user"})
    Optional<AnalyseGlobale> findByImportSessionId(Long importSessionId);

    @EntityGraph(attributePaths = {"importSession", "user"})
    java.util.List<AnalyseGlobale> findTop10ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"importSession"})
    java.util.List<AnalyseGlobale> findByImportSessionIdIn(java.util.Collection<Long> importSessionIds);

    @Modifying
    @Transactional
    void deleteByImportSessionId(Long importSessionId);
}