-- US-29 : renommage COUVERTURE -> PROTECTION_LARGE, extension colonne pour EQUILIBRE
ALTER TABLE devis_auto ALTER COLUMN priorite TYPE varchar(20);
UPDATE devis_auto SET priorite = 'PROTECTION_LARGE' WHERE priorite = 'COUVERTURE';