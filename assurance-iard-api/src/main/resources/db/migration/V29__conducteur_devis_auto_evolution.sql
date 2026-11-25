-- US-28 / US-59 / T-04 : colonnes manquantes sur conducteur et devis_auto pour l'étape 1.4
-- (Conducteurs et antécédents). Ajouts uniquement ; aucune donnée existante n'est modifiée.

-- conducteur : champs complémentaires pour le formulaire de l'étape 1.4
ALTER TABLE conducteur
    ADD COLUMN conducteur_principal               VARCHAR(25),
    ADD COLUMN conduite_accompagnee               BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN nb_sinistres_non_responsables_3_ans INTEGER,
    ADD CONSTRAINT ck_conducteur_principal CHECK (conducteur_principal IS NULL OR conducteur_principal IN
        ('MOI_MEME', 'ENFANT', 'CONJOINT', 'AUTRE_PERSONNE_FOYER')),
    ADD CONSTRAINT ck_conducteur_nb_sinistres_nr CHECK (nb_sinistres_non_responsables_3_ans IS NULL
        OR nb_sinistres_non_responsables_3_ans >= 0);

-- La loi Hamon peut aussi s'appliquer après "assuré moins d'un an" ; ajout de AUTRE_MOTIF
-- (résiliation pour un motif autre que non-paiement, sinistralité ou fausse déclaration)
ALTER TABLE conducteur
    DROP CONSTRAINT ck_conducteur_resiliation,
    ADD CONSTRAINT ck_conducteur_resiliation CHECK (resiliation_assureur IN
        ('AUCUNE', 'NON_PAIEMENT', 'SINISTRALITE', 'FAUSSE_DECLARATION', 'AUTRE_MOTIF'));

-- devis_auto : rendre devis_id nullable (le devis auto peut être créé sans compte en mode
-- parcours anonyme) et remplacer le booléen assure_plus_un_an par une enum à trois valeurs
ALTER TABLE devis_auto
    ALTER COLUMN devis_id DROP NOT NULL,
    ADD COLUMN assure_vehicule VARCHAR(20),
    DROP CONSTRAINT ck_devis_auto_ancien_assureur,
    ADD CONSTRAINT ck_devis_auto_assure_vehicule CHECK (assure_vehicule IS NULL OR assure_vehicule IN
        ('NON', 'OUI_MOINS_UN_AN', 'OUI_PLUS_UN_AN')),
    ADD CONSTRAINT ck_devis_auto_hamon CHECK (assure_vehicule IS NULL
        OR assure_vehicule <> 'OUI_PLUS_UN_AN'
        OR (ancien_assureur IS NOT NULL AND ancien_numero_contrat IS NOT NULL));
