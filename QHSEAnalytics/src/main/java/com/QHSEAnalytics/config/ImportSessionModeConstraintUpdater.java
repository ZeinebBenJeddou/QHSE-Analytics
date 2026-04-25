package com.QHSEAnalytics.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportSessionModeConstraintUpdater {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        try {
            if (!isImportSessionsTablePresent()) {
                log.debug("Skipping import_sessions mode constraint update because table does not exist yet.");
                return;
            }

            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                    "SELECT conname, pg_get_constraintdef(oid) AS definition " +
                    "FROM pg_constraint " +
                    "WHERE conrelid = 'import_sessions'::regclass AND contype = 'c'"
            );

            boolean hasAutoAllowed = constraints.stream()
                    .map(entry -> (String) entry.get("definition"))
                    .anyMatch(definition -> definition.contains("mode") && definition.contains("AUTO"));

            if (hasAutoAllowed) {
                log.info("import_sessions.mode constraint already allows AUTO.");
                return;
            }

            for (Map<String, Object> constraint : constraints) {
                String definition = (String) constraint.get("definition");
                String name = (String) constraint.get("conname");
                if (definition.contains("mode")) {
                    log.info("Dropping outdated import_sessions check constraint {}: {}", name, definition);
                    jdbcTemplate.execute("ALTER TABLE import_sessions DROP CONSTRAINT IF EXISTS \"" + name + "\"");
                }
            }

            jdbcTemplate.execute("ALTER TABLE import_sessions ADD CONSTRAINT import_sessions_mode_check " +
                    "CHECK (mode IN ('TEMPLATE_OFFICIEL', 'FICHIER_LIBRE', 'AUTO'))");
            log.info("import_sessions.mode constraint updated to include AUTO.");
        } catch (Exception ex) {
            log.warn("Unable to update import_sessions.mode constraint automatically: {}", ex.getMessage());
            log.debug("Constraint update failure details", ex);
        }
    }

    private boolean isImportSessionsTablePresent() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = current_schema() AND table_name = 'import_sessions'",
                Integer.class
        );
        return tableCount != null && tableCount > 0;
    }
}
