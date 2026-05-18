-- Rendre nullable les champs de valeur/variation pour les KPIs absents d'un fichier (import dual)
ALTER TABLE resultat_kpis
    ALTER COLUMN valeur_n1 DROP NOT NULL,
    ALTER COLUMN valeur_n DROP NOT NULL,
    ALTER COLUMN variation_absolue DROP NOT NULL,
    ALTER COLUMN variation_relative DROP NOT NULL;
