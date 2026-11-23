-- Annule V23 : le contrat redevient habitation seulement.
-- S'arrête sans rien modifier s'il existe des contrats qui ne tiennent pas dans l'ancien schéma (auto, sans dossier,
-- sans logement, franchise non entière) : les traiter d'abord, aucune donnée n'est supprimée en silence.
BEGIN;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM contrat
               WHERE produit <> 'HABITATION' OR dossier_id IS NULL OR adresse_logement IS NULL
                  OR code_postal IS NULL OR ville IS NULL OR type_logement IS NULL OR surface IS NULL
                  OR franchise <> TRUNC(franchise)) THEN
        RAISE EXCEPTION 'Annulation V23 impossible : des contrats ne respectent pas le schéma du sprint 1';
    END IF;
END $$;
DROP TABLE historique_statut_contrat;
DROP TABLE contrat_option;
DROP TRIGGER tg_contrat_completer ON contrat;
DROP FUNCTION contrat_completer_depuis_dossier();
DROP INDEX idx_contrat_compte;
DROP INDEX idx_contrat_statut;
ALTER TABLE contrat
    DROP CONSTRAINT ck_contrat_produit,
    DROP CONSTRAINT ck_contrat_statut,
    DROP CONSTRAINT ck_contrat_motif_fin,
    DROP CONSTRAINT ck_contrat_frequence_paiement,
    DROP CONSTRAINT ck_contrat_jour_prelevement,
    DROP CONSTRAINT ck_contrat_habitation,
    DROP CONSTRAINT ck_contrat_auto,
    DROP CONSTRAINT ck_contrat_signe_mandat,
    DROP COLUMN produit,
    DROP COLUMN compte_id,
    DROP COLUMN formule,
    DROP COLUMN date_fin,
    DROP COLUMN motif_fin,
    DROP COLUMN frequence_paiement,
    DROP COLUMN jour_prelevement,
    DROP COLUMN mandat_sepa_id,
    DROP COLUMN vehicule_id,
    DROP COLUMN conducteur_principal_id,
    DROP COLUMN devis_id,
    DROP COLUMN signe_le,
    DROP COLUMN justificatifs_recus,
    DROP COLUMN date_modification,
    ALTER COLUMN dossier_id SET NOT NULL,
    ALTER COLUMN adresse_logement SET NOT NULL,
    ALTER COLUMN code_postal SET NOT NULL,
    ALTER COLUMN ville SET NOT NULL,
    ALTER COLUMN type_logement SET NOT NULL,
    ALTER COLUMN surface SET NOT NULL;
ALTER TABLE contrat ALTER COLUMN franchise TYPE INTEGER USING franchise::INTEGER;
DELETE FROM flyway_schema_history WHERE version = '23';
COMMIT;
