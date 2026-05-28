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

    @Query(value = """
        SELECT * FROM admin_audit_log a
        WHERE (:action     IS NULL OR a.action      = :action)
          AND (:adminEmail IS NULL OR LOWER(a.admin_email) LIKE LOWER(CONCAT('%', CAST(:adminEmail AS text), '%')))
          AND (:dateDebut  IS NULL OR a.timestamp  >= CAST(:dateDebut AS timestamp))
          AND (:dateFin    IS NULL OR a.timestamp  <= CAST(:dateFin   AS timestamp))
        ORDER BY a.timestamp DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM admin_audit_log a
        WHERE (:action     IS NULL OR a.action      = :action)
          AND (:adminEmail IS NULL OR LOWER(a.admin_email) LIKE LOWER(CONCAT('%', CAST(:adminEmail AS text), '%')))
          AND (:dateDebut  IS NULL OR a.timestamp  >= CAST(:dateDebut AS timestamp))
          AND (:dateFin    IS NULL OR a.timestamp  <= CAST(:dateFin   AS timestamp))
        """,
        nativeQuery = true)
    Page<AuditLog> findFiltered(
        @Param("action")     String action,
        @Param("adminEmail") String adminEmail,
        @Param("dateDebut")  LocalDateTime dateDebut,
        @Param("dateFin")    LocalDateTime dateFin,
        Pageable pageable
    );
}
