import { libelleFormule, libelleGarantie } from './contrat';
import { libelleSinistre } from './tableau-de-bord';

describe("libellés de l'espace client (US-18)", () => {
  it('formules habitation et auto', () => {
    expect(libelleFormule('confort')).toBe('Confort');
    expect(libelleFormule('TR')).toBe('Tous risques');
    expect(libelleFormule('INCONNUE')).toBe('INCONNUE');
    expect(libelleFormule(null)).toBe('');
  });

  it('garanties et sinistres, un code inconnu restant lisible', () => {
    expect(libelleGarantie('RC')).toBe('Responsabilité civile');
    expect(libelleGarantie('protection-juridique')).toBe('Protection juridique');
    expect(libelleGarantie('AUTRE_OPTION')).toBe('AUTRE_OPTION');
    expect(libelleSinistre('DEGAT_EAUX')).toBe('Dégât des eaux');
    expect(libelleSinistre('NOUVEAU')).toBe('NOUVEAU');
  });
});
