package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.UserMappingTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserMappingTemplateRepository extends JpaRepository<UserMappingTemplate, Long> {

    @EntityGraph(attributePaths = {"colonnes", "colonnes.kpi"})
    List<UserMappingTemplate> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"colonnes", "colonnes.kpi"})
    Optional<UserMappingTemplate> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}
