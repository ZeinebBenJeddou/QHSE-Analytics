package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.ImportSession;
import com.QHSEAnalytics.enums.ImportStatut;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImportSessionRepository extends JpaRepository<ImportSession, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<ImportSession> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<ImportSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ImportSession> findTopByUserIdAndStatutOrderByCreatedAtDesc(Long userId, ImportStatut statut);

    Optional<ImportSession> findTopByUserIdAndStatutInOrderByCreatedAtDesc(Long userId, List<ImportStatut> statuts);

    @EntityGraph(attributePaths = {"user"})
    List<ImportSession> findByStatutOrderByCreatedAtDesc(ImportStatut statut);

    @EntityGraph(attributePaths = {"user"})
    List<ImportSession> findByStatutInOrderByCreatedAtDesc(List<ImportStatut> statuts);

    List<ImportSession> findAllByOrderByCreatedAtDesc();

    long countByStatut(ImportStatut statut);

    long countByStatutIn(List<ImportStatut> statuts);

    Optional<ImportSession> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);

    @Query("select i.id from ImportSession i where i.createdAt < :cutoff and i.statut in :statuses")
    List<Long> findIdsByCreatedAtBeforeAndStatutIn(@Param("cutoff") LocalDateTime cutoff,
                                                   @Param("statuses") List<ImportStatut> statuses);
}
