package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.KpiRawData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KpiRawDataRepository extends JpaRepository<KpiRawData, Long> {
    void deleteByImportSessionId(Long importSessionId);

    long countByImportSessionIdIn(List<Long> importSessionIds);

    @Modifying
    @Query("delete from KpiRawData r where r.importSession.id in :importSessionIds")
    int deleteByImportSessionIds(@Param("importSessionIds") List<Long> importSessionIds);
}
