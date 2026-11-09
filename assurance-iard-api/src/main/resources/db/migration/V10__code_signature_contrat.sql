CREATE TABLE code_signature_contrat (
    id              BIGSERIAL PRIMARY KEY,
    contrat_id      BIGINT NOT NULL REFERENCES contrat(id),
    code_hash       VARCHAR(255) NOT NULL,
    date_expiration TIMESTAMP WITH TIME ZONE NOT NULL,
    essais          INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_code_signature_contrat UNIQUE (contrat_id)
);
