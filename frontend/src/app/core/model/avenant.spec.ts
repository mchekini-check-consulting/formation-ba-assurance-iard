import {
  GarantieProposee,
  ProjetAvenant,
  aChange,
  cotisationApres,
  dateHeureFrancaise,
  dateJjMmAaaa,
  garantiesSouscrites,
  messageApplication,
  montantTtc,
} from './avenant';
import { PROJET_A03 } from './avenant.exemples';

/** Camille, AE-RECETTE-A03 : RC obligatoire, protection juridique souscrite, options ajoutables. */
const GARANTIES: GarantieProposee[] = [
  { code: 'RC', libelle: 'Responsabilité civile', prixMensuelTtc: 12, souscrite: true, obligatoire: true, modifiable: false, raison: 'obligatoire' },
  { code: 'PROTECTION_JURIDIQUE', libelle: 'Protection juridique', prixMensuelTtc: 2.5, souscrite: true, obligatoire: false, modifiable: true, raison: null },
  { code: 'ASSISTANCE_0KM', libelle: 'Assistance dès 0 km', prixMensuelTtc: 3.5, souscrite: false, obligatoire: false, modifiable: true, raison: null },
];

describe('avenant (US-23)', () => {
  it('montants au format « 18,50 € TTC » et dates au format JJ/MM/AAAA', () => {
    expect(montantTtc(18.5)).toBe('18,50 € TTC');
    expect(montantTtc(48.5)).toBe('48,50 € TTC');
    expect(dateJjMmAaaa('2026-11-27')).toBe('27/11/2026');
    expect(dateJjMmAaaa(null)).toBe('');
    expect(dateHeureFrancaise('2026-11-26T14:05:00Z')).toBe('26/11/2026 à 15:05');
    expect(dateHeureFrancaise(null)).toBe('');
  });

  it('« Continuer » est désactivé tant qu’aucune garantie n’a changé', () => {
    const actuelles = new Set(garantiesSouscrites(GARANTIES));
    expect([...actuelles]).toEqual(['RC', 'PROTECTION_JURIDIQUE']);
    expect(aChange(GARANTIES, actuelles)).toBe(false);
    expect(aChange(GARANTIES, new Set(['RC', 'PROTECTION_JURIDIQUE', 'ASSISTANCE_0KM']))).toBe(true);
    expect(aChange(GARANTIES, new Set(['RC']))).toBe(true);
  });

  it('AC3 : nouvelle cotisation = 47,50 + 3,50 − 2,50 = 48,50 € (même calcul que le serveur)', () => {
    expect(cotisationApres(47.5, GARANTIES, new Set(['RC', 'ASSISTANCE_0KM']))).toBe(48.5);
    expect(cotisationApres(47.5, GARANTIES, new Set(['RC', 'PROTECTION_JURIDIQUE']))).toBe(47.5);
  });

  it('RG3 : la nouvelle cotisation s’applique à la première échéance après la date d’effet, ou à l’échéance annuelle', () => {
    expect(messageApplication(PROJET_A03)).toBe(
      "La nouvelle mensualité s'appliquera à partir de votre première échéance après la date d'effet (05/12/2026), sans prorata.",
    );
    const annuel: ProjetAvenant = { ...PROJET_A03, frequencePaiement: 'ANNUEL', premiereEcheanceConcernee: '2027-03-05' };
    expect(messageApplication(annuel)).toBe(
      "Vous payez à l'année : la nouvelle cotisation s'appliquera à votre prochaine échéance annuelle (05/03/2027).",
    );
    expect(messageApplication({ ...PROJET_A03, premiereEcheanceConcernee: null })).not.toContain('(');
  });
});
