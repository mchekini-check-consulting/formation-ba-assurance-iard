-- T-04 : référentiels auto (ref_vehicule + ref_plaque_test) pour les tests de recette.
-- Idempotent : ne recrée rien si déjà présent.
DO $$
DECLARE
    v_peugeot   BIGINT;
    v_renault   BIGINT;
    v_porsche   BIGINT;
BEGIN
    -- Véhicules de référence
    INSERT INTO ref_vehicule (marque, modele, energie, version, annee, valeur_neuve)
    VALUES ('Peugeot', '308', 'ESSENCE', '1.2 PureTech 130', 2021, 18500.00)
    ON CONFLICT DO NOTHING;

    INSERT INTO ref_vehicule (marque, modele, energie, version, annee, valeur_neuve)
    VALUES ('Renault', 'Clio', 'ESSENCE', '1.0 TCe 90', 2022, 14200.00)
    ON CONFLICT DO NOTHING;

    INSERT INTO ref_vehicule (marque, modele, energie, version, annee, valeur_neuve)
    VALUES ('Porsche', 'Taycan', 'ELECTRIQUE', 'Turbo S', 2023, 185000.00)
    ON CONFLICT DO NOTHING;

    INSERT INTO ref_vehicule (marque, modele, energie, version, annee, valeur_neuve)
    VALUES ('Citroën', 'C3', 'DIESEL', '1.5 BlueHDi 100', 2020, 11500.00)
    ON CONFLICT DO NOTHING;

    SELECT id INTO v_peugeot FROM ref_vehicule WHERE marque = 'Peugeot' AND modele = '308' LIMIT 1;
    SELECT id INTO v_renault FROM ref_vehicule WHERE marque = 'Renault' AND modele = 'Clio' LIMIT 1;
    SELECT id INTO v_porsche FROM ref_vehicule WHERE marque = 'Porsche' AND modele = 'Taycan' LIMIT 1;

    -- Plaques de test
    -- AB-123-CD → Peugeot 308 trouvé (cas nominal)
    INSERT INTO ref_plaque_test (immatriculation, ref_vehicule_id, comportement)
    VALUES ('AB-123-CD', v_peugeot, 'TROUVE')
    ON CONFLICT (immatriculation) DO NOTHING;

    -- EF-456-GH → Renault Clio trouvé (cas avec véhicule de petite valeur)
    INSERT INTO ref_plaque_test (immatriculation, ref_vehicule_id, comportement)
    VALUES ('EF-456-GH', v_renault, 'TROUVE')
    ON CONFLICT (immatriculation) DO NOTHING;

    -- ZZ-999-ZZ → Porsche Taycan trouvé (cas véhicule haut de gamme)
    INSERT INTO ref_plaque_test (immatriculation, ref_vehicule_id, comportement)
    VALUES ('ZZ-999-ZZ', v_porsche, 'TROUVE')
    ON CONFLICT (immatriculation) DO NOTHING;

    -- XX-000-XX → plaque inconnue au SIV (comportement simulé)
    INSERT INTO ref_plaque_test (immatriculation, ref_vehicule_id, comportement)
    VALUES ('XX-000-XX', null, 'INCONNU')
    ON CONFLICT (immatriculation) DO NOTHING;

END $$;
