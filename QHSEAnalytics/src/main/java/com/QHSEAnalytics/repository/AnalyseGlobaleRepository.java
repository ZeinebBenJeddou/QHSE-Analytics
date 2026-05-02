package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.AnalyseGlobale;
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

    @Modifying
    @Transactional
    void deleteByImportSessionId(Long importSessionId);
}