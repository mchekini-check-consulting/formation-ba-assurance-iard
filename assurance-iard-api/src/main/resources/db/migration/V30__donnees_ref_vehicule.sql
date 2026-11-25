-- US-27 : données de référence des véhicules (marques vedettes du parcours auto).
-- Appliquée au démarrage du serveur sur tous les environnements (migration Flyway).
-- Les noms de marques doivent correspondre exactement aux MARQUES_VEDETTES du frontend.

INSERT INTO ref_vehicule (marque, modele, energie, version, annee, valeur_neuve) VALUES
-- Renault
('Renault', 'Clio', 'ESSENCE', '1.0 TCe 90', 2022, 14200.00),
('Renault', 'Clio', 'HYBRIDE', 'E-Tech 140', 2023, 18900.00),
('Renault', 'Mégane', 'ESSENCE', '1.3 TCe 140', 2021, 21500.00),
('Renault', 'Mégane', 'ELECTRIQUE', 'EV60 220 ch', 2023, 35990.00),
('Renault', 'Captur', 'ESSENCE', '1.0 TCe 90', 2022, 20900.00),
('Renault', 'Captur', 'HYBRIDE', 'E-Tech 145', 2022, 24900.00),
('Renault', 'Zoé', 'ELECTRIQUE', 'R110', 2021, 23200.00),
-- Peugeot
('Peugeot', '208', 'ESSENCE', '1.2 PureTech 100', 2022, 17200.00),
('Peugeot', '208', 'ELECTRIQUE', 'e-208 136 ch', 2022, 27200.00),
('Peugeot', '308', 'ESSENCE', '1.2 PureTech 130', 2021, 18500.00),
('Peugeot', '308', 'DIESEL', '1.5 BlueHDi 130', 2021, 20500.00),
('Peugeot', '3008', 'ESSENCE', '1.2 PureTech 130', 2022, 28900.00),
('Peugeot', '3008', 'HYBRIDE', 'PHEV 300', 2022, 42900.00),
-- Citroën
('Citroën', 'C3', 'ESSENCE', '1.2 PureTech 83', 2022, 14500.00),
('Citroën', 'C3', 'DIESEL', '1.5 BlueHDi 100', 2020, 11500.00),
('Citroën', 'C4', 'ESSENCE', '1.2 PureTech 130', 2022, 24500.00),
('Citroën', 'C4', 'ELECTRIQUE', 'ë-C4 136 ch', 2022, 33800.00),
-- Toyota
('Toyota', 'Yaris', 'ESSENCE', '1.0 VVT-i 72 ch', 2021, 15400.00),
('Toyota', 'Yaris', 'HYBRIDE', '1.5 Hybride 116 ch', 2022, 20900.00),
('Toyota', 'Corolla', 'HYBRIDE', '1.8 Hybride 122 ch', 2022, 27500.00),
('Toyota', 'RAV4', 'HYBRIDE', '2.5 Hybride 218 ch', 2022, 38700.00),
-- Volkswagen
('Volkswagen', 'Polo', 'ESSENCE', '1.0 TSI 95', 2022, 18900.00),
('Volkswagen', 'Golf', 'ESSENCE', '1.0 TSI 110', 2022, 24700.00),
('Volkswagen', 'Golf', 'DIESEL', '2.0 TDI 150', 2022, 27900.00),
('Volkswagen', 'Tiguan', 'ESSENCE', '1.5 TSI 150', 2022, 34500.00),
-- Dacia
('Dacia', 'Sandero', 'ESSENCE', '1.0 TCe 90', 2022, 11690.00),
('Dacia', 'Sandero', 'GPL', '1.0 TCe 100 ECO-G', 2022, 14090.00),
('Dacia', 'Duster', 'ESSENCE', '1.3 TCe 150', 2022, 19490.00),
('Dacia', 'Duster', 'GPL', '1.0 TCe 100 ECO-G', 2022, 16690.00),
('Dacia', 'Logan', 'ESSENCE', '1.0 SCe 65', 2022, 11690.00),
-- Ford
('Ford', 'Puma', 'ESSENCE', '1.0 EcoBoost 125', 2022, 24900.00),
('Ford', 'Puma', 'HYBRIDE', '1.0 EcoBoost mHEV 155', 2022, 28100.00),
('Ford', 'Focus', 'ESSENCE', '1.0 EcoBoost 125', 2022, 23500.00),
('Ford', 'Kuga', 'HYBRIDE', 'PHEV 225', 2022, 39900.00);
