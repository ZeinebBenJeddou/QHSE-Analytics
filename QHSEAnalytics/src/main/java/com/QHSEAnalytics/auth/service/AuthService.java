package com.QHSEAnalytics.auth.service;

import com.QHSEAnalytics.auth.dto.request.*;
import com.QHSEAnalytics.auth.dto.response.AuthResponse;
import com.QHSEAnalytics.auth.dto.response.MessageResponse;
import com.QHSEAnalytics.auth.entity.EmailToken;
import com.QHSEAnalytics.auth.entity.RefreshToken;
import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.*;
import com.QHSEAnalytics.auth.repository.EmailTokenRepository;
import com.QHSEAnalytics.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.email-token.expiration-hours}")
    private int emailTokenExpirationHours;

    @Value("${app.email-token.resend-cooldown-minutes}")
    private int resendCooldownMinutes;

    @Value("${app.password-reset.expiration-hours}")
    private int passwordResetExpirationHours;

    // REGISTER
    @Transactional
    public MessageResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé.");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Les mots de passe ne correspondent pas.");
        }

        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ANALYSTE)
                .verified(false)
                .build();

        userRepository.save(user);
        sendVerificationToken(user);

        return new MessageResponse("Inscription réussie. Vérifiez votre email pour activer votre compte.");
    }

    // VERIFY ACCOUNT
    @Transactional
    public MessageResponse verifyAccount(String token) {

        EmailToken emailToken = emailTokenRepository
                .findByTokenAndTypeAndUsedFalse(token, EmailToken.EmailTokenType.VERIFICATION)
                .orElseThrow(() -> new InvalidTokenException("Lien de vérification invalide."));

        if (emailToken.isExpired()) {
            throw new TokenExpiredException("Lien de vérification expiré.");
        }

        User user = emailToken.getUser();
        user.setVerified(true);
        userRepository.save(user);

        emailToken.setUsed(true);
        emailTokenRepository.save(emailToken);

        return new MessageResponse("Compte vérifié avec succès.");
    }

    // RESEND VERIFICATION
    @Transactional
    public MessageResponse resendVerification(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Aucun compte trouvé."));

        if (user.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compte déjà vérifié.");
        }

        emailTokenRepository
                .findTopByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(user, EmailToken.EmailTokenType.VERIFICATION)
                .ifPresent(t -> {
                    LocalDateTime cooldownEnd = t.getCreatedAt().plusMinutes(resendCooldownMinutes);
                    if (LocalDateTime.now().isBefore(cooldownEnd)) {
                        throw new TooManyRequestsException("Veuillez attendre avant de renvoyer.");
                    }
                });

        emailTokenRepository.invalidateAllForUser(user, EmailToken.EmailTokenType.VERIFICATION);
        sendVerificationToken(user);

        return new MessageResponse("Lien renvoyé.");
    }

    // ADMIN VERIFY
    @Transactional
    public MessageResponse adminVerifyUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        if (user.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Déjà vérifié.");
        }

        user.setVerified(true);
        userRepository.save(user);

        emailTokenRepository.invalidateAllForUser(user, EmailToken.EmailTokenType.VERIFICATION);

        return new MessageResponse("Compte vérifié par admin.");
    }

    // LOGIN
    @Transactional
    public MessageResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Email ou mot de passe incorrect."));

        if (!user.isVerified()) {
            throw new AccountNotVerifiedException("Compte non vérifié.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Email ou mot de passe incorrect.");
        }

        otpService.generateAndSendOtp(user);

        return new MessageResponse("OTP envoyé.");
    }

    // VERIFY OTP
    @Transactional
    public AuthResponse verifyOtpAndLogin(VerifyOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        boolean valid = otpService.verifyOtp(user, request.getCode());

        if (!valid) {
            throw new InvalidTokenException("OTP invalide.");
        }

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, request.isRememberMe());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .role(user.getRole().name())
                .build();
    }

    // RESEND OTP
    @Transactional
    public MessageResponse resendOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        if (!user.isVerified()) {
            throw new AccountNotVerifiedException("Compte non vérifié.");
        }

        otpService.generateAndSendOtp(user);

        return new MessageResponse("OTP renvoyé.");
    }

    // REFRESH TOKEN
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .role(user.getRole().name())
                .build();
    }

    // FORGOT PASSWORD
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            emailTokenRepository.invalidateAllForUser(user, EmailToken.EmailTokenType.PASSWORD_RESET);
            sendPasswordResetToken(user);
        });

        return new MessageResponse("Si email existe, lien envoyé.");
    }

    // RESET PASSWORD
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Mismatch password.");
        }

        EmailToken emailToken = emailTokenRepository
                .findByTokenAndTypeAndUsedFalse(request.getToken(), EmailToken.EmailTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidTokenException("Token invalide."));

        if (emailToken.isExpired()) {
            throw new TokenExpiredException("Token expiré.");
        }

        User user = emailToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        emailToken.setUsed(true);
        emailTokenRepository.save(emailToken);

        refreshTokenService.revokeAllForUser(user);

        return new MessageResponse("Mot de passe réinitialisé.");
    }

    // LOGOUT
    @Transactional
    public MessageResponse logout(String email) {

        userRepository.findByEmail(email)
                .ifPresent(refreshTokenService::revokeAllForUser);

        return new MessageResponse("Déconnexion réussie.");
    }

    // HELPERS

    private void sendVerificationToken(User user) {
        EmailToken token = EmailToken.builder()
                .user(user)
                .type(EmailToken.EmailTokenType.VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(emailTokenExpirationHours))
                .build();

        emailTokenRepository.save(token);
        emailService.sendVerificationEmail(user.getEmail(), user.getPrenom(), token.getToken());
    }

    private void sendPasswordResetToken(User user) {
        EmailToken token = EmailToken.builder()
                .user(user)
                .type(EmailToken.EmailTokenType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusHours(passwordResetExpirationHours))
                .build();

        emailTokenRepository.save(token);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getPrenom(), token.getToken());
    }
}