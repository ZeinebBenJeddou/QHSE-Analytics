package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.CategorieKpi;
import com.QHSEAnalytics.entity.Kpi;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KpiRepository extends JpaRepository<Kpi, Long> {

    List<Kpi> findByIsActiveTrueOrderByOrdreAsc();

    List<Kpi> findByCategorieKpiCodeAndIsActiveTrueOrderByOrdreAsc(String code);

    Optional<Kpi> findByNomIgnoreCase(String nom);

    List<Kpi> findAllByOrderByOrdreAsc();

    List<Kpi> findByIsActiveFalse();

    boolean existsByNomAndCategorieKpi(String nom, CategorieKpi categorie);

    boolean existsByNomAndCategorieKpiAndIdNot(String nom, CategorieKpi categorie, Long id);

    int countByCategorieKpiAndIsActiveTrue(CategorieKpi categorieKpi);

    @EntityGraph(attributePaths = {"categorieKpi"})
    Optional<Kpi> findById(Long id);

    default boolean hasLinkedData(Long kpiId) {
        return false;
    }
}
