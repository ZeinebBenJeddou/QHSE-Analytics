package com.QHSEAnalytics.auth.service;


import com.QHSEAnalytics.auth.entity.OtpCode;
import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.repository.OtpCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final EmailService emailService;

    @Value("${app.otp.expiration-minutes}")
    private int expirationMinutes;

    private final SecureRandom random = new SecureRandom();

    // génère et envoie OTP
    @Transactional
    public void generateAndSendOtp(User user) {
        // invalider  anciens OTP
        otpCodeRepository.invalidateAllForUser(user);

        String code = String.format("%06d", random.nextInt(1_000_000));

        OtpCode otp = OtpCode.builder()
                .code(code)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .build();

        otpCodeRepository.save(otp);
        emailService.sendOtpEmail(user.getEmail(), user.getPrenom(), code);
    }

    // vérifie OTP
    @Transactional
    public boolean verifyOtp(User user, String code) {
        return otpCodeRepository
                .findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .filter(otp -> !otp.isExpired())
                .filter(otp -> otp.getCode().equals(code))
                .map(otp -> {
                    otp.setUsed(true);
                    otpCodeRepository.save(otp);
                    return true;
                })
                .orElse(false);
    }
}