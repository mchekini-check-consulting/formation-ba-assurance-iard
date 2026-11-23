-- Annule V21 : le mandat redevient rattaché au seul dossier de souscription.
-- S'arrête sans rien modifier s'il existe des mandats sans dossier (mandats auto) : les traiter d'abord.
BEGIN;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM mandat_sepa WHERE dossier_id IS NULL) THEN
        RAISE EXCEPTION 'Annulation V21 impossible : des mandats sans dossier existent';
    END IF;
END $$;
DROP TRIGGER tg_mandat_sepa_compte ON mandat_sepa;
DROP FUNCTION mandat_sepa_compte_depuis_dossier();
DROP INDEX idx_mandat_sepa_compte;
ALTER TABLE mandat_sepa
    DROP CONSTRAINT fk_mandat_sepa_compte,
    DROP CONSTRAINT ck_mandat_sepa_statut,
    DROP COLUMN iban_masque,
    DROP COLUMN compte_id,
    DROP COLUMN bic,
    DROP COLUMN banque,
    DROP COLUMN statut,
    DROP COLUMN date_modification,
    ALTER COLUMN dossier_id SET NOT NULL;
DELETE FROM flyway_schema_history WHERE version = '21';
COMMIT;
