package com.QHSEAnalytics.shared.repository;

import com.QHSEAnalytics.shared.entity.RagKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RagKnowledgeRepository extends JpaRepository<RagKnowledge, Long> {

    Optional<RagKnowledge> findByKpiNameAndChunkType(String kpiName, String chunkType);

    @Query("""
        SELECT r FROM RagKnowledge r
        WHERE LOWER(r.kpiName) = LOWER(:kpiName)
        ORDER BY CASE WHEN LOWER(COALESCE(r.chunkType, '')) = 'full' THEN 0 ELSE 1 END, r.id ASC
        """)
    List<RagKnowledge> findAllByKpiNameIgnoreCaseOrderByPreferredChunk(@Param("kpiName") String kpiName);

    @Query("""
        SELECT r FROM RagKnowledge r
        WHERE LOWER(r.kpiName) LIKE LOWER(CONCAT('%', :term, '%'))
        ORDER BY CASE WHEN LOWER(COALESCE(r.chunkType, '')) = 'full' THEN 0 ELSE 1 END, r.id ASC
        """)
    List<RagKnowledge> findByKpiNameContainingIgnoreCase(@Param("term") String term);

    List<RagKnowledge> findByKpiNameIn(List<String> kpiNames);

    List<RagKnowledge> findByCategory(String category);

    List<RagKnowledge> findAllByKpiName(String kpiName);

    @Modifying
    @Query("DELETE FROM RagKnowledge r WHERE r.kpiName = :kpiName")
    void deleteAllByKpiName(@Param("kpiName") String kpiName);

    List<RagKnowledge> findAll();

    default Optional<RagKnowledge> findBestByKpiName(String kpiName) {
        return findAllByKpiNameIgnoreCaseOrderByPreferredChunk(kpiName).stream().findFirst();
    }
}
