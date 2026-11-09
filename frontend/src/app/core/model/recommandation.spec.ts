import {
  ReponsesBesoins,
  depassePlafondSerenite,
  formaterPrix,
  reponsesCompletes,
  reponsesVides,
} from './recommandation';

/** Situation sans aucune condition de RG1, qui mène à Essentielle si rien n'est changé. */
const essentielle: ReponsesBesoins = {
  valeurContenu: 5000,
  objetsValeur: false,
  appareilsCouteux: false,
  remboursement: 'vetuste',
  assistance: false,
  litiges: false,
  priorite: 'prix',
};

describe('reponsesCompletes', () => {
  it('exige les 7 réponses', () => {
    expect(reponsesCompletes(reponsesVides())).toBe(false);
    expect(reponsesCompletes({ ...essentielle, litiges: null })).toBe(false);
    expect(reponsesCompletes(essentielle)).toBe(true);
  });
});

describe('depassePlafondSerenite', () => {
  it.each([
    [null, false],
    [35000, false],
    [35001, true],
  ])('contenu de %s € : %s', (valeurContenu, attendu) => {
    expect(depassePlafondSerenite({ ...essentielle, valeurContenu })).toBe(attendu);
  });
});

describe('formaterPrix', () => {
  it('affiche deux décimales avec une virgule', () => {
    expect(formaterPrix(12.5)).toBe('12,50');
  });
});
