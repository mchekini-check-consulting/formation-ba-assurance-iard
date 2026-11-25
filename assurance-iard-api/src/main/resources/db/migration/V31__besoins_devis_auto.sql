-- US-29 : besoins et recommandation de formule auto (étape 2)
ALTER TABLE devis_auto
  ADD COLUMN valeur_vehicule         integer,
  ADD COLUMN vehicule_indispensable  boolean,
  ADD COLUMN assistance_0km          boolean,
  ADD COLUMN protection_conducteur   boolean,
  ADD COLUMN bris_glace              boolean,
  ADD COLUMN accompagnement_litige   boolean,
  ADD COLUMN priorite                varchar(10),
  ADD COLUMN formule_conseillee      varchar(30),
  ADD COLUMN raisons_formule         text,
  ADD COLUMN prix_mensuel_estime     numeric(10, 2);
