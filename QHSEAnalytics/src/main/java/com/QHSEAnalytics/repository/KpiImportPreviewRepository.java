package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.KpiImportPreview;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KpiImportPreviewRepository extends JpaRepository<KpiImportPreview, Long> {

    List<KpiImportPreview> findByImportSessionIdOrderByIdAsc(Long importSessionId);

    boolean existsByImportSessionId(Long importSessionId);

    void deleteByImportSessionId(Long importSessionId);

    long countByImportSessionIdIn(List<Long> importSessionIds);

    @Modifying
    @Query("delete from KpiImportPreview p where p.importSession.id in :importSessionIds")
    int deleteByImportSessionIds(@Param("importSessionIds") List<Long> importSessionIds);
}
