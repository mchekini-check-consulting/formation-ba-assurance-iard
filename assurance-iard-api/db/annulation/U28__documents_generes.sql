-- Annule V28 : supprime le contenu des PDF générés, les conditions générales provisoires et le champ reference.
-- S'arrête sans rien modifier si un contrat a plusieurs documents EN_VIGUEUR du même type (plusieurs factures ou
-- avenants) : l'ancienne règle d'unicité ne pourrait pas être rétablie ; les traiter d'abord.
BEGIN;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM document
               WHERE statut = 'EN_VIGUEUR' AND contrat_id IS NOT NULL
               GROUP BY contrat_id, type HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Annulation V28 impossible : des contrats ont plusieurs documents en vigueur du même type';
    END IF;
END $$;
-- Les documents dont le PDF était en base redeviennent « non stockés » (ils seront à regénérer).
UPDATE document SET stocke = FALSE, cle_stockage = NULL, empreinte = NULL
WHERE cle_stockage LIKE 'base:document_contenu/%';
DROP TABLE document_contenu;
-- Conditions générales provisoires créées par T-03 (sans contrat, rattachées par leur référence).
DELETE FROM document WHERE type = 'CONDITIONS_GENERALES' AND contrat_id IS NULL AND reference IS NOT NULL;
DROP INDEX idx_document_a_stocker;
DROP INDEX uk_document_en_vigueur;
CREATE UNIQUE INDEX uk_document_en_vigueur ON document (contrat_id, type) WHERE statut = 'EN_VIGUEUR';
ALTER TABLE document DROP COLUMN reference;
DELETE FROM flyway_schema_history WHERE version = '28';
COMMIT;
