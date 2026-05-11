package com.QHSEAnalytics.shared.repository;

import com.QHSEAnalytics.shared.entity.KpiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface KpiAnalysisRepository extends JpaRepository<KpiAnalysis, Long> {

    List<KpiAnalysis> findByImportSessionIdOrderByIdAsc(Long importSessionId);

    Optional<KpiAnalysis> findByImportSessionIdAndKpiName(Long importSessionId, String kpiName);

    boolean existsByImportSessionId(Long importSessionId);

    @Modifying
    @Transactional
    void deleteByImportSessionId(Long importSessionId);
}
