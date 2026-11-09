CREATE TABLE contrat (
    id               BIGSERIAL PRIMARY KEY,
    numero           VARCHAR(20)     NOT NULL UNIQUE,
    dossier_id       BIGINT          NOT NULL REFERENCES dossier_souscription(id),
    statut           VARCHAR(30)     NOT NULL DEFAULT 'EN_ATTENTE_DE_SIGNATURE',
    adresse_logement VARCHAR(255)    NOT NULL,
    code_postal      VARCHAR(5)      NOT NULL,
    ville            VARCHAR(100)    NOT NULL,
    type_logement    VARCHAR(50)     NOT NULL,
    surface          INTEGER         NOT NULL,
    franchise        INTEGER         NOT NULL,
    cotisation_mensuelle NUMERIC(10,2) NOT NULL,
    cotisation_annuelle  NUMERIC(10,2) NOT NULL,
    resiliation_hamon    BOOLEAN       NOT NULL DEFAULT false,
    date_effet       DATE            NOT NULL,
    date_creation    TIMESTAMP       NOT NULL DEFAULT NOW()
);
