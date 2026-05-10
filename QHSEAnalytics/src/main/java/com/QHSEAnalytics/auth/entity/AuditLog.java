package com.QHSEAnalytics.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_log", indexes = {
        @Index(name = "idx_audit_timestamp", columnList = "timestamp DESC"),
        @Index(name = "idx_audit_admin",     columnList = "admin_email")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_email")
    private String targetEmail;

    @Column(length = 512)
    private String details;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
