ALTER TABLE import_sessions
    ADD COLUMN contexte_secteur        VARCHAR(100),
    ADD COLUMN contexte_taille         VARCHAR(50),
    ADD COLUMN contexte_certifications VARCHAR(200),
    ADD COLUMN contexte_objectifs      VARCHAR(500),
    ADD COLUMN contexte_reglementation VARCHAR(200),
    ADD COLUMN contexte_specifique     VARCHAR(500);
