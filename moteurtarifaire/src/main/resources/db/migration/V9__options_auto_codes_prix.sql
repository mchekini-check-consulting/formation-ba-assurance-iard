-- US-29 : correction des codes et prix des options auto.
-- Les options BRIS_DE_GLACE, ASSISTANCE_PANNE, CONDUCTEUR_NON_DESIGNE, REMPLACEMENT_VEHICULE
-- ne correspondent pas aux codes envoyés par le frontend.

delete from ref_option ro
using grille_tarifaire g
where ro.grille_id = g.id
  and g.produit = 'AUTO'
  and ro.code in ('BRIS_DE_GLACE', 'ASSISTANCE_PANNE', 'CONDUCTEUR_NON_DESIGNE', 'REMPLACEMENT_VEHICULE');

insert into ref_option (grille_id, code, libelle, prix_mensuel, produit, formules_compatibles, categorie_taxe)
select g.id, o.code, o.libelle, o.prix_mensuel, 'AUTO', o.formules_compatibles, 'ACCESSOIRE'
from grille_tarifaire g
cross join (values
    ('ASSISTANCE_0KM',        'Assistance dès 0 km',               3.50::numeric(10,2), array['TIERS','INTER','TR']::text[]),
    ('VEHICULE_REMPLACEMENT', 'Véhicule de remplacement',           4.00::numeric(10,2), array['INTER','TR']::text[]),
    ('PROTECTION_CONDUCTEUR', 'Protection du conducteur renforcée', 5.00::numeric(10,2), array['TIERS','INTER','TR']::text[])
) as o(code, libelle, prix_mensuel, formules_compatibles)
where g.produit = 'AUTO' and g.version = 'AUTO-2026.1'
on conflict (grille_id, code) do update
    set libelle              = excluded.libelle,
        prix_mensuel         = excluded.prix_mensuel,
        formules_compatibles = excluded.formules_compatibles,
        date_modification    = now();

update ref_option ro
set prix_mensuel = 2.50, date_modification = now()
from grille_tarifaire g
where ro.grille_id = g.id
  and g.produit = 'AUTO'
  and ro.code = 'PROTECTION_JURIDIQUE';
