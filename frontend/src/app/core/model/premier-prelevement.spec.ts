import {
  JourPrelevement,
  dateDepuisSaisie,
  erreurDateDebut,
  formaterDate,
  premierPrelevement,
} from './premier-prelevement';

describe('RG1 : date de début', () => {
  const aujourdhui = new Date(2026, 10, 5, 15, 30);

  it.each([
    ['06/11/2026', 'le lendemain'],
    ['03/02/2027', 'le 90e jour'],
    ['01/12/2026', 'une date entre les deux'],
  ])('accepte %s (%s)', (date) => {
    expect(erreurDateDebut(date, aujourdhui)).toBeNull();
  });

  it.each(['04/11/2026', '05/11/2026', '01/01/2020'])('refuse %s : passée ou aujourd’hui', (date) => {
    expect(erreurDateDebut(date, aujourdhui)).toBe('La date de début doit être au plus tôt demain.');
  });

  it('refuse le 91e jour', () => {
    expect(erreurDateDebut('04/02/2027', aujourdhui)).toBe('La date de début doit être dans les 90 prochains jours.');
  });

  it.each(['31/02/2027', '2026-12-01', '1/1/2027'])('refuse le format ou la date impossible %s', (date) => {
    expect(erreurDateDebut(date, aujourdhui)).toContain('JJ/MM/AAAA');
  });

  it('réclame la date si le champ est vide', () => {
    expect(erreurDateDebut(' ', aujourdhui)).toBe('Indiquez la date de début de votre contrat.');
  });
});

describe('RG3 et RG4 : premier prélèvement', () => {
  const date = (texte: string): Date => dateDepuisSaisie(texte) as Date;

  it.each([
    ['06/11/2026', 10, '05/11/2026', '10/12/2026', 'le délai de 14 jours écarte le 10 novembre'],
    ['06/11/2026', 15, '01/11/2026', '15/11/2026', 'le délai tombe exactement sur le jour choisi'],
    ['06/11/2026', 15, '02/11/2026', '15/12/2026', 'un jour de moins que le délai'],
    ['20/01/2027', 5, '05/11/2026', '05/02/2027', 'la date de début décide'],
    ['05/01/2027', 5, '05/11/2026', '05/01/2027', 'la date de début tombe sur le jour choisi'],
    ['20/12/2026', 5, '10/12/2026', '05/01/2027', 'passage à l’année suivante'],
    ['16/02/2027', 15, '01/12/2026', '15/03/2027', 'février court'],
  ] as [string, JourPrelevement, string, string, string][])(
    'début %s, le %i, signature %s : %s (%s)',
    (debut, jour, signature, attendu) => {
      expect(formaterDate(premierPrelevement(date(debut), jour, date(signature)))).toBe(attendu);
    },
  );

  it('ne tient pas compte de l’heure de signature', () => {
    const resultat = premierPrelevement(date('06/11/2026'), 15, new Date(2026, 10, 1, 23, 59));
    expect(formaterDate(resultat)).toBe('15/11/2026');
  });
});

describe('dateDepuisSaisie', () => {
  it('renvoie null pour une date impossible', () => {
    expect(dateDepuisSaisie('31/02/2027')).toBeNull();
  });
});
