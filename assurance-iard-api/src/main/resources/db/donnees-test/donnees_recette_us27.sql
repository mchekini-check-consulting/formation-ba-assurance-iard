-- US-27 : plaques de test supplémentaires pour les comportements ERREUR et DELAI du simulateur SIV.
-- Idempotent : ne recrée rien si déjà présent.
INSERT INTO ref_plaque_test (immatriculation, ref_vehicule_id, comportement)
VALUES ('EE-500-EE', null, 'ERREUR')
ON CONFLICT (immatriculation) DO NOTHING;

INSERT INTO ref_plaque_test (immatriculation, ref_vehicule_id, comportement)
VALUES ('DD-408-DD', null, 'DELAI')
ON CONFLICT (immatriculation) DO NOTHING;
