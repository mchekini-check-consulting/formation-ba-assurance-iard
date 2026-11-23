-- Annule V26 : supprime les sinistres et leurs pièces jointes (données comprises).
BEGIN;
DROP TABLE piece_jointe;
ALTER TABLE document DROP CONSTRAINT fk_document_sinistre;
DROP TABLE sinistre;
DELETE FROM flyway_schema_history WHERE version = '26';
COMMIT;
