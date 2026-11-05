import {
  TAILLE_MAX_FICHIER,
  dateDepuisIso,
  dateVersIso,
  erreurDateNaissance,
  erreurFichier,
  erreurTelephone,
  formaterSaisieDate,
  nombreAutresColocataires,
  normaliserTelephone,
} from './informations';

const PDF = [0x25, 0x50, 0x44, 0x46, 0x2d, 0x31];
const JPG = [0xff, 0xd8, 0xff, 0xe0];
const PNG = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];

const fichier = (nom: string, octets: number[]): File => new File([new Uint8Array(octets)], nom);

describe('RG1 : date de naissance', () => {
  const aujourdhui = new Date(2026, 10, 5);

  it.each([
    ['05/11/2008', 'le jour de ses 18 ans'],
    ['04/11/2008', 'la veille de ses 18 ans passée'],
    ['31/01/1990', 'bien plus de 18 ans'],
    ['29/02/2000', 'un 29 février qui existe'],
  ])('accepte %s (%s)', (date) => {
    expect(erreurDateNaissance(date, aujourdhui)).toBeNull();
  });

  it.each([
    ['06/11/2008', 'un jour avant ses 18 ans'],
    ['05/12/2008', 'un mois avant ses 18 ans'],
    ['01/01/2020', 'un enfant'],
    ['01/01/2030', 'une date future'],
  ])('refuse %s (%s)', (date) => {
    expect(erreurDateNaissance(date, aujourdhui)).toContain('18 ans');
  });

  it.each(['31/02/1990', '29/02/2001', '32/01/1990', '12/13/1990', '1990-01-31', '1/1/1990', 'abc', '01/01/1899'])(
    'refuse le format ou la date impossible %s',
    (date) => {
      expect(erreurDateNaissance(date, aujourdhui)).toContain('JJ/MM/AAAA');
    },
  );

  it('réclame la date si le champ est vide', () => {
    expect(erreurDateNaissance('  ', aujourdhui)).toBe('Indiquez votre date de naissance.');
  });

  it.each([
    ['3', '3'],
    ['3101', '31/01'],
    ['31011990', '31/01/1990'],
    ['31/01/1990999', '31/01/1990'],
    ['ab31', '31'],
    ['', ''],
  ])('ajoute les barres pendant la frappe : %s', (saisie, attendu) => {
    expect(formaterSaisieDate(saisie)).toBe(attendu);
  });

  it('convertit la date dans les deux sens', () => {
    expect(dateVersIso('31/01/1990')).toBe('1990-01-31');
    expect(dateDepuisIso('1990-01-31')).toBe('31/01/1990');
  });
});

describe('RG2 : téléphone', () => {
  it.each(['0612345678', '06 12 34 56 78', '0123456789'])('accepte %s', (numero) => {
    expect(erreurTelephone(numero)).toBeNull();
  });

  it.each(['061234567', '06123456789', '0012345678', '+33612345678', '06.12.34.56.78', 'abcdefghij'])(
    'refuse %s',
    (numero) => {
      expect(erreurTelephone(numero)).toContain('10 chiffres');
    },
  );

  it('réclame le numéro si le champ est vide', () => {
    expect(erreurTelephone('')).toBe('Indiquez votre numéro de téléphone.');
  });

  it('retire les espaces avant envoi', () => {
    expect(normaliserTelephone('06 12 34 56 78')).toBe('0612345678');
  });
});

describe('RG4 : fichier', () => {
  it.each([
    ['carte.pdf', PDF],
    ['photo.jpg', JPG],
    ['photo.JPEG', JPG],
    ['scan.png', PNG],
  ])('accepte %s', async (nom, octets) => {
    expect(await erreurFichier(fichier(nom, octets))).toBeNull();
  });

  it('accepte un fichier de 5 Mo exactement', async () => {
    const contenu = new Uint8Array(TAILLE_MAX_FICHIER);
    contenu.set(PDF);
    expect(await erreurFichier(new File([contenu], 'carte.pdf'))).toBeNull();
  });

  it('refuse un fichier de plus de 5 Mo et dit quoi déposer', async () => {
    const contenu = new Uint8Array(TAILLE_MAX_FICHIER + 1);
    contenu.set(PDF);
    const erreur = await erreurFichier(new File([contenu], 'carte.pdf'));
    expect(erreur).toContain('dépasse 5 Mo');
    expect(erreur).toContain('PDF, JPG ou PNG');
  });

  it.each([
    ['un texte renommé en .pdf', 'faux.pdf', [0x74, 0x65, 0x78, 0x74, 0x65]],
    ['un programme renommé en .jpg', 'faux.jpg', [0x4d, 0x5a, 0, 0]],
    ['un vrai PDF avec une mauvaise extension', 'carte.exe', PDF],
    ['un fichier trop court', 'court.pdf', [0x25]],
    ['une photo HEIC', 'photo.heic', [0, 0, 0, 0x18, 0x66, 0x74, 0x79, 0x70]],
  ])('refuse %s', async (_cas, nom, octets) => {
    const erreur = await erreurFichier(fichier(nom, octets));
    expect(erreur).toContain("Ce format n'est pas accepté");
    expect(erreur).toContain('PDF, JPG ou PNG');
  });
});

describe('AC4 : colocataires', () => {
  it.each([
    ['2', 1],
    ['4', 3],
    ['10', 9],
    ['', 0],
  ])('%s personnes dans la colocation : %i noms à saisir', (nombre, attendu) => {
    expect(nombreAutresColocataires(nombre)).toBe(attendu);
  });
});
