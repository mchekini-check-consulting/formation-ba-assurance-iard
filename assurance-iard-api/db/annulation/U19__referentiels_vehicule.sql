-- Annule V19 : supprime les référentiels véhicule (données T-04 comprises).
BEGIN;
DROP TABLE ref_plaque_test;
DROP TABLE ref_vehicule;
DELETE FROM flyway_schema_history WHERE version = '19';
COMMIT;
