-- Annule V20 : supprime véhicules et conducteurs (données comprises).
BEGIN;
DROP TABLE conducteur;
DROP TABLE vehicule;
DELETE FROM flyway_schema_history WHERE version = '20';
COMMIT;
