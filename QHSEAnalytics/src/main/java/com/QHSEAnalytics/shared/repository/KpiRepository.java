package com.QHSEAnalytics.shared.repository;

import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KpiRepository extends JpaRepository<Kpi, Long> {

    @EntityGraph(attributePaths = {"categorieKpi"})
    List<Kpi> findByIsActiveTrueOrderByNomAsc();

    Page<Kpi> findByIsActiveTrueOrderByNomAsc(Pageable pageable);

    List<Kpi> findByCategorieKpiCodeAndIsActiveTrueOrderByNomAsc(String code);

    Page<Kpi> findByCategorieKpiCodeAndIsActiveTrueOrderByNomAsc(String code, Pageable pageable);

    Optional<Kpi> findByNomIgnoreCase(String nom);
    Optional<Kpi> findByNom(String nom);

    List<Kpi> findAllByOrderByNomAsc();

    List<Kpi> findByIsActiveFalse();

    boolean existsByNomAndCategorieKpi(String nom, CategorieKpi categorie);

    Optional<Kpi> findByNomAndCategorieKpi(String nom, CategorieKpi categorie);

    boolean existsByNomAndCategorieKpiAndIdNot(String nom, CategorieKpi categorie, Long id);

    int countByCategorieKpiAndIsActiveTrue(CategorieKpi categorieKpi);

    @EntityGraph(attributePaths = {"categorieKpi"})
    Optional<Kpi> findById(Long id);

    @Query("select count(r) > 0 from ResultatKpi r where r.kpi.id = :kpiId")
    boolean hasLinkedData(@Param("kpiId") Long kpiId);
}
