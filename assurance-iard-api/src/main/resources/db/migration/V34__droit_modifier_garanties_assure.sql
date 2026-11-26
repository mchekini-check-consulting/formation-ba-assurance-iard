-- US-23 : l'assuré modifie ses garanties et accepte son avenant (routes POST /contrats/{numero}/avenants...,
-- droit CONTRATS/MODIFIER). Ce droit n'était utilisé par aucune route jusqu'ici : il est accordé au profil ASSURE.
-- Le prospect ne l'a pas (il n'a pas de contrat). Rien n'est supprimé ; le changement est tracé comme ceux faits
-- depuis l'écran « Gérer les profils » (seulement s'il a eu lieu).
WITH accorde AS (
    UPDATE permission SET valeur = TRUE
    WHERE profil = 'ASSURE' AND rubrique = 'CONTRATS' AND droit = 'MODIFIER' AND valeur = FALSE
    RETURNING profil, rubrique, droit
)
INSERT INTO historique_permission (modifie_le, modifie_par_email, profil, rubrique, droit, ancienne_valeur,
                                   nouvelle_valeur)
SELECT NOW(), 'migration-V34 (US-23)', profil, rubrique, droit, FALSE, TRUE FROM accorde;
