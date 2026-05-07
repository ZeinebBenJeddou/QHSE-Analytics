package com.QHSEAnalytics.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KpiSchemaUpdater {

    private static final String KPI_TABLE = "kpi";
    private static final String DIRECTION_COLUMN = "direction";
    private static final String TARGET_VALUE_COLUMN = "target_value";

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeKpiSchema() {
        try {
            if (!isKpiTablePresent()) {
                log.debug("Skipping KPI schema upgrade because table '{}' does not exist yet.", KPI_TABLE);
                return;
            }

            addColumnIfMissing(DIRECTION_COLUMN, "VARCHAR(50)");
            addColumnIfMissing(TARGET_VALUE_COLUMN, "DOUBLE PRECISION");
        } catch (Exception ex) {
            log.warn("Unable to initialize KPI schema automatically: {}", ex.getMessage());
            log.debug("KPI schema initialization failure details", ex);
        }
    }

    private void addColumnIfMissing(String columnName, String columnType) {
        if (isColumnPresent(columnName)) {
            log.debug("KPI table column '{}' already exists.", columnName);
            return;
        }

        String sql = String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s %s", KPI_TABLE, columnName, columnType);
        jdbcTemplate.execute(sql);
        log.info("Added missing column '{}' to table '{}'.", columnName, KPI_TABLE);
    }

    private boolean isKpiTablePresent() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = current_schema() AND table_name = ?",
                Integer.class,
                KPI_TABLE
        );
        return tableCount != null && tableCount > 0;
    }

    private boolean isColumnPresent(String columnName) {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
                Integer.class,
                KPI_TABLE,
                columnName
        );
        return columnCount != null && columnCount > 0;
    }
}
