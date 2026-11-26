import { ContratResume, StatutContrat } from '../model/contrat';
import { CONTRAT_AUTO } from '../model/contrat.exemples';
import {
  LIVRAISONS,
  RACCOURCIS,
  adresseSiLivree,
  navigationClient,
  raccourcisDuContrat,
  resilieDepuisMoinsDe2Ans,
  tuiles,
} from './raccourcis';

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
  it('affiche les fonctions livrées (US-24 : Déclarer un sinistre est livrée)', () => {
    expect(actions(contrat('ACTIF'))).toEqual([
      'detail',
      'modifierGaranties',
      'regenerer',
      'genererFacture',
      'declarerSinistre',
    ]);
    expect(RACCOURCIS.modifierGaranties.livre).toBe(true);
    expect(RACCOURCIS.genererFacture.livre).toBe(true);
    expect(RACCOURCIS.declarerSinistre.livre).toBe(true);
  });

  it('US-23, AC6 : « Modifier mes garanties » seulement pour un contrat Actif, vers ses garanties', () => {
    const modifier = raccourcisDuContrat(contrat('ACTIF')).find((r) => r.action === 'modifierGaranties');
    expect(modifier?.libelle).toBe('Modifier mes garanties');
    expect(modifier?.lien).toEqual({ commandes: ['/espace-client/contrats', 'AE-1', 'garanties'] });
    for (const statut of [
      'EN_ATTENTE_DE_SIGNATURE',
      'EN_ATTENTE_EFFET',
      'ACTIF_JUSTIFICATIFS_ATTENDUS',
      'AVENANT_EN_ATTENTE',
      'IMPAYE',
      'SUSPENDU',
      'RESILIATION_EN_COURS',
      'RESILIE',
      'RENONCE',
    ] as const) {
      expect(actions(contrat(statut))).not.toContain('modifierGaranties');
    }
  });

  it('US-23, AC5 : un contrat « Avenant en attente » propose de relire et accepter son avenant', () => {
    const relire = raccourcisDuContrat(contrat('AVENANT_EN_ATTENTE')).find((r) => r.action === 'relireAvenant');
    expect(relire?.libelle).toBe('Relire et accepter mon avenant');
    expect(relire?.lien).toEqual({ commandes: ['/espace-client/contrats', 'AE-1', 'avenant'] });
    expect(actions(contrat('ACTIF'))).not.toContain('relireAvenant');
  });

  it('affiche une fonction dès que le réglage la dit livrée, si le statut la permet', () => {
    const reglage = {
      ...RACCOURCIS,
      declarerSinistre: { ...RACCOURCIS.declarerSinistre, livre: true, lien: () => ({ commandes: ['/sinistres'] }) },
    };
    expect(actions(contrat('ACTIF'), reglage)).toEqual([
      'detail',
      'modifierGaranties',
      'regenerer',
      'genererFacture',
      'declarerSinistre',
    ]);
    expect(actions(contrat('RENONCE'), reglage)).toEqual(['detail', 'genererFacture']);
  });

  it('une fonction livrée sans adresse reste masquée', () => {
    const reglage = { ...RACCOURCIS, declarerSinistre: { ...RACCOURCIS.declarerSinistre, lien: undefined } };
    expect(actions(contrat('ACTIF'), reglage)).toEqual(['detail', 'modifierGaranties', 'regenerer', 'genererFacture']);
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

  it('RG2 : jamais proposé sur un contrat Suspendu, pas encore signé, Résilié ou Renoncé', () => {
    for (const statut of ['SUSPENDU', 'EN_ATTENTE_DE_SIGNATURE', 'RESILIE', 'RENONCE'] as const) {
      expect(actions(contrat(statut))).not.toContain('regenerer');
    }
  });

  it('RG2 : proposé pour tout autre contrat signé et en vigueur (Impayé, en attente d’effet…)', () => {
    for (const statut of ['EN_ATTENTE_EFFET', 'ACTIF', 'ACTIF_JUSTIFICATIFS_ATTENDUS', 'AVENANT_EN_ATTENTE', 'IMPAYE', 'RESILIATION_EN_COURS'] as const) {
      expect(actions(contrat(statut))).toContain('regenerer');
    }
  });
});

describe('navigation et tuiles (US-18, maquette)', () => {
  const tout = { ...LIVRAISONS, factures: true, sinistres: true, monCompte: true, modifierGaranties: true };
  const adresses = {
    factures: '/espace-client/factures',
    sinistres: '/espace-client/sinistres',
    declarerSinistre: '/espace-client/sinistres/declarer',
    monCompte: '/compte',
    modifierGaranties: '/garanties',
  };

  it('masque Mon compte tant que son US n’est pas livrée ; US-22 et US-24 : Mes factures et Mes sinistres sont livrées', () => {
    expect(navigationClient().map((e) => e.libelle)).toEqual([
      'Tableau de bord',
      'Mes contrats',
      'Mes devis',
      'Mes documents',
      'Mes factures',
      'Mes sinistres',
    ]);
    expect(navigationClient().find((e) => e.libelle === 'Mes factures')).toEqual({
      libelle: 'Mes factures',
      adresse: '/espace-client/factures',
      rubrique: 'CONTRATS',
    });
    expect(navigationClient().find((e) => e.libelle === 'Mes sinistres')).toEqual({
      libelle: 'Mes sinistres',
      adresse: '/espace-client/sinistres',
      rubrique: 'SINISTRES',
    });
    expect(LIVRAISONS.monCompte).toBe(false);
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
    // US-23 : « Modifier mes garanties » ouvre Mes contrats (choix du contrat).
    expect(tuiles().map((t) => t.libelle)).toEqual([
      'Déclarer un sinistre',
      'Modifier mes garanties',
      'Mes factures',
      'Faire un nouveau devis',
    ]);
    expect(tuiles().find((t) => t.libelle === 'Modifier mes garanties')?.adresse).toBe('/espace-client/contrats');
    // US-24 : la tuile ouvre l'étape 1 de la déclaration.
    expect(tuiles().find((t) => t.libelle === 'Déclarer un sinistre')?.adresse).toBe('/espace-client/sinistres/declarer');
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
    expect(RACCOURCIS.declarerSinistre.livre).toBe(LIVRAISONS.declarerSinistre);
    expect(RACCOURCIS.detail.libelle).toBe('Voir mon contrat');
  });
});

describe('US-22 : raccourci « Générer une facture » et lien « Voir mes factures »', () => {
  it('ouvre Mes factures sur le contrat (ancre contrat-<numéro>)', () => {
    const facture = raccourcisDuContrat(contrat('ACTIF', 'AUTO')).find((r) => r.action === 'genererFacture');
    expect(facture?.libelle).toBe('Générer une facture');
    expect(facture?.lien).toEqual({ commandes: ['/espace-client/factures'], fragment: 'contrat-AE-1' });
    expect(adresseSiLivree('factures')).toBe('/espace-client/factures');
  });

  it('jamais proposé pour un contrat en attente de signature ; proposé pour les autres statuts', () => {
    expect(actions(contrat('EN_ATTENTE_DE_SIGNATURE'))).not.toContain('genererFacture');
    for (const statut of [
      'EN_ATTENTE_EFFET',
      'ACTIF',
      'ACTIF_JUSTIFICATIFS_ATTENDUS',
      'AVENANT_EN_ATTENTE',
      'IMPAYE',
      'SUSPENDU',
      'RESILIATION_EN_COURS',
      'RESILIE',
      'RENONCE',
    ] as const) {
      expect(actions(contrat(statut))).toContain('genererFacture');
    }
  });

  it('sans adresse, le raccourci est masqué', () => {
    const reglage = { ...RACCOURCIS, genererFacture: { ...RACCOURCIS.genererFacture, lien: () => null } };
    expect(actions(contrat('ACTIF'), reglage)).not.toContain('genererFacture');
  });
});

describe('US-24 : raccourci « Déclarer un sinistre »', () => {
  const aujourdhui = new Date('2026-11-26T12:00:00');

  it('AC1 : ouvre l’étape 1 sur ce contrat, choisi par le client', () => {
    const declarer = raccourcisDuContrat(contrat('ACTIF', 'AUTO')).find((r) => r.action === 'declarerSinistre');
    expect(declarer?.libelle).toBe('Déclarer un sinistre');
    expect(declarer?.lien).toEqual({ commandes: ['/espace-client/sinistres/declarer'], parametres: { contrat: 'AE-1' } });
  });

  it('AC1 : proposé pour les contrats Actif, Impayé, Suspendu, Résiliation en cours, avenant ou justificatifs attendus', () => {
    for (const statut of [
      'ACTIF',
      'ACTIF_JUSTIFICATIFS_ATTENDUS',
      'AVENANT_EN_ATTENTE',
      'IMPAYE',
      'SUSPENDU',
      'RESILIATION_EN_COURS',
    ] as const) {
      expect(actions(contrat(statut))).toContain('declarerSinistre');
    }
    for (const statut of ['EN_ATTENTE_DE_SIGNATURE', 'EN_ATTENTE_EFFET', 'RENONCE'] as const) {
      expect(actions(contrat(statut))).not.toContain('declarerSinistre');
    }
  });

  it('AC1 : un contrat résilié l’est encore moins de 2 ans après sa date de fin', () => {
    const recent = new Date();
    recent.setMonth(recent.getMonth() - 3);
    const dateFin = recent.toISOString().slice(0, 10);
    expect(actions({ ...contrat('RESILIE'), dateFin })).toContain('declarerSinistre');
    expect(actions({ ...contrat('RESILIE'), dateFin: '2020-01-31' })).not.toContain('declarerSinistre');
    expect(actions({ ...contrat('RESILIE'), dateFin: null })).not.toContain('declarerSinistre');
  });

  it('règle des 2 ans, comme le serveur', () => {
    expect(resilieDepuisMoinsDe2Ans('2024-11-27', aujourdhui)).toBe(true);
    expect(resilieDepuisMoinsDe2Ans('2024-11-25', aujourdhui)).toBe(false);
    expect(resilieDepuisMoinsDe2Ans(undefined, aujourdhui)).toBe(false);
  });
});
