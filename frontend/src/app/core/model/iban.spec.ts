import { erreurIban, formaterIban, ibanMasque, ibanValide, normaliserIban } from './iban';

// Uniquement des IBAN de test, publiés comme exemples.
describe('IBAN', () => {
  it.each(['FR7630006000011234567890189', 'DE89370400440532013000', 'BE71096123456769', 'FR1420041010050500013M02606'])(
    'accepte %s',
    (iban) => {
      expect(ibanValide(iban)).toBe(true);
    },
  );

  it('accepte la saisie avec espaces et minuscules', () => {
    expect(ibanValide('fr76 3000 6000 0112 3456 7890 189')).toBe(true);
  });

  it.each(['FR7630006000011234567890188', 'FR7730006000011234567890189', 'DE89370400440532013001'])(
    'refuse la clé de contrôle fausse de %s',
    (iban) => {
      expect(ibanValide(iban)).toBe(false);
    },
  );

  it.each([
    'FR76',
    '7630006000011234567890189FR',
    'FR763000600001123456789018',
    'FR76300060000112345678901890',
    'FR76-30006000011234567890189',
    'FRAB30006000011234567890189',
  ])('refuse le format de %s', (iban) => {
    expect(ibanValide(iban)).toBe(false);
  });

  it('refuse un IBAN français trop court, même avec une clé juste', () => {
    expect(ibanValide('FR1420041010050500013M026')).toBe(false);
  });

  it('explique le problème', () => {
    expect(erreurIban('  ')).toBe('Indiquez votre IBAN.');
    expect(erreurIban('FR7630006000011234567890188')).toContain("n'est pas valide");
    expect(erreurIban('FR7630006000011234567890189')).toBeNull();
  });

  it('normalise et présente la saisie par groupes de 4', () => {
    expect(normaliserIban(' fr76 3000 ')).toBe('FR763000');
    expect(formaterIban('fr7630006000011234567890189')).toBe('FR76 3000 6000 0112 3456 7890 189');
    expect(formaterIban('FR76')).toBe('FR76');
    expect(formaterIban('')).toBe('');
  });

  it('AC1 : ne laisse visibles que les 4 derniers caractères', () => {
    const masque = ibanMasque('FR76 3000 6000 0112 3456 7890 189');
    expect(masque.endsWith('0189')).toBe(true);
    expect(masque).not.toContain('FR76');
    expect(masque).not.toContain('3000');
  });
});
