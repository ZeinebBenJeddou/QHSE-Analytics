package com.QHSEAnalytics.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

class KpiSchemaUpdaterTest {

    @Test
    void shouldAddMissingKpiColumnsWhenTableExists() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(
                        eq("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?"),
                        eq(Integer.class),
                        eq("kpi")))
                .thenReturn(1);
        Mockito.when(jdbcTemplate.queryForObject(
                        eq("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?"),
                        eq(Integer.class),
                        eq("kpi"),
                        eq("direction")))
                .thenReturn(0);
        Mockito.when(jdbcTemplate.queryForObject(
                        eq("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?"),
                        eq(Integer.class),
                        eq("kpi"),
                        eq("target_value")))
                .thenReturn(0);

        KpiSchemaUpdater updater = new KpiSchemaUpdater(jdbcTemplate);
        updater.initializeKpiSchema();

        Mockito.verify(jdbcTemplate, times(1)).execute("ALTER TABLE kpi ADD COLUMN IF NOT EXISTS direction VARCHAR(50)");
        Mockito.verify(jdbcTemplate, times(1)).execute("ALTER TABLE kpi ADD COLUMN IF NOT EXISTS target_value DOUBLE PRECISION");
    }

    @Test
    void shouldSkipColumnAdditionWhenColumnsAlreadyExist() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(
                        eq("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?"),
                        eq(Integer.class),
                        eq("kpi")))
                .thenReturn(1);
        Mockito.when(jdbcTemplate.queryForObject(
                        eq("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?"),
                        eq(Integer.class),
                        eq("kpi"),
                        eq("direction")))
                .thenReturn(1);
        Mockito.when(jdbcTemplate.queryForObject(
                        eq("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?"),
                        eq(Integer.class),
                        eq("kpi"),
                        eq("target_value")))
                .thenReturn(1);

        KpiSchemaUpdater updater = new KpiSchemaUpdater(jdbcTemplate);
        updater.initializeKpiSchema();

        Mockito.verify(jdbcTemplate, times(0)).execute("ALTER TABLE kpi ADD COLUMN IF NOT EXISTS direction VARCHAR(50)");
        Mockito.verify(jdbcTemplate, times(0)).execute("ALTER TABLE kpi ADD COLUMN IF NOT EXISTS target_value DOUBLE PRECISION");
    }

    @Test
    void shouldSkipWhenKpiTableIsMissing() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(
                        eq("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?"),
                        eq(Integer.class),
                        eq("kpi")))
                .thenReturn(0);

        KpiSchemaUpdater updater = new KpiSchemaUpdater(jdbcTemplate);
        updater.initializeKpiSchema();

        Mockito.verify(jdbcTemplate, times(0)).execute(any(String.class));
    }
}
