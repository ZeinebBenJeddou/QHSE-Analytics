package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.UserMappingColonne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMappingColonneRepository extends JpaRepository<UserMappingColonne, Long> {

    List<UserMappingColonne> findByMappingTemplateId(Long templateId);

    void deleteByMappingTemplateId(Long templateId);
}
