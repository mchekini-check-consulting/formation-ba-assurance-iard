-- T-05 : chaque contrat du sprint 1 reçoit ses conditions particulières en version 1, EN_VIGUEUR.
-- Le PDF du contrat n'a jamais été stocké (il est produit à la demande) : stocke = faux, sans clé de stockage.

INSERT INTO document (contrat_id, devis_id, type, version, statut, motif, stocke)
SELECT c.id, c.devis_id, 'CONDITIONS_PARTICULIERES', 1, 'EN_VIGUEUR', 'Reprise sprint 1 (T-05)', FALSE
FROM contrat c
WHERE NOT EXISTS (SELECT 1 FROM document d
                  WHERE d.contrat_id = c.id AND d.type = 'CONDITIONS_PARTICULIERES' AND d.statut = 'EN_VIGUEUR');
