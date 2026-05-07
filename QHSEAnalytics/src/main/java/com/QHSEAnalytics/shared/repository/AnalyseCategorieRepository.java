package com.QHSEAnalytics.shared.repository;

import com.QHSEAnalytics.shared.entity.AnalyseCategorie;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AnalyseCategorieRepository extends JpaRepository<AnalyseCategorie, Long> {


    @EntityGraph(attributePaths = {"importSession", "user"})
    List<AnalyseCategorie> findByImportSessionId(Long importSessionId);


    @EntityGraph(attributePaths = {"importSession", "user"})
    Optional<AnalyseCategorie> findByImportSessionIdAndCategorieCode(Long importSessionId, String categorieCode);

    @Modifying
    @Transactional
    void deleteByImportSessionId(Long importSessionId);
}