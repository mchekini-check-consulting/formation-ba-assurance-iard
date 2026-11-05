import { logementVide } from './devis';
import { saisieVide } from './informations';
import { BlocRecapitulatif, DonneesRecapitulatif, blocsRecapitulatif } from './recapitulatif';

const donnees = (partiel: Partial<DonneesRecapitulatif> = {}): DonneesRecapitulatif => ({
  logement: {
    ...logementVide(),
    statut: 'colocation',
    residence: 'principale',
    typeLogement: 'appartement',
    adresse: '12 rue de la Paix',
    codePostal: '75001',
    ville: 'Paris',
    surface: '60',
    nombrePieces: '3',
  },
  besoins: {
    valeurContenu: 15000,
    objetsValeur: true,
    appareilsCouteux: false,
    remboursement: 'vetuste',
    assistance: true,
    litiges: false,
    priorite: 'equilibre',
  },
  formule: 'confort',
  options: ['objets-valeur'],
  franchise: '300',
  prixMensuel: 12.5,
  informations: {
    ...saisieVide(),
    civilite: 'madame',
    nom: 'Durand',
    prenom: 'Marie',
    dateNaissance: '31/01/1990',
    telephone: '06 12 34 56 78',
    pieceIdentite: { type: 'passeport', fichier: new File(['a'], 'carte.pdf'), erreur: null },
    domicile: { type: 'gaz', fichier: new File(['b'], 'facture.pdf'), erreur: null },
  },
  colocataires: ['Paul Martin'],
  ...partiel,
});

const valeurs = (bloc: BlocRecapitulatif): Record<string, string> =>
  Object.fromEntries(bloc.lignes.map(({ libelle, valeur }) => [libelle, valeur]));

describe('blocsRecapitulatif', () => {
  it('AC5 : présente 4 blocs reliés à leur étape', () => {
    expect(blocsRecapitulatif(donnees()).map(({ titre, etape }) => [titre, etape])).toEqual([
      ['Mon logement', '/devis/logement'],
      ['Mes besoins', '/devis/besoins'],
      ['Ma formule', '/devis/formule'],
      ['Mon identité', '/devis/informations'],
    ]);
  });

  it('traduit le logement', () => {
    expect(valeurs(blocsRecapitulatif(donnees())[0])).toEqual({
      Situation: 'Colocation',
      Résidence: 'Principale',
      Logement: 'Appartement',
      Adresse: '12 rue de la Paix, 75001 Paris',
      Surface: '60 m²',
      Pièces: '3',
    });
  });

  it('traduit les autres valeurs du logement', () => {
    const logement = {
      ...logementVide(),
      statut: 'proprietaire-occupant' as const,
      residence: 'secondaire' as const,
      typeLogement: 'maison' as const,
      nombrePieces: '10',
    };
    const bloc = valeurs(blocsRecapitulatif(donnees({ logement }))[0]);

    expect(bloc['Situation']).toBe('Propriétaire occupant');
    expect(bloc['Résidence']).toBe('Secondaire');
    expect(bloc['Logement']).toBe('Maison');
    expect(bloc['Pièces']).toBe('10 ou plus');
  });

  it('laisse vide une situation inconnue', () => {
    const logement = { ...logementVide(), statut: null };
    expect(valeurs(blocsRecapitulatif(donnees({ logement }))[0])['Situation']).toBe('');
  });

  it('traduit les besoins', () => {
    const bloc = valeurs(blocsRecapitulatif(donnees())[1]);

    expect(bloc['Valeur du contenu']).toContain('€');
    expect(bloc['Objets de valeur']).toBe('Oui');
    expect(bloc['Appareils coûteux']).toBe('Non');
    expect(bloc['Remboursement']).toBe('Vétusté déduite');
    expect(bloc['Priorité']).toBe("L'équilibre");
  });

  it('traduit les autres réponses aux besoins', () => {
    const besoins = { ...donnees().besoins, remboursement: 'neuf' as const, priorite: null, valeurContenu: null };
    const bloc = valeurs(blocsRecapitulatif(donnees({ besoins }))[1]);

    expect(bloc['Remboursement']).toBe('À neuf');
    expect(bloc['Priorité']).toBe('');
    expect(bloc['Valeur du contenu']).toBe('0 €');
  });

  it('traduit la formule', () => {
    expect(valeurs(blocsRecapitulatif(donnees())[2])).toEqual({
      Formule: 'Confort',
      Options: 'Objets de valeur',
      Franchise: '300 €',
      Cotisation: '12,50 €/mois',
    });
  });

  it('indique l’absence d’option et les valeurs manquantes', () => {
    const bloc = valeurs(
      blocsRecapitulatif(donnees({ formule: null, options: [], franchise: null, prixMensuel: null }))[2],
    );

    expect(bloc).toEqual({ Formule: '', Options: 'Aucune', Franchise: '', Cotisation: '' });
  });

  it('reprend l’identité de l’étape 4', () => {
    expect(valeurs(blocsRecapitulatif(donnees())[3])).toEqual({
      Identité: 'Madame Marie Durand',
      'Date de naissance': '31/01/1990',
      Téléphone: '06 12 34 56 78',
      "Pièce d'identité": 'Passeport : carte.pdf',
      'Justificatif de domicile': 'Facture de gaz : facture.pdf',
      Colocataires: 'Paul Martin',
    });
  });

  it('n’affiche pas de colocataires s’il n’y en a pas', () => {
    expect(valeurs(blocsRecapitulatif(donnees({ colocataires: [] }))[3])['Colocataires']).toBeUndefined();
  });

  it('traduit la civilité Monsieur et tolère des fichiers absents', () => {
    const informations = { ...saisieVide(), civilite: 'monsieur' as const, nom: 'Durand', prenom: 'Paul' };
    const bloc = valeurs(blocsRecapitulatif(donnees({ informations }))[3]);

    expect(bloc['Identité']).toBe('Monsieur Paul Durand');
    expect(bloc["Pièce d'identité"]).toBe(' : ');
  });
});
