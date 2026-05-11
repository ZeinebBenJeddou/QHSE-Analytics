

ALTER TABLE categorie_kpi
    ALTER COLUMN code TYPE VARCHAR(10);

ALTER TABLE categorie_kpi
    ADD CONSTRAINT ck_categorie_kpi_code
    CHECK (code ~ '^[A-Z][A-Z0-9]{0,9}$');
