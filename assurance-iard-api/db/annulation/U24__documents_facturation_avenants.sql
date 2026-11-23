-- Annule V24 : supprime avenants, factures, échéances et documents (données comprises).
BEGIN;
DROP TABLE avenant_ligne;
DROP TABLE avenant;
DROP TABLE facture;
DROP TABLE echeance;
DROP TABLE document;
DELETE FROM flyway_schema_history WHERE version = '24';
COMMIT;
