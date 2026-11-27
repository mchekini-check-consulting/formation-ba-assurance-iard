-- US-24 (recette) : sinistres de test d'Alice et de Camille rendus cohérents avec les règles de la déclaration.
-- Chargé seulement si DONNEES_TEST_ACTIVES=true (local, dev, qualif ; jamais en preprod ni en prod), après T-05 et
-- US-23. Le script T-05 (déjà sur main) n'est pas modifié : ce script corrige après lui, y compris sur les bases dev
-- et qualif déjà chargées. Rejouable : ne change rien si c'est déjà fait.
--
-- * Alice (recette.assure1, AE-RECETTE-1, habitation Confort) : SIN-RECETTE-0001 (DECLARE, dégât des eaux) prend un
--   numéro au format de RG5 (SIN-AAAA-XXXXXX : année de la déclaration, identifiant sur 6 chiffres). Il était daté
--   de 10 jours avant sa déclaration, au-delà des 5 jours ouvrés (RG2), sans explication du retard : il est daté de
--   la veille de sa déclaration. Corbeille HABITATION (RG9) ; aucune photo déposée : « pièces manquantes » (AC7).
-- * Camille (recette.auto, AE-RECETTE-A03, auto Tous risques) : son brouillon (accident sans tiers) reprend à
--   l'étape 2 ; « qui déclare » et « blessés » n'avaient pas été répondus par le client (valeurs par défaut de
--   T-05) : ils sont vidés (aucune présélection). Il est recréé s'il a disparu (purge à 30 jours, RG6).
-- * Camille : un brouillon expiré (créé il y a 31 jours) pour tester la purge (RG6). Invisible dans « Mes sinistres » ;
--   supprimé par la purge (2 minutes après le démarrage, puis toutes les heures), recréé au démarrage suivant.
-- * Camille (décision PO du 27/11/2026) : un brouillon complet (bris de glace) sur AE-RECETTE-A06 (Impayé), prêt à
--   l'envoi : l'envoi est refusé avec le numéro d'assistance 24h/24 (AC1). Recréé s'il a disparu (purge, RG6).
-- * Cas de recette déjà présents (rien à créer) : AE-RECETTE-A04 (formule Tiers : vol non couvert, AC3),
--   AE-RECETTE-A09 (résilié il y a 5 jours, moins de 2 ans : déclarable, AC1), AE-RECETTE-A01, A02, A06, A07 et A10
--   (en attente de signature, en attente d'effet, impayé, suspendu, renoncé : non déclarables, AC1).
DO $$
DECLARE
    v_a03 BIGINT;
    v_a06 BIGINT;
BEGIN
    UPDATE sinistre s
    SET numero = 'SIN-' || EXTRACT(YEAR FROM (s.declare_le AT TIME ZONE 'Europe/Paris'))::INTEGER || '-'
                 || LPAD(s.id::TEXT, 6, '0'),
        date_survenance = LEAST(CAST(s.declare_le AT TIME ZONE 'Europe/Paris' AS DATE) - 1,
                                CAST(s.date_creation AS DATE)),
        tiers_implique = COALESCE(s.tiers_implique, FALSE),
        blesses = COALESCE(s.blesses, FALSE),
        hors_delai = FALSE,
        corbeille = 'HABITATION',
        pieces_manquantes = NOT EXISTS (SELECT 1 FROM piece_jointe p
                                        WHERE p.sinistre_id = s.id AND p.type_piece = 'PHOTOS'
                                          AND p.statut = 'STOCKEE'),
        etape = 3,
        date_modification = NOW()
    WHERE s.numero = 'SIN-RECETTE-0001';

    SELECT id INTO v_a03 FROM contrat WHERE numero = 'AE-RECETTE-A03';
    IF v_a03 IS NULL THEN
        RAISE NOTICE 'Données de test US-24 : contrat AE-RECETTE-A03 absent, brouillons de Camille ignorés';
        RETURN;
    END IF;

    UPDATE sinistre
    SET declarant = NULL, blesses = NULL, etape = 2, corbeille = 'AUTO', date_modification = NOW()
    WHERE contrat_id = v_a03 AND statut = 'BROUILLON' AND description LIKE '%(brouillon de recette T-05)'
      AND etape = 1;

    IF NOT EXISTS (SELECT 1 FROM sinistre WHERE contrat_id = v_a03 AND statut = 'BROUILLON'
                   AND description LIKE '%(brouillon de recette T-05)') THEN
        INSERT INTO sinistre (contrat_id, statut, type, date_survenance, lieu, description, tiers_implique,
                              corbeille, etape)
        VALUES (v_a03, 'BROUILLON', 'ACCIDENT_SANS_TIERS', CURRENT_DATE - 2, 'Parking du travail',
                'Rétroviseur arraché (brouillon de recette T-05)', FALSE, 'AUTO', 2);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM sinistre WHERE contrat_id = v_a03 AND description LIKE '%(recette US-24 : purge)') THEN
        INSERT INTO sinistre (contrat_id, statut, type, date_survenance, lieu, description, corbeille, etape,
                              brouillon_expire_le, date_creation, date_modification)
        VALUES (v_a03, 'BROUILLON', 'BRIS_GLACE', CURRENT_DATE - 32, 'Rue de la Recette',
                'Pare-brise fissuré, brouillon expiré (recette US-24 : purge)', 'AUTO', 2,
                NOW() - INTERVAL '1 day', NOW() - INTERVAL '31 days', NOW() - INTERVAL '31 days');
    END IF;

    SELECT id INTO v_a06 FROM contrat WHERE numero = 'AE-RECETTE-A06' AND statut = 'IMPAYE';
    IF v_a06 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sinistre WHERE contrat_id = v_a06
                                         AND description LIKE '%(recette US-24 : impayé)') THEN
        INSERT INTO sinistre (contrat_id, statut, type, date_survenance, lieu, description, tiers_implique, blesses,
                              declarant, corbeille, etape)
        VALUES (v_a06, 'BROUILLON', 'BRIS_GLACE', CURRENT_DATE - 1, 'Rue de la Recette',
                'Pare-brise fissuré par un gravillon (recette US-24 : impayé)', FALSE, FALSE, 'SOUSCRIPTEUR', 'AUTO', 3);
    END IF;

    RAISE NOTICE 'Données de recette US-24 vérifiées';
END $$;
