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

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    public static final String CREATE_KPI        = "CREER_KPI";
    public static final String UPDATE_KPI        = "MODIFIER_KPI";
    public static final String DELETE_KPI        = "SUPPRIMER_KPI";
    public static final String DEACTIVATE_KPI    = "DESACTIVER_KPI";
    public static final String REACTIVATE_KPI    = "REACTIVER_KPI";
    public static final String RESTORE_KPI       = "RESTAURER_KPI";

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
                .targetLabel(targetEmail)
                .details(details)
                .build());
    }

    @Transactional
    public void logKpi(String action, Long targetKpiId, String kpiName, String details) {
        String adminEmail = securityUtils.getCurrentUserEmail();
        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action(action)
                .targetLabel(kpiName == null || kpiName.isBlank() ? "KPI" : "KPI: " + kpiName)
                .details(details)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getFiltered(
            String action,
            String adminEmail,
            LocalDate dateDebut,
            LocalDate dateFin,
            Pageable pageable) {

        // Normalise : chaînes vides → null pour le JPQL (:x IS NULL)
        String actionParam     = (action     != null && !action.isBlank())     ? action.trim()     : null;
        String adminEmailParam = (adminEmail != null && !adminEmail.isBlank()) ? adminEmail.trim() : null;
        LocalDateTime debut = dateDebut != null ? dateDebut.atStartOfDay()              : null;
        LocalDateTime fin   = dateFin   != null ? dateFin.atTime(23, 59, 59)            : null;

        return auditLogRepository
                .findFiltered(actionParam, adminEmailParam, debut, fin, pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .adminEmail(a.getAdminEmail())
                .action(a.getAction())
                .targetUserId(a.getTargetUserId())
                .targetEmail(a.getTargetEmail())
                .targetLabel(a.getTargetLabel())
                .details(a.getDetails())
                .timestamp(a.getTimestamp())
                .build();
    }
}
