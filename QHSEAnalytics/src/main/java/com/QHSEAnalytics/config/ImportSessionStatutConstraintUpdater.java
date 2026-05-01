package com.QHSEAnalytics.config;

import com.QHSEAnalytics.enums.ImportStatut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class ImportSessionStatutConstraintUpdater {

    private static final String STATUT_CONSTRAINT_NAME = "import_sessions_statut_check";
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeConstraint() {
        try {
            if (!isImportSessionsTablePresent()) {
                log.debug("Skipping import_sessions statut constraint update because table does not exist yet.");
                return;
            }

            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                    "SELECT conname, pg_get_constraintdef(oid) AS definition " +
                    "FROM pg_constraint " +
                    "WHERE conrelid = 'import_sessions'::regclass AND contype = 'c'"
            );

            String values = ImportStatut.valuesForCheckConstraint();
            if (values.isBlank()) {
                throw new IllegalStateException("ImportStatut enum contains no values to populate the DB constraint.");
            }

            String desiredDefinition = "CHECK (statut IN (" + values + "))";
            boolean constraintUpToDate = constraints.stream()
                    .map(constraint -> (String) constraint.get("definition"))
                    .filter(definition -> definition != null)
                    .anyMatch(definition -> definition.trim().equalsIgnoreCase(desiredDefinition));

            if (constraintUpToDate) {
                log.info("import_sessions.statut constraint is already up to date.");
                return;
            }

            for (Map<String, Object> constraint : constraints) {
                String definition = (String) constraint.get("definition");
                String name = (String) constraint.get("conname");
                if (definition != null && definition.contains("statut")) {
                    log.info("Dropping outdated import_sessions statut constraint {}: {}", name, definition);
                    jdbcTemplate.execute("ALTER TABLE import_sessions DROP CONSTRAINT IF EXISTS \"" + name + "\"");
                }
            }

            String sql = "ALTER TABLE import_sessions ADD CONSTRAINT " + STATUT_CONSTRAINT_NAME + " " + desiredDefinition;
            jdbcTemplate.execute(sql);
            log.info("import_sessions.statut constraint updated successfully with values: {}", values);
        } catch (Exception ex) {
            log.warn("Unable to update import_sessions.statut constraint automatically: {}", ex.getMessage());
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
