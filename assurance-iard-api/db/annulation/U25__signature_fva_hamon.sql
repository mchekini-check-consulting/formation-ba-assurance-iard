-- Annule V25 : supprime preuves de signature, déclarations FVA et résiliations Hamon (données comprises).
BEGIN;
DROP TABLE resiliation_hamon;
DROP TABLE declaration_fva;
DROP TABLE preuve_signature;
DELETE FROM flyway_schema_history WHERE version = '25';
COMMIT;
