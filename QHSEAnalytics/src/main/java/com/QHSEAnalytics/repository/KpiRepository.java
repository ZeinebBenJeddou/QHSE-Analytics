package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.CategorieKpi;
import com.QHSEAnalytics.entity.Kpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KpiRepository extends JpaRepository<Kpi, Long> {

    List<Kpi> findByIsActiveTrueOrderByOrdreAsc();

    List<Kpi> findByCategorieKpiCodeAndIsActiveTrueOrderByOrdreAsc(String code);

    List<Kpi> findByIsActiveFalse();

    boolean existsByNomAndCategorieKpi(String nom, CategorieKpi categorie);

    boolean existsByNomAndCategorieKpiAndIdNot(String nom, CategorieKpi categorie, Long id);

    int countByCategorieKpiAndIsActiveTrue(CategorieKpi categorieKpi);

    default boolean hasLinkedData(Long kpiId) {
        return false;
    }
}
