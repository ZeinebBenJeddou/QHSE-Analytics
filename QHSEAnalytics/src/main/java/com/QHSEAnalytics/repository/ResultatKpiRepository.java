package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.ResultatKpi;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Query("select r from ResultatKpi r join fetch r.kpi k join fetch k.categorieKpi where r.importSession.id = :importSessionId")
    List<ResultatKpi> findByImportSessionIdWithKpi(@Param("importSessionId") Long importSessionId);

    long countByImportSessionIdAndNiveauVariation(Long importSessionId, com.QHSEAnalytics.enums.NiveauVariation niveau);

    @Query("select r.importSession.id as importSessionId, count(r) as total from ResultatKpi r where r.importSession.id in :importSessionIds and r.niveauVariation = :niveau group by r.importSession.id")
    List<ImportSessionCountView> countByImportSessionIdsAndNiveauVariation(@Param("importSessionIds") List<Long> importSessionIds, @Param("niveau") com.QHSEAnalytics.enums.NiveauVariation niveau);

    long countByNiveauVariation(com.QHSEAnalytics.enums.NiveauVariation niveau);

    @Query("select r from ResultatKpi r join fetch r.kpi k join fetch k.categorieKpi where r.niveauVariation = :niveau")
    List<ResultatKpi> findByNiveauVariationWithKpi(@Param("niveau") com.QHSEAnalytics.enums.NiveauVariation niveau);

    boolean existsByImportSessionId(Long sessionId);

    @Modifying
    @Transactional
    void deleteByImportSessionId(Long sessionId);

    interface ImportSessionCountView {
        Long getImportSessionId();

        Long getTotal();
    }
}
