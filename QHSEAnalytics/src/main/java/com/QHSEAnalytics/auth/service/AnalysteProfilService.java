package com.QHSEAnalytics.auth.service;

import com.QHSEAnalytics.auth.dto.AnalysteProfilDTO;
import com.QHSEAnalytics.auth.entity.AnalysteProfil;
import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.repository.AnalysteProfilRepository;
import com.QHSEAnalytics.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysteProfilService {

    private final AnalysteProfilRepository profilRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AnalysteProfilDTO getProfil(Long userId) {
        return profilRepository.findByUserId(userId)
                .map(this::toDTO)
                .orElse(new AnalysteProfilDTO());
    }

    @Transactional
    public AnalysteProfilDTO saveProfil(Long userId, AnalysteProfilDTO dto) {
        User user = userRepository.getReferenceById(userId);
        AnalysteProfil profil = profilRepository.findByUserId(userId)
                .orElse(AnalysteProfil.builder().user(user).build());

        profil.setSecteurActivite(dto.getSecteurActivite());
        profil.setTailleSite(dto.getTailleSite());
        profil.setCertifications(dto.getCertifications());
        profil.setObjectifsQhse(dto.getObjectifsQhse());
        profil.setReglementation(dto.getReglementation());
        profil.setContexteSpecifique(dto.getContexteSpecifique());

        return toDTO(profilRepository.save(profil));
    }

    public String buildContextBlock(Long userId) {
        AnalysteProfilDTO dto = getProfil(userId);

        StringBuilder sb = new StringBuilder();
        if (hasValue(dto.getSecteurActivite()))
            sb.append("Secteur d'activité : ").append(dto.getSecteurActivite()).append("\n");
        if (hasValue(dto.getTailleSite()))
            sb.append("Taille du site : ").append(dto.getTailleSite()).append("\n");
        if (hasValue(dto.getCertifications()))
            sb.append("Certifications : ").append(dto.getCertifications()).append("\n");
        if (hasValue(dto.getObjectifsQhse()))
            sb.append("Objectifs QHSE : ").append(dto.getObjectifsQhse()).append("\n");
        if (hasValue(dto.getReglementation()))
            sb.append("Réglementation : ").append(dto.getReglementation()).append("\n");
        if (hasValue(dto.getContexteSpecifique()))
            sb.append("Contexte spécifique : ").append(dto.getContexteSpecifique()).append("\n");

        if (sb.isEmpty()) return "";

        return "=== CONTEXTE DE L'ORGANISATION ===\n" + sb + "===================================";
    }

    private AnalysteProfilDTO toDTO(AnalysteProfil p) {
        return AnalysteProfilDTO.builder()
                .secteurActivite(p.getSecteurActivite())
                .tailleSite(p.getTailleSite())
                .certifications(p.getCertifications())
                .objectifsQhse(p.getObjectifsQhse())
                .reglementation(p.getReglementation())
                .contexteSpecifique(p.getContexteSpecifique())
                .build();
    }

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }
}
