-- US-22 (recette) : données de test d'Alice (AE-RECETTE-1) rendues cohérentes avec sa date d'effet.
-- Chargées seulement si DONNEES_TEST_ACTIVES=true (local, dev, qualif ; jamais en preprod ni en prod), après T-05.
-- Les scripts T-05 et DonneesRecette (déjà sur main) ne sont pas modifiés : ce script corrige après eux, y compris sur
-- les bases dev et qualif déjà chargées. Rejouable : ne change rien si c'est déjà fait. Rien n'est supprimé.
--
-- Repère : FAC-RECETTE-0001 couvre le mois M-1 (du 1er au dernier jour) ; les échéances de T-05 tombent le 10 des
-- mois M-2, M-1, M et M+1. Exemple au chargement du 25/11/2026 : facture d'octobre, échéances des 10/09, 10/10,
-- 10/11 et 10/12.
-- * Date d'effet : le 26 du mois M, un an plus tôt (26/11/2025). L'année d'assurance du 26/11/2025 au 25/11/2026
--   compte les 12 échéances du 10/12/2025 au 10/11/2026.
-- * Échéance du mois M (10/11/2026) : PAYEE. Cette année d'assurance est alors entièrement réglée : la facture
--   annuelle devient testable. FAC-RECETTE-0001 couvre toujours l'échéance du 10/10.
DO $$
DECLARE
    v_contrat BIGINT;
    v_mois    DATE;
    v_effet   DATE;
BEGIN
    SELECT c.id, (f.periode_debut + INTERVAL '1 month')::DATE INTO v_contrat, v_mois
    FROM contrat c
    JOIN facture f ON f.contrat_id = c.id AND f.numero = 'FAC-RECETTE-0001'
    WHERE c.numero = 'AE-RECETTE-1';

    IF v_contrat IS NULL THEN
        RAISE NOTICE 'Données de test US-22 : contrat AE-RECETTE-1 ou facture FAC-RECETTE-0001 absent, rien à corriger';
        RETURN;
    END IF;

    v_effet := (v_mois + INTERVAL '25 days' - INTERVAL '1 year')::DATE;

    UPDATE contrat SET date_effet = v_effet
    WHERE id = v_contrat AND date_effet <> v_effet;

    UPDATE echeance SET statut = 'PAYEE', date_modification = NOW()
    WHERE contrat_id = v_contrat AND date_echeance = (v_mois + INTERVAL '9 days')::DATE AND statut <> 'PAYEE';
END $$;
