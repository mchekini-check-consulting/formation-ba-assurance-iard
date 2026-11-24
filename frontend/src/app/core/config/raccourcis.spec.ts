import { ContratResume, StatutContrat } from '../model/contrat';
import { CONTRAT_AUTO } from '../model/contrat.exemples';
import { LIVRAISONS, RACCOURCIS, adresseSiLivree, navigationClient, raccourcisDuContrat, tuiles } from './raccourcis';

const contrat = (statut: StatutContrat, produit: 'HABITATION' | 'AUTO' = 'HABITATION'): ContratResume => ({
  ...CONTRAT_AUTO,
  referenceDevis: produit === 'HABITATION' ? '7c9e6679-7425-40de-944b-e07fc1f90ae7' : null,
  numero: 'AE-1',
  produit,
  statut,
});

const actions = (c: ContratResume, reglage = RACCOURCIS): string[] =>
  raccourcisDuContrat(c, reglage).map((r) => r.action);

describe('raccourcis du contrat (US-18, AC2)', () => {
  it('masque les fonctions pas encore livrées', () => {
    expect(actions(contrat('ACTIF'))).toEqual(['detail', 'regenerer']);
    expect(RACCOURCIS.modifierGaranties.livre).toBe(false);
    expect(RACCOURCIS.genererFacture.livre).toBe(false);
    expect(RACCOURCIS.declarerSinistre.livre).toBe(false);
  });

  it('affiche une fonction dès que le réglage la dit livrée, si le statut la permet', () => {
    const reglage = {
      ...RACCOURCIS,
      declarerSinistre: { ...RACCOURCIS.declarerSinistre, livre: true, lien: () => ({ commandes: ['/sinistres'] }) },
    };
    expect(actions(contrat('ACTIF'), reglage)).toEqual(['detail', 'regenerer', 'declarerSinistre']);
    expect(actions(contrat('RESILIE'), reglage)).toEqual(['detail']);
  });

  it('une fonction livrée sans adresse reste masquée', () => {
    const reglage = { ...RACCOURCIS, genererFacture: { ...RACCOURCIS.genererFacture, livre: true } };
    expect(actions(contrat('ACTIF'), reglage)).toEqual(['detail', 'regenerer']);
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

describe('US-21 : raccourci « Régénérer mon contrat »', () => {
  it('est livré et ouvre la fiche du contrat sur sa section « Versions »', () => {
    expect(LIVRAISONS.regenerer).toBe(true);
    const regenerer = raccourcisDuContrat(contrat('ACTIF', 'AUTO')).find((r) => r.action === 'regenerer');
    expect(regenerer?.libelle).toBe('Régénérer mon contrat');
    expect(regenerer?.lien).toEqual({ commandes: ['/espace-client/contrats', 'AE-1'], fragment: 'versions' });
  });

  it('RG2 / RG3 : jamais proposé sur un contrat Suspendu, Impayé, résilié ou en attente', () => {
    for (const statut of ['SUSPENDU', 'IMPAYE', 'RESILIE', 'RESILIATION_EN_COURS', 'RENONCE', 'EN_ATTENTE_EFFET', 'EN_ATTENTE_DE_SIGNATURE'] as const) {
      expect(actions(contrat(statut))).not.toContain('regenerer');
    }
    expect(actions(contrat('AVENANT_EN_ATTENTE'))).toContain('regenerer');
    expect(actions(contrat('ACTIF_JUSTIFICATIFS_ATTENDUS'))).toContain('regenerer');
  });
});

describe('navigation et tuiles (US-18, maquette)', () => {
  const tout = { ...LIVRAISONS, factures: true, sinistres: true, monCompte: true, modifierGaranties: true };
  const adresses = { factures: '/espace-client/factures', sinistres: '/espace-client/sinistres', monCompte: '/compte', modifierGaranties: '/garanties' };

  it('masque Mes factures, Mes sinistres et Mon compte tant que leurs US ne sont pas livrées', () => {
    expect(navigationClient().map((e) => e.libelle)).toEqual(['Tableau de bord', 'Mes contrats', 'Mes devis', 'Mes documents']);
    expect(LIVRAISONS.factures || LIVRAISONS.sinistres || LIVRAISONS.monCompte).toBe(false);
  });

  it('affiche les liens dans l’ordre de la maquette une fois livrés', () => {
    expect(navigationClient(tout, adresses).map((e) => e.libelle)).toEqual([
      'Tableau de bord',
      'Mes contrats',
      'Mes devis',
      'Mes documents',
      'Mes factures',
      'Mes sinistres',
      'Mon compte',
    ]);
    expect(navigationClient(tout, adresses).find((e) => e.libelle === 'Mes sinistres')?.rubrique).toBe('SINISTRES');
  });

  it('une fonction livrée sans adresse reste masquée', () => {
    expect(navigationClient(tout, {}).map((e) => e.libelle)).not.toContain('Mes factures');
    expect(adresseSiLivree('factures', tout, {})).toBeNull();
  });

  it('« Faire un nouveau devis » est toujours proposé, les autres tuiles selon les livraisons', () => {
    expect(tuiles().map((t) => t.libelle)).toEqual(['Faire un nouveau devis']);
    expect(tuiles(tout, adresses).map((t) => t.libelle)).toEqual([
      'Déclarer un sinistre',
      'Modifier mes garanties',
      'Mes factures',
      'Faire un nouveau devis',
    ]);
    expect(adresseSiLivree('sinistres', tout, adresses)).toBe('/espace-client/sinistres');
  });

  it('les raccourcis de contrat suivent le même réglage', () => {
    expect(RACCOURCIS.genererFacture.livre).toBe(LIVRAISONS.factures);
    expect(RACCOURCIS.declarerSinistre.livre).toBe(LIVRAISONS.sinistres);
    expect(RACCOURCIS.detail.libelle).toBe('Voir mon contrat');
  });
});
