-- V4 : Élargir categorie_kpi.code de VARCHAR(1) à VARCHAR(10)
-- Permet des codes multimots jusqu'à 10 caractères (ex : "S", "EN", "HSE").
-- La contrainte CHECK force le format [A-Z][A-Z0-9]* pour éviter les codes invalides.

ALTER TABLE categorie_kpi
    ALTER COLUMN code TYPE VARCHAR(10);

ALTER TABLE categorie_kpi
    ADD CONSTRAINT ck_categorie_kpi_code
    CHECK (code ~ '^[A-Z][A-Z0-9]{0,9}$');
