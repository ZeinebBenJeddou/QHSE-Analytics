package com.QHSEAnalytics.service;

import com.QHSEAnalytics.auth.dto.response.MessageResponse;
import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.repository.EmailTokenRepository;
import com.QHSEAnalytics.auth.repository.OtpCodeRepository;
import com.QHSEAnalytics.auth.repository.RefreshTokenRepository;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.auth.service.AdminUserService;
import com.QHSEAnalytics.auth.service.AuditLogService;
import com.QHSEAnalytics.auth.service.EmailService;
import com.QHSEAnalytics.shared.entity.ImportSession;
import com.QHSEAnalytics.shared.enums.ImportMode;
import com.QHSEAnalytics.shared.enums.ImportStatut;
import com.QHSEAnalytics.shared.repository.AnalyseCategorieRepository;
import com.QHSEAnalytics.shared.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.shared.repository.ImportSessionRepository;
import com.QHSEAnalytics.shared.repository.KpiAnalysisRepository;
import com.QHSEAnalytics.shared.repository.KpiImportPreviewRepository;
import com.QHSEAnalytics.shared.repository.KpiRawDataRepository;
import com.QHSEAnalytics.shared.repository.ResultatKpiRepository;
import com.QHSEAnalytics.shared.repository.StagingDonneeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminUserServiceTest {

    @Test
    void deleteUserRemovesKpiAnalysisBeforeImportSessions() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        EmailService emailService = Mockito.mock(EmailService.class);
        AuditLogService auditLogService = Mockito.mock(AuditLogService.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        EmailTokenRepository emailTokenRepository = Mockito.mock(EmailTokenRepository.class);
        OtpCodeRepository otpCodeRepository = Mockito.mock(OtpCodeRepository.class);
        ImportSessionRepository importSessionRepository = Mockito.mock(ImportSessionRepository.class);
        StagingDonneeRepository stagingDonneeRepository = Mockito.mock(StagingDonneeRepository.class);
        ResultatKpiRepository resultatKpiRepository = Mockito.mock(ResultatKpiRepository.class);
        KpiAnalysisRepository kpiAnalysisRepository = Mockito.mock(KpiAnalysisRepository.class);
        KpiImportPreviewRepository kpiImportPreviewRepository = Mockito.mock(KpiImportPreviewRepository.class);
        KpiRawDataRepository kpiRawDataRepository = Mockito.mock(KpiRawDataRepository.class);
        AnalyseCategorieRepository analyseCategorieRepository = Mockito.mock(AnalyseCategorieRepository.class);
        AnalyseGlobaleRepository analyseGlobaleRepository = Mockito.mock(AnalyseGlobaleRepository.class);

        AdminUserService service = new AdminUserService(
                userRepository,
                Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                emailService,
                auditLogService,
                refreshTokenRepository,
                emailTokenRepository,
                otpCodeRepository,
                importSessionRepository,
                stagingDonneeRepository,
                resultatKpiRepository,
                kpiAnalysisRepository,
                kpiImportPreviewRepository,
                kpiRawDataRepository,
                analyseCategorieRepository,
                analyseGlobaleRepository
        );
        ReflectionTestUtils.setField(service, "adminEmail", "admin@qhse.test");

        User user = User.builder()
                .id(5L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@qhse.test")
                .role(User.Role.ANALYSTE)
                .active(true)
                .verified(true)
                .build();

        ImportSession session = ImportSession.builder()
                .id(12L)
                .user(user)
                .mode(ImportMode.MANUAL)
                .nomFichier("report.xlsx")
                .periodeN1(2024)
                .periodeN(2025)
                .statut(ImportStatut.TRAITE)
                .build();

        Mockito.when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        Mockito.when(importSessionRepository.findByUserId(5L)).thenReturn(List.of(session));

        MessageResponse response = service.deleteUser(5L);

        assertEquals("Utilisateur supprimé avec succès.", response.getMessage());

        InOrder inOrder = Mockito.inOrder(
                importSessionRepository,
                kpiAnalysisRepository,
                analyseCategorieRepository,
                analyseGlobaleRepository,
                kpiImportPreviewRepository,
                kpiRawDataRepository,
                resultatKpiRepository,
                stagingDonneeRepository
        );
        inOrder.verify(importSessionRepository).findByUserId(5L);
        inOrder.verify(kpiAnalysisRepository).deleteByImportSessionId(12L);
        inOrder.verify(analyseCategorieRepository).deleteByImportSessionId(12L);
        inOrder.verify(analyseGlobaleRepository).deleteByImportSessionId(12L);
        inOrder.verify(kpiImportPreviewRepository).deleteByImportSessionId(12L);
        inOrder.verify(kpiRawDataRepository).deleteByImportSessionId(12L);
        inOrder.verify(resultatKpiRepository).deleteByImportSessionId(12L);
        inOrder.verify(stagingDonneeRepository).deleteByImportSessionId(12L);
        inOrder.verify(importSessionRepository).deleteByUserId(5L);

        Mockito.verify(emailTokenRepository).deleteByUserId(5L);
        Mockito.verify(otpCodeRepository).deleteByUserId(5L);
        Mockito.verify(refreshTokenRepository).deleteByUserId(5L);
        Mockito.verify(userRepository).delete(user);
    }
}
