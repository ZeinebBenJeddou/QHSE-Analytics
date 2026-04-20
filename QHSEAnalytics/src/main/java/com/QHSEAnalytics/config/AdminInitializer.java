package com.QHSEAnalytics.config;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Value("${app.admin.nom}")
    private String nom;

    @Value("${app.admin.prenom}")
    private String prenom;

    @Override
    public void run(String... args) {
        try {
            if (!userRepository.existsByEmail(email)) {
                userRepository.save(User.builder()
                        .email(email)
                        .nom(nom)
                        .prenom(prenom)
                        .password(passwordEncoder.encode(password))
                        .role(User.Role.ADMIN)
                        .verified(true)
                        .build());
                log.info("Compte admin initial créé");
            }
        } catch (Exception ex) {

            log.error("Échec initialisation admin cause={}", ex.getMessage());
            throw ex;
        }
    }
}