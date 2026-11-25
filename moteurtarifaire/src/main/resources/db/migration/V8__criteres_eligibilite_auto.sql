-- US-27 : critères d'éligibilité à la souscription en ligne pour la grille AUTO-2026.1.
-- Un bonus-malus au-dessus du seuil ou une résiliation pour non-paiement bloquent la souscription.

insert into parametre_tarifaire (grille_id, categorie, cle, valeur)
select g.id, 'ELIGIBILITE', 'BONUS_MALUS_MAX_LIGNE', '1.50'
from grille_tarifaire g
where g.produit = 'AUTO' and g.version = 'AUTO-2026.1'
on conflict (grille_id, categorie, cle) do nothing;

insert into parametre_tarifaire (grille_id, categorie, cle, valeur)
select g.id, 'ELIGIBILITE', 'RESILIATION_BLOQUANTE', 'NON_PAIEMENT'
from grille_tarifaire g
where g.produit = 'AUTO' and g.version = 'AUTO-2026.1'
on conflict (grille_id, categorie, cle) do nothing;
