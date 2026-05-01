-- Flyway migration: align import_sessions.statut constraint with ImportStatut enum
ALTER TABLE import_sessions DROP CONSTRAINT IF EXISTS import_sessions_statut_check;

ALTER TABLE import_sessions ADD CONSTRAINT import_sessions_statut_check
  CHECK (statut IN (
    'EN_ATTENTE',
    'EN_TRAITEMENT',
    'IMPORTED',
    'CALCULATED',
    'READY_FOR_AI',
    'TRAITE',
    'ERREUR'
  ));
