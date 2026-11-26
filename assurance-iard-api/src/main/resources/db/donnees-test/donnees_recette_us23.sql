-- US-23 (recette) : données de test de Camille (recette.auto@assureasy.fr) rendues cohérentes avec les règles de
-- l'avenant. Chargées seulement si DONNEES_TEST_ACTIVES=true (local, dev, qualif ; jamais en preprod ni en prod),
-- après T-05. Le script T-05 (déjà sur main) n'est pas modifié : ce script corrige après lui, y compris sur les bases
-- dev et qualif déjà chargées. Rejouable : ne change rien si c'est déjà fait. Rien n'est supprimé.
--
-- * AE-RECETTE-H01 (habitation, Avenant en attente) : l'avenant de T-05 ajoutait « BRIS_GLACE », qui n'existe pas
--   en habitation. Il ajoute l'option « objets-valeur » de la grille du sprint 1, au même prix (2,50 € TTC par mois) :
--   18,50 € → 21,00 € TTC par mois. Date d'effet : le lendemain (elle est recalculée à l'acceptation). Son projet
--   d'avenant (document sans référence) reçoit le numéro de l'avenant AV-AAAA-NNNNNN.
-- * AE-RECETTE-A03 (auto, Actif, Tous risques) : une option déjà souscrite pour que le retrait d'une garantie (RG4) se
--   teste dès la première recette : protection juridique, 2,50 € TTC par mois (grille AUTO-2026.1). Cotisation :
--   45,00 € → 47,50 € TTC par mois (570,00 € par an) ; la responsabilité civile (12,00 €, obligatoire) est gardée.
-- * AE-RECETTE-A05 (auto, Intermédiaire, « Avenant en attente » dans T-05 mais sans aucun avenant) : un vrai avenant
--   EN_ATTENTE, + assistance dès 0 km (3,50 € TTC par mois, grille AUTO-2026.1, compatible Intermédiaire) :
--   55,00 € → 58,50 € TTC par mois (660,00 → 702,00 € par an), date d'effet le lendemain.
-- * Règle : aucun contrat « Avenant en attente » sans avenant EN_ATTENTE. Tout autre contrat dans ce cas redevient
--   « Actif » (historique tracé, auteur SYSTEME).
DO $$
DECLARE
    v_h01     BIGINT;
    v_a03     BIGINT;
    v_a05     BIGINT;
    v_avenant BIGINT;
BEGIN
    SELECT id INTO v_h01 FROM contrat WHERE numero = 'AE-RECETTE-H01';
    SELECT id INTO v_a03 FROM contrat WHERE numero = 'AE-RECETTE-A03' AND statut = 'ACTIF';

    IF v_h01 IS NULL THEN
        RAISE NOTICE 'Données de test US-23 : contrat AE-RECETTE-H01 absent, rien à corriger';
    ELSE
        SELECT id INTO v_avenant FROM avenant WHERE contrat_id = v_h01 AND statut = 'EN_ATTENTE';
        IF v_avenant IS NOT NULL THEN
            UPDATE avenant_ligne SET code_option = 'objets-valeur', prix_mensuel_ttc = 2.50, date_modification = NOW()
            WHERE avenant_id = v_avenant AND code_option = 'BRIS_GLACE';
            UPDATE avenant SET date_effet = CURRENT_DATE + 1, date_modification = NOW()
            WHERE id = v_avenant AND date_effet <> CURRENT_DATE + 1;
            UPDATE document d
            SET reference = 'AV-' || EXTRACT(YEAR FROM (a.date_creation AT TIME ZONE 'Europe/Paris'))::INTEGER
                            || '-' || LPAD(a.id::TEXT, 6, '0'),
                date_modification = NOW()
            FROM avenant a
            WHERE a.id = v_avenant AND d.id = a.document_id AND d.reference IS NULL;
        END IF;
    END IF;

    IF v_a03 IS NULL THEN
        RAISE NOTICE 'Données de test US-23 : contrat AE-RECETTE-A03 actif absent, rien à compléter';
    ELSIF NOT EXISTS (SELECT 1 FROM contrat_option WHERE contrat_id = v_a03 AND code_option = 'PROTECTION_JURIDIQUE')
          AND NOT EXISTS (SELECT 1 FROM avenant WHERE contrat_id = v_a03) THEN
        INSERT INTO contrat_option (contrat_id, code_option, prix_mensuel_ttc, obligatoire, date_debut)
        SELECT v_a03, 'PROTECTION_JURIDIQUE', 2.50, FALSE, date_effet FROM contrat WHERE id = v_a03;
        UPDATE contrat SET cotisation_mensuelle = cotisation_mensuelle + 2.50,
                           cotisation_annuelle = (cotisation_mensuelle + 2.50) * 12
        WHERE id = v_a03;
    END IF;

    SELECT c.id INTO v_a05 FROM contrat c
    WHERE c.numero = 'AE-RECETTE-A05' AND c.statut = 'AVENANT_EN_ATTENTE'
      AND NOT EXISTS (SELECT 1 FROM avenant a WHERE a.contrat_id = c.id AND a.statut = 'EN_ATTENTE');
    IF v_a05 IS NOT NULL THEN
        INSERT INTO avenant (numero, contrat_id, statut, date_effet, prime_avant_ttc, prime_apres_ttc)
        SELECT COALESCE((SELECT MAX(numero) FROM avenant WHERE contrat_id = v_a05), 0) + 1, v_a05, 'EN_ATTENTE',
               CURRENT_DATE + 1, cotisation_mensuelle, cotisation_mensuelle + 3.50
        FROM contrat WHERE id = v_a05
        RETURNING id INTO v_avenant;
        INSERT INTO avenant_ligne (avenant_id, code_option, action, prix_mensuel_ttc)
        VALUES (v_avenant, 'ASSISTANCE_0KM', 'AJOUT', 3.50);
    END IF;

    -- Aucun contrat « Avenant en attente » sans avenant EN_ATTENTE.
    INSERT INTO historique_statut_contrat (contrat_id, statut_avant, statut_apres, change_le, motif, auteur)
    SELECT c.id, 'AVENANT_EN_ATTENTE', 'ACTIF', NOW(), 'Recette US-23 : aucun avenant en attente', 'SYSTEME'
    FROM contrat c
    WHERE c.statut = 'AVENANT_EN_ATTENTE'
      AND NOT EXISTS (SELECT 1 FROM avenant a WHERE a.contrat_id = c.id AND a.statut = 'EN_ATTENTE');
    UPDATE contrat c SET statut = 'ACTIF'
    WHERE c.statut = 'AVENANT_EN_ATTENTE'
      AND NOT EXISTS (SELECT 1 FROM avenant a WHERE a.contrat_id = c.id AND a.statut = 'EN_ATTENTE');

    RAISE NOTICE 'Données de recette US-23 vérifiées';
END $$;
