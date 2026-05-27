CREATE TABLE analyste_profil (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    secteur_activite VARCHAR(100),
    taille_site      VARCHAR(50),
    certifications   VARCHAR(200),
    objectifs_qhse   VARCHAR(500),
    reglementation   VARCHAR(200),
    contexte_specifique VARCHAR(500),
    created_at       TIMESTAMP    DEFAULT NOW(),
    updated_at       TIMESTAMP    DEFAULT NOW()
);
