package com.QHSEAnalytics.shared.repository;

import com.QHSEAnalytics.shared.entity.RagKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RagKnowledgeRepository extends JpaRepository<RagKnowledge, Long> {

    Optional<RagKnowledge> findByKpiName(String kpiName);

    Optional<RagKnowledge> findByKpiNameIgnoreCase(String kpiName);

    @Query("SELECT r FROM RagKnowledge r WHERE LOWER(r.kpiName) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<RagKnowledge> findByKpiNameContainingIgnoreCase(@Param("term") String term);

    List<RagKnowledge> findByCategory(String category);

    List<RagKnowledge> findAll();
}
