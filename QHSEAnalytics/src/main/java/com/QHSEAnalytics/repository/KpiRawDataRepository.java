package com.QHSEAnalytics.repository;

import com.QHSEAnalytics.entity.KpiRawData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiRawDataRepository extends JpaRepository<KpiRawData, Long> {
}
