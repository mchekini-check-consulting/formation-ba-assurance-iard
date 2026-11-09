CREATE TABLE compte (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email             VARCHAR(254) NOT NULL,
    mot_de_passe_hash VARCHAR(100) NOT NULL,
    date_creation     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_compte_email UNIQUE (email)
);
