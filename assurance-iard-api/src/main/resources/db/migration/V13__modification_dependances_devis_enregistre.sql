ALTER TABLE devis_enregistre
    ALTER COLUMN dependances TYPE VARCHAR(20)
    USING CASE WHEN dependances = FALSE THEN 'aucune' END;
