package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.CategorieKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategorieKpiRepository extends JpaRepository<CategorieKpi, Long> {
    Optional<CategorieKpi> findByCode(String code);
    boolean existsByCode(String code);
}
