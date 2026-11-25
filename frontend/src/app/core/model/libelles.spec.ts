import { dateFrancaise, libelleFormule, libelleGarantie, libelleMotif } from './contrat';
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

  it('US-21 : motifs des versions en français, date JJ/MM/AAAA', () => {
    expect(libelleMotif('DEMANDE_CLIENT')).toBe('À votre demande');
    expect(libelleMotif('AVENANT')).toBe('Suite à un avenant');
    expect(libelleMotif('COORDONNEES')).toBe('Suite au changement de vos coordonnées');
    expect(libelleMotif('CONDITIONS_GENERALES')).toBe('Nouvelles conditions générales');
    expect(libelleMotif('Données de recette T-05')).toBeNull();
    expect(libelleMotif(null)).toBeNull();
    expect(dateFrancaise('2026-12-01T23:30:00Z')).toBe('02/12/2026');
    expect(dateFrancaise(null)).toBe('');
  });
});
