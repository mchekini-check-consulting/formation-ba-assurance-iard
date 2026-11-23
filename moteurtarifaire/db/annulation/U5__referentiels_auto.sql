-- Annule V5 : supprime les référentiels de tarification auto (données T-04 comprises).
BEGIN;
DROP TABLE ref_tarif;
DROP TABLE ref_option;
DROP TABLE ref_formule;
DELETE FROM flyway_schema_history WHERE version = '5';
COMMIT;
