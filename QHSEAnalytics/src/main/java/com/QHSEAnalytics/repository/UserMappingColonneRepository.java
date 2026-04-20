package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.UserMappingColonne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserMappingColonneRepository extends JpaRepository<UserMappingColonne, Long> {

    List<UserMappingColonne> findByMappingTemplateId(Long templateId);

    @Modifying
    @Transactional
    void deleteByMappingTemplateId(Long templateId);
}
