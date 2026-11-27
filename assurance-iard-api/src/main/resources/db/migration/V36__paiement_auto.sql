-- US-31 étape 5 : lien entre un contrat auto et son devis auto source (idempotence, évite les doublons).
ALTER TABLE contrat ADD COLUMN devis_auto_id BIGINT;
ALTER TABLE contrat ADD CONSTRAINT fk_contrat_devis_auto FOREIGN KEY (devis_auto_id) REFERENCES devis_auto (id);
CREATE UNIQUE INDEX idx_contrat_devis_auto ON contrat (devis_auto_id) WHERE devis_auto_id IS NOT NULL;
