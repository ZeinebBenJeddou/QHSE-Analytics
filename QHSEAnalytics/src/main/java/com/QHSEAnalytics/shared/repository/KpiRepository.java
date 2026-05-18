package com.QHSEAnalytics.shared.repository;

import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KpiRepository extends JpaRepository<Kpi, Long> {

    @EntityGraph(attributePaths = {"categorieKpi"})
    List<Kpi> findByIsActiveTrueOrderByOrdreAsc();

    Page<Kpi> findByIsActiveTrueOrderByOrdreAsc(Pageable pageable);

    List<Kpi> findByCategorieKpiCodeAndIsActiveTrueOrderByOrdreAsc(String code);

    Page<Kpi> findByCategorieKpiCodeAndIsActiveTrueOrderByOrdreAsc(String code, Pageable pageable);

    Optional<Kpi> findByNomIgnoreCase(String nom);
    Optional<Kpi> findByNom(String nom);

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
