import { emailValide, motDePasseConforme, reglesMotDePasse } from './compte';

describe('emailValide', () => {
  it.each([
    ['prenom.nom@exemple.fr', true],
    ['  a@b.fr  ', true],
    ['prenom.nom', false],
    ['prenom@exemple', false],
    ['pre nom@exemple.fr', false],
    ['', false],
  ])('%s → %s', (email, attendu) => {
    expect(emailValide(email)).toBe(attendu);
  });
});

describe('reglesMotDePasse', () => {
  it('indique les règles respectées une par une', () => {
    expect(reglesMotDePasse('abc').map((r) => r.respectee)).toEqual([false, false, false]);
    expect(reglesMotDePasse('abcdefgh').map((r) => r.respectee)).toEqual([true, false, false]);
    expect(reglesMotDePasse('Abcdefgh').map((r) => r.respectee)).toEqual([true, true, false]);
    expect(reglesMotDePasse('Abcdefg1').map((r) => r.respectee)).toEqual([true, true, true]);
  });

  it('donne le libellé de chaque règle (RG2)', () => {
    expect(reglesMotDePasse('').map((r) => r.libelle)).toEqual([
      '8 caractères minimum',
      'Au moins une majuscule',
      'Au moins un chiffre',
    ]);
  });
});

describe('motDePasseConforme', () => {
  it.each([
    ['Abcdefg1', true],
    ['MotDePasse2026', true],
    ['Abc1', false],
    ['abcdefg1', false],
    ['Abcdefgh', false],
  ])('%s → %s', (motDePasse, attendu) => {
    expect(motDePasseConforme(motDePasse)).toBe(attendu);
  });
});
