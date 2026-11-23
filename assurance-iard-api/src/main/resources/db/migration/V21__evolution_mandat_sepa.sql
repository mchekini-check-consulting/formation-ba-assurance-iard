-- T-05 : le mandat SEPA est rattaché au client (compte_id) et non plus seulement au dossier d'une souscription
-- habitation. Règle RGPD inchangée (US-14, US-04) : l'IBAN complet n'est jamais conservé, ni en clair ni chiffré ;
-- iban_masque est calculé par la base et ne montre que les 4 derniers caractères.

ALTER TABLE mandat_sepa
    ADD COLUMN compte_id         BIGINT,
    ADD COLUMN iban_masque       VARCHAR(34) GENERATED ALWAYS AS (
        CASE WHEN iban_fin IS NULL THEN NULL ELSE '•••• ' || iban_fin END) STORED,
    ADD COLUMN bic               VARCHAR(11),
    ADD COLUMN banque            VARCHAR(100),
    ADD COLUMN statut            VARCHAR(10) NOT NULL DEFAULT 'ACTIF',
    ADD COLUMN date_modification TIMESTAMP WITH TIME ZONE;

-- Reprise : le client du mandat est celui de son dossier.
UPDATE mandat_sepa m
SET compte_id = d.compte_id
FROM dossier_souscription d
WHERE d.id = m.dossier_id;

-- Compatibilité avec le code du sprint 1, qui crée le mandat à partir du dossier sans renseigner le client.
CREATE FUNCTION mandat_sepa_compte_depuis_dossier() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.compte_id IS NULL AND NEW.dossier_id IS NOT NULL THEN
        SELECT d.compte_id INTO NEW.compte_id FROM dossier_souscription d WHERE d.id = NEW.dossier_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_mandat_sepa_compte
    BEFORE INSERT OR UPDATE OF dossier_id, compte_id ON mandat_sepa
    FOR EACH ROW EXECUTE FUNCTION mandat_sepa_compte_depuis_dossier();

ALTER TABLE mandat_sepa
    ALTER COLUMN compte_id SET NOT NULL,
    ALTER COLUMN dossier_id DROP NOT NULL,
    ADD CONSTRAINT fk_mandat_sepa_compte FOREIGN KEY (compte_id) REFERENCES compte (id),
    ADD CONSTRAINT ck_mandat_sepa_statut CHECK (statut IN ('ACTIF', 'REVOQUE'));

CREATE INDEX idx_mandat_sepa_compte ON mandat_sepa (compte_id);
