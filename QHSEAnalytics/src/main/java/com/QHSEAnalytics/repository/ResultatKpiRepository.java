package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.ResultatKpi;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResultatKpiRepository extends JpaRepository<ResultatKpi, Long> {

    @EntityGraph(attributePaths = {"kpi", "kpi.categorieKpi", "user", "importSession"})
    List<ResultatKpi> findByImportSessionId(Long sessionId);

    @EntityGraph(attributePaths = {"kpi", "kpi.categorieKpi", "user", "importSession"})
    List<ResultatKpi> findByImportSessionIdOrderByCreatedAtDesc(Long sessionId);

    @EntityGraph(attributePaths = {"kpi", "kpi.categorieKpi", "user", "importSession"})
    List<ResultatKpi> findByUserId(Long userId);


    @EntityGraph(attributePaths = {"kpi", "kpi.categorieKpi", "user", "importSession"})
    @Query("select r from ResultatKpi r order by r.createdAt desc")
    List<ResultatKpi> findAllWithDetails();

    @EntityGraph(attributePaths = {"kpi", "kpi.categorieKpi", "user", "importSession"})
    List<ResultatKpi> findAllByOrderByCreatedAtDesc();

    boolean existsByImportSessionId(Long sessionId);

    void deleteByImportSessionId(Long sessionId);
}
