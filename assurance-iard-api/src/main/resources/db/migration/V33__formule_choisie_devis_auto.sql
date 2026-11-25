-- US-29 étape 3 : formule choisie, options, dérogation conseil, prix finaux et validité.
ALTER TABLE devis_auto
    ADD COLUMN formule_choisie                   varchar(30),
    ADD COLUMN options_choisies                  text,
    ADD COLUMN choix_different_du_conseil        boolean NOT NULL DEFAULT false,
    ADD COLUMN date_confirmation_choix_different timestamp with time zone,
    ADD COLUMN prix_mensuel_ttc                  numeric(10, 2),
    ADD COLUMN prix_annuel_ttc                   numeric(10, 2),
    ADD COLUMN version_grille                    varchar(50),
    ADD COLUMN date_fin_validite                 date;
