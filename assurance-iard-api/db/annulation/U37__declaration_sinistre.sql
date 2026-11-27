-- Annule V37 (US-24) : référentiels des sinistres, contenu des pièces jointes et champs ajoutés au sinistre.
-- S'arrête sans rien modifier si une pièce jointe dépasse 5 Mo (l'ancienne contrainte ne pourrait pas être rétablie).
-- Les pièces jointes stockées en base perdent leur contenu : elles redeviennent de simples descriptions.
BEGIN;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM piece_jointe WHERE taille_octets > 5242880) THEN
        RAISE EXCEPTION 'Annulation V37 impossible : des pièces jointes dépassent 5 Mo';
    END IF;
END $$;
DROP TABLE ref_piece_attendue;
DROP TABLE ref_type_sinistre;
DROP INDEX idx_sinistre_brouillon_expire;
ALTER TABLE sinistre DROP CONSTRAINT ck_sinistre_nombre_blesses;
ALTER TABLE sinistre DROP CONSTRAINT ck_sinistre_etape;
ALTER TABLE sinistre DROP CONSTRAINT ck_sinistre_corbeille;
UPDATE sinistre SET date_survenance = CAST(date_creation AS DATE) WHERE date_survenance IS NULL;
ALTER TABLE sinistre ALTER COLUMN date_survenance SET NOT NULL;
UPDATE sinistre SET tiers_implique = FALSE WHERE tiers_implique IS NULL;
UPDATE sinistre SET blesses = FALSE WHERE blesses IS NULL;
ALTER TABLE sinistre ALTER COLUMN tiers_implique SET NOT NULL;
ALTER TABLE sinistre ALTER COLUMN tiers_implique SET DEFAULT FALSE;
ALTER TABLE sinistre ALTER COLUMN blesses SET NOT NULL;
ALTER TABLE sinistre ALTER COLUMN blesses SET DEFAULT FALSE;
ALTER TABLE sinistre DROP COLUMN etape;
ALTER TABLE sinistre DROP COLUMN date_arrete_catnat;
ALTER TABLE sinistre DROP COLUMN nombre_blesses;
ALTER TABLE sinistre DROP COLUMN pieces_manquantes;
ALTER TABLE sinistre DROP COLUMN a_requalifier;
ALTER TABLE sinistre DROP COLUMN corbeille;
DROP TABLE piece_jointe_contenu;
UPDATE piece_jointe SET cle_stockage = NULL WHERE cle_stockage LIKE 'base:piece_jointe_contenu/%';
ALTER TABLE piece_jointe DROP COLUMN empreinte;
ALTER TABLE piece_jointe DROP CONSTRAINT ck_piece_jointe_taille;
ALTER TABLE piece_jointe ADD CONSTRAINT ck_piece_jointe_taille CHECK (taille_octets BETWEEN 1 AND 5242880);
DELETE FROM flyway_schema_history WHERE version = '37';
COMMIT;
