package com.QHSEAnalytics.shared.repository;

import com.QHSEAnalytics.shared.entity.MappingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MappingConfigRepository extends JpaRepository<MappingConfig, Long> {


    List<MappingConfig> findByUserIdOrderByTemplateNameAscCreatedAtDesc(Long userId);


    List<MappingConfig> findByUserIdAndTemplateName(Long userId, String templateName);


    @Query("SELECT DISTINCT m.templateName FROM MappingConfig m WHERE m.userId = :userId ORDER BY m.templateName")
    List<String> findTemplateNamesByUserId(@Param("userId") Long userId);


    @Modifying
    @Transactional
    void deleteByUserIdAndTemplateName(Long userId, String templateName);

    @Modifying
    @Transactional
    void deleteByIdAndUserId(Long id, Long userId);
}
