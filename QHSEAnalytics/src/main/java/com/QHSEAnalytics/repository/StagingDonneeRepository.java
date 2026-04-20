package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.StagingDonnee;
import com.QHSEAnalytics.enums.StatutNettoyage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StagingDonneeRepository extends JpaRepository<StagingDonnee, Long> {

    @EntityGraph(attributePaths = {"kpi", "kpi.categorieKpi", "importSession"})
    List<StagingDonnee> findByImportSessionId(Long sessionId);

    @EntityGraph(attributePaths = {"kpi", "kpi.categorieKpi", "importSession"})
    List<StagingDonnee> findByImportSessionIdAndStatutNettoyageIn(Long sessionId, List<StatutNettoyage> statuts);

    @Modifying
    @Transactional
    void deleteByImportSessionId(Long sessionId);

    long countByImportSessionIdAndStatutNettoyage(Long sessionId, StatutNettoyage statut);


    @Query("select s.statutNettoyage as statutNettoyage, count(s) as total from StagingDonnee s where s.importSession.id = :sessionId group by s.statutNettoyage")
    List<StatutNettoyageCountView> countByImportSessionIdGrouped(@Param("sessionId") Long sessionId);

    interface StatutNettoyageCountView {
        StatutNettoyage getStatutNettoyage();

        Long getTotal();
    }
}
