-- T-05 (correctif) : correction de données invalides, appliquée juste avant V23 qui limite les statuts de contrat.
-- La valeur SIGNE a été écrite par une ancienne version de ContratService sur une branche jamais fusionnée
-- (feature/moteurtarifaire, commit 37612b5) : ce n'est pas un statut du projet. Il ne s'agit PAS d'un changement de
-- statut des contrats existants (décision PO : statuts inchangés), seulement de la remise en conformité de ces lignes.
-- Un contrat SIGNE est signé : ACTIF si sa date d'effet est aujourd'hui ou passée, EN_ATTENTE_EFFET sinon.

UPDATE contrat
SET statut = CASE WHEN date_effet <= CURRENT_DATE THEN 'ACTIF' ELSE 'EN_ATTENTE_EFFET' END
WHERE statut = 'SIGNE';

-- Garde-fou : aucun autre statut inconnu n'est corrigé au hasard ; la migration s'arrête en listant les valeurs.
DO $$
DECLARE
    inconnus TEXT;
BEGIN
    SELECT string_agg(DISTINCT statut, ', ' ORDER BY statut) INTO inconnus
    FROM contrat
    WHERE statut NOT IN ('EN_ATTENTE_DE_SIGNATURE', 'EN_ATTENTE_EFFET', 'ACTIF', 'ACTIF_JUSTIFICATIFS_ATTENDUS',
                         'AVENANT_EN_ATTENTE', 'IMPAYE', 'SUSPENDU', 'RESILIATION_EN_COURS', 'RESILIE', 'RENONCE');
    IF inconnus IS NOT NULL THEN
        RAISE EXCEPTION 'V22.1 : statuts de contrat inconnus à traiter avant V23 : %', inconnus;
    END IF;
END $$;
