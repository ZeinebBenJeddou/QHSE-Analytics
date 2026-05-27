package com.QHSEAnalytics.auth.repository;

import com.QHSEAnalytics.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:action     IS NULL OR a.action     = :action)
          AND (:adminEmail IS NULL OR LOWER(a.adminEmail) LIKE LOWER(CONCAT('%', :adminEmail, '%')))
          AND (:dateDebut  IS NULL OR a.timestamp >= :dateDebut)
          AND (:dateFin    IS NULL OR a.timestamp <= :dateFin)
        ORDER BY a.timestamp DESC
        """)
    Page<AuditLog> findFiltered(
        @Param("action")     String action,
        @Param("adminEmail") String adminEmail,
        @Param("dateDebut")  LocalDateTime dateDebut,
        @Param("dateFin")    LocalDateTime dateFin,
        Pageable pageable
    );
}
