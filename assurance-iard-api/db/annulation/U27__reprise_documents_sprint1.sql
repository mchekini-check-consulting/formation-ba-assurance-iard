-- Annule V27 : retire les documents V1 créés par la reprise du sprint 1.
BEGIN;
DELETE FROM document WHERE motif = 'Reprise sprint 1 (T-05)';
DELETE FROM flyway_schema_history WHERE version = '27';
COMMIT;
