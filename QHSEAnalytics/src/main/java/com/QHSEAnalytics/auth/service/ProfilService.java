package com.QHSEAnalytics.auth.service;

import com.QHSEAnalytics.auth.dto.request.ChangePasswordRequest;
import com.QHSEAnalytics.auth.dto.request.UpdateProfilRequest;
import com.QHSEAnalytics.auth.dto.response.MessageResponse;
import com.QHSEAnalytics.auth.dto.response.ProfilResponse;
import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.PasswordMismatchException;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.RefreshTokenRepository;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.exception.WrongPasswordException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfilService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public ProfilResponse getProfil(Long userId) {
        return toProfilResponse(getUserById(userId));
    }

    @Transactional
    public ProfilResponse updateProfil(Long userId, UpdateProfilRequest request) {
        User user = getUserById(userId);
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        return toProfilResponse(userRepository.save(user));
    }

    @Transactional
    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(request.getAncienPassword(), user.getPassword())) {
            throw new WrongPasswordException("L'ancien mot de passe est incorrect.");
        }

        if (!request.getNouveauPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Les mots de passe ne correspondent pas.");
        }

        user.setPassword(passwordEncoder.encode(request.getNouveauPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user);
        log.info("Mot de passe modifié pour userId={}", user.getId());
        return new MessageResponse("Mot de passe modifié avec succès. Veuillez vous reconnecter.");
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));
    }

    private ProfilResponse toProfilResponse(User user) {
        return ProfilResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .role(user.getRole() == null ? null : user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}