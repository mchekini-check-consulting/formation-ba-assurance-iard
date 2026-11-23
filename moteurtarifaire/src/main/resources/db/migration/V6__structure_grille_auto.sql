-- T-04 : séparation habitation / auto sur grille_tarifaire,
-- et colonnes manquantes sur ref_formule, ref_option, ref_tarif (créés en T-05).

-- 1. Colonne produit sur grille_tarifaire (toutes les grilles existantes sont HABITATION).
alter table grille_tarifaire
    add column if not exists produit varchar(20) not null default 'HABITATION';

update grille_tarifaire set produit = 'HABITATION' where produit is null;

-- 2. Remplacer l'unicité sur version seule par (produit, version).
alter table grille_tarifaire
    drop constraint if exists grille_tarifaire_version_key;

alter table grille_tarifaire
    add constraint grille_tarifaire_produit_version_key unique (produit, version);

alter table grille_tarifaire
    add constraint grille_tarifaire_produit_check check (produit in ('HABITATION', 'AUTO'));

-- Supprimer la valeur par défaut : toute nouvelle grille doit préciser son produit.
alter table grille_tarifaire
    alter column produit drop default;

-- 3. Colonnes supplémentaires sur ref_formule.
alter table ref_formule
    add column if not exists garanties_incluses text[] not null default '{}',
    add column if not exists non_couvert        text[] not null default '{}',
    add column if not exists part_rc            numeric(5, 4),
    add column if not exists soumis_bonus_malus boolean not null default false;

-- 4. Colonnes supplémentaires sur ref_option.
alter table ref_option
    add column if not exists categorie_taxe     varchar(20),
    add column if not exists soumis_bonus_malus boolean not null default false;

-- 5. Étendre le CHECK sur ref_tarif.critere pour accueillir les critères auto.
alter table ref_tarif drop constraint if exists ref_tarif_critere;
alter table ref_tarif
    add constraint ref_tarif_critere check (
        critere in ('AGE', 'BONUS_MALUS', 'USAGE', 'VALEUR_VEHICULE',
                    'KM_ANNUEL', 'STATIONNEMENT', 'SURPRIME_NOVICE')
    );
