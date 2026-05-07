package com.QHSEAnalytics.shared.repository;

import com.QHSEAnalytics.shared.entity.RagKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RagKnowledgeRepository extends JpaRepository<RagKnowledge, Long> {

    Optional<RagKnowledge> findByKpiName(String kpiName);

    List<RagKnowledge> findByCategory(String category);

    List<RagKnowledge> findAll();
}