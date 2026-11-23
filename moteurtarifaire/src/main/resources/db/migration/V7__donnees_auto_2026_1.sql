-- T-04 : grille tarifaire AUTO-2026.1 avec formules, options et coefficients.
-- Flyway applique chaque migration une seule fois ; les ON CONFLICT des tables
-- avec contrainte d'unicité protègent d'une ré-exécution manuelle.

-- 1. Grille AUTO-2026.1
insert into grille_tarifaire (produit, version, date_debut)
values ('AUTO', 'AUTO-2026.1', '2026-01-01')
on conflict (produit, version) do update set date_debut = excluded.date_debut;

-- 2. Formules (3 niveaux de couverture, ordre croissant)
insert into ref_formule (grille_id, code, libelle, franchise, prix_mensuel_base, ordre,
                         garanties_incluses, non_couvert, part_rc, soumis_bonus_malus)
select g.id,
       f.code,
       f.libelle,
       f.franchise,
       f.prix_mensuel_base,
       f.ordre,
       f.garanties_incluses,
       f.non_couvert,
       f.part_rc,
       f.soumis_bonus_malus
from grille_tarifaire g
cross join (values
    ('TIERS', 'Tiers',
     null::numeric(10,2), 25.00::numeric(10,2), 1,
     array['Responsabilité civile']::text[],
     array['Dommages à votre véhicule', 'Vol', 'Incendie', 'Bris de glace']::text[],
     1.0000::numeric(5,4), true),
    ('INTER', 'Intermédiaire',
     400.00::numeric(10,2), 35.00::numeric(10,2), 2,
     array['RC', 'Vol', 'Incendie', 'Bris de glace', 'Événements climatiques']::text[],
     array['Accident responsable']::text[],
     0.6000::numeric(5,4), true),
    ('TR', 'Tous risques',
     400.00::numeric(10,2), 50.00::numeric(10,2), 3,
     array['RC', 'Vol', 'Incendie', 'Bris de glace', 'Événements climatiques', 'Accident responsable']::text[],
     array[]::text[],
     0.4000::numeric(5,4), true)
) as f(code, libelle, franchise, prix_mensuel_base, ordre,
       garanties_incluses, non_couvert, part_rc, soumis_bonus_malus)
where g.produit = 'AUTO' and g.version = 'AUTO-2026.1'
on conflict (grille_id, code) do update
    set libelle            = excluded.libelle,
        franchise          = excluded.franchise,
        prix_mensuel_base  = excluded.prix_mensuel_base,
        ordre              = excluded.ordre,
        garanties_incluses = excluded.garanties_incluses,
        non_couvert        = excluded.non_couvert,
        part_rc            = excluded.part_rc,
        soumis_bonus_malus = excluded.soumis_bonus_malus,
        date_modification  = now();

-- 3. Options auto
insert into ref_option (grille_id, code, libelle, prix_mensuel, produit, obligatoire,
                        formules_compatibles, categorie_taxe, soumis_bonus_malus)
select g.id,
       o.code,
       o.libelle,
       o.prix_mensuel,
       'AUTO',
       false,
       o.formules_compatibles,
       o.categorie_taxe,
       false
from grille_tarifaire g
cross join (values
    ('BRIS_DE_GLACE',          'Bris de glace',              3.50::numeric(10,2), array['TIERS','INTER','TR']::text[], 'ACCESSOIRE'),
    ('ASSISTANCE_PANNE',       'Assistance 0 km',            2.90::numeric(10,2), array['TIERS','INTER','TR']::text[], 'ACCESSOIRE'),
    ('CONDUCTEUR_NON_DESIGNE', 'Conducteur non désigné',     4.00::numeric(10,2), array['INTER','TR']::text[],         'ACCESSOIRE'),
    ('PROTECTION_JURIDIQUE',   'Protection juridique',       5.50::numeric(10,2), array['TIERS','INTER','TR']::text[], 'ACCESSOIRE'),
    ('REMPLACEMENT_VEHICULE',  'Véhicule de remplacement',   6.90::numeric(10,2), array['TR']::text[],                 'ACCESSOIRE')
) as o(code, libelle, prix_mensuel, formules_compatibles, categorie_taxe)
where g.produit = 'AUTO' and g.version = 'AUTO-2026.1'
on conflict (grille_id, code) do update
    set libelle              = excluded.libelle,
        prix_mensuel         = excluded.prix_mensuel,
        formules_compatibles = excluded.formules_compatibles,
        categorie_taxe       = excluded.categorie_taxe,
        date_modification    = now();

-- 4. Coefficients (ref_tarif) — table sans unicité métier, INSERT direct
insert into ref_tarif (grille_id, critere, borne_min, borne_max, valeur, coefficient)
select g.id, c.critere, c.borne_min, c.borne_max, c.valeur, c.coefficient
from grille_tarifaire g
cross join (values
    -- Années de permis (0 = moins d'un an, 2 = 2 ans…)
    ('AGE', 0.00::numeric(10,2),  2.00::numeric(10,2),  null::varchar(30), 1.300::numeric(6,3)),
    ('AGE', 3.00::numeric(10,2),  25.00::numeric(10,2), null,              1.000::numeric(6,3)),
    ('AGE', 26.00::numeric(10,2), null,                 null,              0.950::numeric(6,3)),
    -- Coefficient bonus-malus (CRM)
    ('BONUS_MALUS', 0.50::numeric(10,2), 0.79::numeric(10,2), null, 0.850::numeric(6,3)),
    ('BONUS_MALUS', 0.80::numeric(10,2), 1.00::numeric(10,2), null, 1.000::numeric(6,3)),
    ('BONUS_MALUS', 1.01::numeric(10,2), 1.50::numeric(10,2), null, 1.250::numeric(6,3)),
    ('BONUS_MALUS', 1.51::numeric(10,2), 3.50::numeric(10,2), null, 1.600::numeric(6,3)),
    -- Usage du véhicule
    ('USAGE', null, null, 'PRIVE',          1.000::numeric(6,3)),
    ('USAGE', null, null, 'TRAJET_TRAVAIL', 1.100::numeric(6,3)),
    ('USAGE', null, null, 'PROFESSIONNEL',  1.350::numeric(6,3)),
    -- Valeur à neuf du véhicule (€)
    ('VALEUR_VEHICULE', 0.00::numeric(10,2),    15000.00::numeric(10,2), null, 0.900::numeric(6,3)),
    ('VALEUR_VEHICULE', 15001.00::numeric(10,2), 30000.00::numeric(10,2), null, 1.000::numeric(6,3)),
    ('VALEUR_VEHICULE', 30001.00::numeric(10,2), null,                    null, 1.200::numeric(6,3)),
    -- Kilométrage annuel
    ('KM_ANNUEL', 0.00::numeric(10,2),     10000.00::numeric(10,2), null, 0.900::numeric(6,3)),
    ('KM_ANNUEL', 10001.00::numeric(10,2), 20000.00::numeric(10,2), null, 1.000::numeric(6,3)),
    ('KM_ANNUEL', 20001.00::numeric(10,2), null,                    null, 1.200::numeric(6,3)),
    -- Stationnement habituel
    ('STATIONNEMENT', null, null, 'GARAGE_FERME',  0.900::numeric(6,3)),
    ('STATIONNEMENT', null, null, 'PARKING_PRIVE', 1.000::numeric(6,3)),
    ('STATIONNEMENT', null, null, 'RUE',           1.100::numeric(6,3)),
    -- Surprime novice (permis < 3 ans)
    ('SURPRIME_NOVICE', null, null, 'OUI', 1.200::numeric(6,3)),
    ('SURPRIME_NOVICE', null, null, 'NON', 1.000::numeric(6,3))
) as c(critere, borne_min, borne_max, valeur, coefficient)
where g.produit = 'AUTO' and g.version = 'AUTO-2026.1'
  and not exists (
      select 1 from ref_tarif rt
      where rt.grille_id = g.id
        and rt.critere = c.critere
  );
