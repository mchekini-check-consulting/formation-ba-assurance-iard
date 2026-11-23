-- Annule V22 : le devis enregistré redevient habitation seulement.
-- S'arrête sans rien modifier s'il existe des devis auto ou incomplets : les traiter d'abord.
BEGIN;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM devis_enregistre WHERE produit <> 'HABITATION') THEN
        RAISE EXCEPTION 'Annulation V22 impossible : des devis auto existent';
    END IF;
END $$;
DROP TABLE devis_option;
DROP TABLE devis_auto;
ALTER TABLE devis_enregistre
    DROP CONSTRAINT ck_devis_enregistre_produit,
    DROP CONSTRAINT ck_devis_enregistre_statut_devis,
    DROP CONSTRAINT ck_devis_enregistre_habitation,
    DROP COLUMN produit,
    DROP COLUMN statut_devis,
    DROP COLUMN formule_conseillee,
    DROP COLUMN choix_different_conseil,
    DROP COLUMN reponses_besoins,
    DROP COLUMN date_modification,
    ALTER COLUMN reference SET NOT NULL,
    ALTER COLUMN type_logement SET NOT NULL,
    ALTER COLUMN adresse SET NOT NULL,
    ALTER COLUMN code_postal SET NOT NULL,
    ALTER COLUMN ville SET NOT NULL,
    ALTER COLUMN surface SET NOT NULL,
    ALTER COLUMN formule SET NOT NULL,
    ALTER COLUMN options SET NOT NULL,
    ALTER COLUMN franchise SET NOT NULL,
    ALTER COLUMN prix_mensuel SET NOT NULL;
DELETE FROM flyway_schema_history WHERE version = '22';
COMMIT;
