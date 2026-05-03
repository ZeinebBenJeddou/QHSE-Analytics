package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.KpiImportPreview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KpiImportPreviewRepository extends JpaRepository<KpiImportPreview, Long> {

    List<KpiImportPreview> findByImportSessionIdOrderByIdAsc(Long importSessionId);

    boolean existsByImportSessionId(Long importSessionId);

    void deleteByImportSessionId(Long importSessionId);
}
