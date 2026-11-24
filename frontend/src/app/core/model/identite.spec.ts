import { formaterNomPropre, initiales } from './identite';

describe('prénom et nom affichés (US-18)', () => {
  it.each([
    ['INES', 'Ines'],
    ['inès', 'Inès'],
    ['  BRAHIMI ', 'Brahimi'],
    ['jean-pierre', 'Jean-Pierre'],
    ["o'neil", "O'Neil"],
    ['marie claire', 'Marie Claire'],
    ['ÉLODIE', 'Élodie'],
    ['', ''],
    [null, ''],
  ])('« %s » s’affiche « %s »', (saisi, affiche) => {
    expect(formaterNomPropre(saisi)).toBe(affiche);
  });

  it('initiales du prénom et du nom', () => {
    expect(initiales('Camille', 'Durand')).toBe('CD');
    expect(initiales('élodie', 'martin')).toBe('ÉM');
    expect(initiales('David', null)).toBe('D');
  });
});
