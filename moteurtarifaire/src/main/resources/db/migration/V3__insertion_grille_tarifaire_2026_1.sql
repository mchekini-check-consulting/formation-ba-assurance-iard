insert into grille_tarifaire (version, date_debut) values ('2026.1', date '2026-01-01');

insert into parametre_tarifaire (grille_id, categorie, cle, valeur)
select grille.id, parametre.categorie, parametre.cle, parametre.valeur
from grille_tarifaire grille
cross join (values
    ('general', 'duree-validite-jours', '30'),
    ('arrondi', 'decimales', '2'),
    ('arrondi', 'mode', 'HALF_UP'),

    ('prix-base', 'ESSENTIELLE', '7.90'),
    ('prix-base', 'CONFORT', '10.90'),
    ('prix-base', 'SERENITE', '15.90'),

    ('coefficient-depart', 'surface-reference', '45'),
    ('coefficient-depart', 'surface-plafond', '400'),
    ('coefficient-depart', 'pente-surface', '0.35'),
    ('coefficient-depart', 'coefficient-surface-minimum', '0.80'),
    ('coefficient-depart', 'pieces-reference', '2'),
    ('coefficient-depart', 'increment-par-piece', '0.06'),

    ('statut', 'LOCATAIRE', '1'),
    ('statut', 'PROPRIETAIRE_OCCUPANT', '1.6'),
    ('type-logement', 'APPARTEMENT', '1'),
    ('type-logement', 'MAISON', '1.35'),
    ('residence', 'PRINCIPALE', '1'),
    ('residence', 'SECONDAIRE', '1.25'),
    ('sinistres', '0', '1'),
    ('sinistres', '1', '1.15'),
    ('sinistres', '2', '1.3'),
    ('sinistres', '3', '1.5'),
    ('zone', '75', '1.2'),
    ('zone', '92', '1.2'),
    ('zone', '93', '1.2'),
    ('zone', '94', '1.2'),
    ('zone', '13', '1.1'),
    ('zone', '69', '1.1'),
    ('zone', '06', '1.1'),
    ('zone', '33', '1.1'),
    ('zone', '31', '1.1'),
    ('zone', '59', '1.1'),
    ('etage', 'RDC', '1.05'),
    ('etage', 'INTERMEDIAIRE', '1'),
    ('etage', 'DERNIER', '1'),
    ('dependances', 'AUCUNE', '1'),
    ('dependances', 'JARDIN', '1.05'),
    ('dependances', 'GARAGE', '1.05'),
    ('dependances', 'PISCINE', '1.15'),
    ('dependances', 'PLUSIEURS', '1.2'),

    ('prix-option', 'OBJETS_VALEUR', '2.50'),
    ('prix-option', 'ASSISTANCE', '1.50'),
    ('prix-option', 'PROTECTION_JURIDIQUE', '2.90'),
    ('franchise', '150', '1.00'),
    ('franchise', '300', '0'),
    ('franchise', '500', '-1.00'),

    ('refus-statut', 'PROPRIETAIRE_NON_OCCUPANT', 'La tarification en ligne n''est pas disponible pour un propriétaire non occupant.'),
    ('refus-resiliation', 'NON_PAIEMENT', 'La tarification en ligne n''est pas disponible après une résiliation pour non-paiement.'),

    ('recommandation', 'seuil-contenu-serenite', '20000'),
    ('recommandation', 'seuil-contenu-essentielle', '10000'),
    ('raison', 'CONTENU_ELEVE', 'Votre contenu dépasse 20 000 €'),
    ('raison', 'REMBOURSEMENT_NEUF', 'Vous souhaitez un remboursement à neuf'),
    ('raison', 'APPAREILS_COUTEUX', 'Vous possédez des appareils coûteux'),
    ('raison', 'PRIORITE_PROTECTION', 'Vous privilégiez la protection la plus large'),
    ('raison', 'CONTENU_MOYEN', 'Votre contenu est compris entre 10 000 € et 20 000 €'),
    ('raison', 'OBJETS_VALEUR', 'Vous déclarez des objets de valeur'),
    ('raison', 'PRIORITE_EQUILIBRE', 'Vous recherchez l''équilibre entre protection et budget'),
    ('raison', 'CONTENU_FAIBLE', 'Votre contenu est inférieur à 10 000 €'),
    ('raison', 'AUCUN_OBJET_VALEUR', 'Vous ne déclarez aucun objet de valeur'),
    ('raison', 'PRIORITE_PRIX', 'Votre priorité est le prix')
) as parametre (categorie, cle, valeur)
where grille.version = '2026.1';
