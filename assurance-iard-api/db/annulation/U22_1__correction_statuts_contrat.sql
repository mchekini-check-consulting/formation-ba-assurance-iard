-- Annule V22.1 : retire seulement sa ligne de flyway_schema_history (à exécuter après U23, avant U22).
-- La correction SIGNE -> ACTIF / EN_ATTENTE_EFFET n'est pas défaite : l'ancienne valeur, invalide, n'est conservée
-- nulle part.
BEGIN;
DELETE FROM flyway_schema_history WHERE version = '22.1';
COMMIT;
