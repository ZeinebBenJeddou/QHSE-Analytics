package com.QHSEAnalytics.auth.service;

import com.QHSEAnalytics.auth.dto.request.CreateAnalysteRequest;
import com.QHSEAnalytics.auth.dto.request.UpdateUserRequest;
import com.QHSEAnalytics.auth.dto.response.MessageResponse;
import com.QHSEAnalytics.auth.dto.response.UserListResponse;
import com.QHSEAnalytics.auth.dto.response.UserResponse;
import com.QHSEAnalytics.auth.entity.EmailToken;
import com.QHSEAnalytics.auth.entity.OtpCode;
import com.QHSEAnalytics.auth.entity.RefreshToken;
import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.EmailAlreadyExistsException;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.EmailTokenRepository;
import com.QHSEAnalytics.auth.repository.OtpCodeRepository;
import com.QHSEAnalytics.auth.repository.RefreshTokenRepository;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.entity.ImportSession;
import com.QHSEAnalytics.entity.ResultatKpi;
import com.QHSEAnalytics.entity.UserMappingColonne;
import com.QHSEAnalytics.entity.UserMappingTemplate;
import com.QHSEAnalytics.exception.AdminProtectedException;
import com.QHSEAnalytics.exception.UserAlreadyActiveException;
import com.QHSEAnalytics.exception.UserAlreadyAdminException;
import com.QHSEAnalytics.exception.UserAlreadyAnalysteException;
import com.QHSEAnalytics.exception.UserAlreadyInactiveException;
import com.QHSEAnalytics.repository.AnalyseCategorieRepository;
import com.QHSEAnalytics.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.repository.ImportSessionRepository;
import com.QHSEAnalytics.repository.ResultatKpiRepository;
import com.QHSEAnalytics.repository.StagingDonneeRepository;
import com.QHSEAnalytics.repository.UserMappingColonneRepository;
import com.QHSEAnalytics.repository.UserMappingTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "@$!%*?&";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final ImportSessionRepository importSessionRepository;
    private final StagingDonneeRepository stagingDonneeRepository;
    private final ResultatKpiRepository resultatKpiRepository;
    private final AnalyseCategorieRepository analyseCategorieRepository;
    private final AnalyseGlobaleRepository analyseGlobaleRepository;
    private final UserMappingTemplateRepository userMappingTemplateRepository;
    private final UserMappingColonneRepository userMappingColonneRepository;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Transactional(readOnly = true)
    public UserListResponse getAllUsers() {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        List<UserResponse> responses = users.stream().map(this::toUserResponse).toList();

        return UserListResponse.builder()
                .users(responses)
                .totalAdmins((int) userRepository.countByRole(User.Role.ADMIN))
                .totalAnalystes((int) userRepository.countByRole(User.Role.ANALYSTE))
                .totalActifs((int) userRepository.countByActiveTrue())
                .totalInactifs((int) userRepository.countByActiveFalse())
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toUserResponse(getUserByIdInternal(id));
    }

    @Transactional
    public UserResponse createAnalyste(CreateAnalysteRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé.");
        }

        String temporaryPassword = generateTemporaryPassword();
        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(temporaryPassword))
                .role(User.Role.ANALYSTE)
                .verified(true)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getPrenom(), temporaryPassword);
        log.info("Analyste créé par admin id={}", saved.getId());
        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = getUserByIdInternal(id);
        boolean emailChanged = !user.getEmail().equalsIgnoreCase(request.getEmail());

        if (isSystemAdmin(user) && emailChanged) {
            throw new AdminProtectedException("L'administrateur initial ne peut pas être modifié ou supprimé.");
        }

        if (emailChanged && userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé.");
        }

        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        if (emailChanged) {
            user.setEmail(request.getEmail());
        }

        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public MessageResponse deleteUser(Long id) {
        User user = getUserByIdInternal(id);
        checkNotSystemAdmin(user);

        List<ImportSession> sessions = importSessionRepository.findByUserId(id);
        for (ImportSession session : sessions) {
            Long sessionId = session.getId();
            analyseCategorieRepository.deleteByImportSessionId(sessionId);
            analyseGlobaleRepository.deleteByImportSessionId(sessionId);
            resultatKpiRepository.deleteByImportSessionId(sessionId);
            stagingDonneeRepository.deleteByImportSessionId(sessionId);
        }

        importSessionRepository.deleteByUserId(id);
        emailTokenRepository.deleteByUserId(id);
        otpCodeRepository.deleteByUserId(id);
        refreshTokenRepository.deleteByUserId(id);

        List<UserMappingTemplate> templates = userMappingTemplateRepository.findByUserId(id);
        for (UserMappingTemplate template : templates) {
            userMappingColonneRepository.deleteByMappingTemplateId(template.getId());
        }
        userMappingTemplateRepository.deleteByUserId(id);
        userRepository.delete(user);

        log.info("Utilisateur supprimé avec toutes ses données id={}", user.getId());
        return new MessageResponse("Utilisateur supprimé avec succès.");
    }

    @Transactional
    public UserResponse verifyUser(Long id) {
        User user = getUserByIdInternal(id);

        if (user.isVerified()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Compte déjà vérifié.");
        }

        user.setVerified(true);
        emailTokenRepository.invalidateAllForUser(user, EmailToken.EmailTokenType.VERIFICATION);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse activateUser(Long id) {
        User user = getUserByIdInternal(id);
        checkNotSystemAdmin(user);

        if (user.isActive()) {
            throw new UserAlreadyActiveException("Ce compte est déjà actif.");
        }

        user.setActive(true);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse deactivateUser(Long id) {
        User user = getUserByIdInternal(id);
        checkNotSystemAdmin(user);

        if (!user.isActive()) {
            throw new UserAlreadyInactiveException("Ce compte est déjà désactivé.");
        }

        user.setActive(false);
        refreshTokenRepository.revokeAllForUser(user);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse promoteToAdmin(Long id) {
        User user = getUserByIdInternal(id);
        checkNotSystemAdmin(user);

        if (user.getRole() == User.Role.ADMIN) {
            throw new UserAlreadyAdminException("Cet utilisateur est déjà administrateur.");
        }

        user.setRole(User.Role.ADMIN);
        refreshTokenRepository.revokeAllForUser(user);
        log.info("Utilisateur promu ADMIN id={}", user.getId());
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse demoteToAnalyste(Long id) {
        User user = getUserByIdInternal(id);
        checkNotSystemAdmin(user);

        if (user.getRole() == User.Role.ANALYSTE) {
            throw new UserAlreadyAnalysteException("Cet utilisateur est déjà analyste.");
        }

        user.setRole(User.Role.ANALYSTE);
        refreshTokenRepository.revokeAllForUser(user);
        log.info("Utilisateur rétrogradé ANALYSTE id={}", user.getId());
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public MessageResponse adminResetPassword(Long id) {
        User user = getUserByIdInternal(id);
        checkNotSystemAdmin(user);

        emailTokenRepository.invalidateAllForUser(user, EmailToken.EmailTokenType.PASSWORD_RESET);
        EmailToken token = EmailToken.builder()
                .user(user)
                .type(EmailToken.EmailTokenType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        emailTokenRepository.save(token);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getPrenom(), token.getToken());
        log.info("Reset mdp déclenché par admin id={}", user.getId());
        return new MessageResponse("Un email de réinitialisation a été envoyé.");
    }

    private User getUserByIdInternal(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));
    }

    private boolean isSystemAdmin(User user) {
        return user.getEmail() != null && user.getEmail().equalsIgnoreCase(adminEmail);
    }

    private void checkNotSystemAdmin(User user) {
        if (isSystemAdmin(user)) {
            throw new AdminProtectedException("L'administrateur initial ne peut pas être modifié ou supprimé.");
        }
    }

    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        List<Character> chars = new ArrayList<>();
        chars.add(randomChar(random, UPPER));
        chars.add(randomChar(random, LOWER));
        chars.add(randomChar(random, DIGITS));
        chars.add(randomChar(random, SPECIAL));

        String all = UPPER + LOWER + DIGITS + SPECIAL;
        while (chars.size() < 10) {
            chars.add(randomChar(random, all));
        }

        Collections.shuffle(chars, random);
        StringBuilder password = new StringBuilder();
        for (Character character : chars) {
            password.append(character);
        }
        return password.toString();
    }

    private char randomChar(SecureRandom random, String source) {
        return source.charAt(random.nextInt(source.length()));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .role(user.getRole() == null ? null : user.getRole().name())
                .verified(user.isVerified())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .systemAdmin(isSystemAdmin(user))
                .build();
    }
}