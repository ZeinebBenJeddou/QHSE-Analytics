package com.QHSEAnalytics.shared.repository;

import com.QHSEAnalytics.shared.entity.MappingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MappingConfigRepository extends JpaRepository<MappingConfig, Long> {

    /** All mappings belonging to a user, ordered by template name */
    List<MappingConfig> findByUserIdOrderByTemplateNameAscCreatedAtDesc(Long userId);

    /** All rows for a specific template */
    List<MappingConfig> findByUserIdAndTemplateName(Long userId, String templateName);

    /** Distinct template names for a user */
    @Query("SELECT DISTINCT m.templateName FROM MappingConfig m WHERE m.userId = :userId ORDER BY m.templateName")
    List<String> findTemplateNamesByUserId(@Param("userId") Long userId);

    /** Delete all rows for a template */
    void deleteByUserIdAndTemplateName(Long userId, String templateName);

    /** Delete a specific template record */
    void deleteByIdAndUserId(Long id, Long userId);
}
