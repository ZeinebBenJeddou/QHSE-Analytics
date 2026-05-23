package com.QHSEAnalytics.auth.service;

import com.QHSEAnalytics.auth.dto.response.AuditLogResponse;
import com.QHSEAnalytics.auth.entity.AuditLog;
import com.QHSEAnalytics.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    public static final String CREATE_USER       = "CREER_ANALYSTE";
    public static final String UPDATE_USER       = "MODIFIER_UTILISATEUR";
    public static final String DELETE_USER       = "SUPPRIMER_UTILISATEUR";
    public static final String VERIFY_USER       = "VERIFIER_COMPTE";
    public static final String ACTIVATE_USER     = "ACTIVER_COMPTE";
    public static final String DEACTIVATE_USER   = "DESACTIVER_COMPTE";
    public static final String PROMOTE_ADMIN     = "PROMOUVOIR_ADMIN";
    public static final String DEMOTE_ANALYSTE   = "RETROGRADER_ANALYSTE";
    public static final String RESET_PASSWORD    = "REINITIALISER_MDP";

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public void log(String action, Long targetUserId, String targetEmail, String details) {
        String adminEmail = securityUtils.getCurrentUserEmail();
        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action(action)
                .targetUserId(targetUserId)
                .targetEmail(targetEmail)
                .details(details)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .adminEmail(a.getAdminEmail())
                .action(a.getAction())
                .targetUserId(a.getTargetUserId())
                .targetEmail(a.getTargetEmail())
                .details(a.getDetails())
                .timestamp(a.getTimestamp())
                .build();
    }
}
