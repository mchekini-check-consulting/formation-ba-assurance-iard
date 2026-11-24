import { ContratResume, StatutContrat } from '../model/contrat';
import { RACCOURCIS, raccourcisDuContrat } from './raccourcis';

const contrat = (statut: StatutContrat, produit: 'HABITATION' | 'AUTO' = 'HABITATION'): ContratResume => ({
  referenceDevis: produit === 'HABITATION' ? '7c9e6679-7425-40de-944b-e07fc1f90ae7' : null,
  numero: 'AE-1',
  produit,
  statut,
  bienAssure: null,
  cotisationMensuelle: 18.5,
  dateEffet: '2026-01-10',
  dateEcheance: '2027-01-10',
  dateFin: null,
});

const actions = (c: ContratResume, reglage = RACCOURCIS): string[] =>
  raccourcisDuContrat(c, reglage).map((r) => r.action);

describe('raccourcis du contrat (US-18, AC2)', () => {
  it('masque les fonctions pas encore livrées', () => {
    expect(actions(contrat('ACTIF'))).toEqual(['detail']);
    expect(RACCOURCIS.modifierGaranties.livre).toBe(false);
    expect(RACCOURCIS.regenerer.livre).toBe(false);
    expect(RACCOURCIS.genererFacture.livre).toBe(false);
    expect(RACCOURCIS.declarerSinistre.livre).toBe(false);
  });

  it('affiche une fonction dès que le réglage la dit livrée, si le statut la permet', () => {
    const reglage = {
      ...RACCOURCIS,
      declarerSinistre: { ...RACCOURCIS.declarerSinistre, livre: true, lien: () => ({ commandes: ['/sinistres'] }) },
    };
    expect(actions(contrat('ACTIF'), reglage)).toEqual(['detail', 'declarerSinistre']);
    expect(actions(contrat('RESILIE'), reglage)).toEqual(['detail']);
  });

  it('une fonction livrée sans adresse reste masquée', () => {
    const reglage = { ...RACCOURCIS, regenerer: { ...RACCOURCIS.regenerer, livre: true } };
    expect(actions(contrat('ACTIF'), reglage)).toEqual(['detail']);
  });

  it('« Voir le détail » ouvre la fiche par numéro', () => {
    expect(raccourcisDuContrat(contrat('RENONCE'))[0].lien).toEqual({ commandes: ['/espace-client/contrats', 'AE-1'] });
  });

  it('« Signer » rouvre la signature du sprint 1 pour un contrat habitation en attente de signature', () => {
    const signer = raccourcisDuContrat(contrat('EN_ATTENTE_DE_SIGNATURE')).find((r) => r.action === 'signer');
    expect(signer?.lien).toEqual({
      commandes: ['/devis/contrat'],
      parametres: { reference: '7c9e6679-7425-40de-944b-e07fc1f90ae7' },
    });
  });

  it('« Signer » est masqué en auto (US-33 pas livrée) et sans référence de devis', () => {
    expect(actions(contrat('EN_ATTENTE_DE_SIGNATURE', 'AUTO'))).toEqual(['detail']);
    expect(actions({ ...contrat('EN_ATTENTE_DE_SIGNATURE'), referenceDevis: null })).toEqual(['detail']);
  });
});
