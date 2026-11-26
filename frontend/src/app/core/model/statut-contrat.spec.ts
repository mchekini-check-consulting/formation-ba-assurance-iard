import { StatutContrat } from './contrat';
import { STATUTS_CONTRAT, decrireStatut } from './statut-contrat';

describe('statuts du contrat (US-18, RG3 et AC5)', () => {
  it('connaît les 10 statuts avec leur libellé exact', () => {
    expect(Object.keys(STATUTS_CONTRAT)).toHaveLength(10);
    expect(STATUTS_CONTRAT.EN_ATTENTE_EFFET.libelle).toBe("En attente d'effet");
    expect(STATUTS_CONTRAT.ACTIF_JUSTIFICATIFS_ATTENDUS.libelle).toBe('Actif · justificatifs attendus');
    expect(STATUTS_CONTRAT.RENONCE.libelle).toBe('Renoncé');
    expect(Object.keys(STATUTS_CONTRAT)).not.toContain('SIGNE');
  });

  it.each(Object.entries(STATUTS_CONTRAT))('%s a un texte, un message et « Voir le détail »', (_statut, description) => {
    expect(description.libelle.length).toBeGreaterThan(0);
    expect(description.message.length).toBeGreaterThan(0);
    expect(description.actions).toContain('detail');
  });

  it.each<[StatutContrat, string]>([
    ['EN_ATTENTE_DE_SIGNATURE', 'orange'],
    ['EN_ATTENTE_EFFET', 'bleu'],
    ['ACTIF', 'vert'],
    ['ACTIF_JUSTIFICATIFS_ATTENDUS', 'bleu'],
    ['AVENANT_EN_ATTENTE', 'orange'],
    ['IMPAYE', 'rouge'],
    ['SUSPENDU', 'rouge'],
    ['RESILIATION_EN_COURS', 'gris'],
    ['RESILIE', 'gris'],
    ['RENONCE', 'gris'],
  ])('%s est %s', (statut, ton) => {
    expect(STATUTS_CONTRAT[statut].ton).toBe(ton);
  });

  it('bloque les actions de RG3', () => {
    expect(STATUTS_CONTRAT.EN_ATTENTE_DE_SIGNATURE.actions).not.toContain('declarerSinistre');
    expect(STATUTS_CONTRAT.AVENANT_EN_ATTENTE.actions).not.toContain('modifierGaranties');
    expect(STATUTS_CONTRAT.IMPAYE.actions).not.toContain('modifierGaranties');
    // US-23, AC6 (décision BA) : seul un contrat Actif modifie ses garanties ; AC5 : relire l'avenant en attente.
    expect(STATUTS_CONTRAT.ACTIF_JUSTIFICATIFS_ATTENDUS.actions).not.toContain('modifierGaranties');
    expect(STATUTS_CONTRAT.AVENANT_EN_ATTENTE.actions).toContain('relireAvenant');
    expect(STATUTS_CONTRAT.AVENANT_EN_ATTENTE.message).toContain('conditions actuelles');
    expect(STATUTS_CONTRAT.SUSPENDU.actions).not.toContain('regenerer');
    // US-24, AC1 (décision BA) : un contrat résilié depuis moins de 2 ans reste ouvert à la déclaration.
    expect(STATUTS_CONTRAT.RESILIE.actions).toContain('declarerSinistre');
    expect(STATUTS_CONTRAT.EN_ATTENTE_EFFET.actions).not.toContain('declarerSinistre');
    expect(STATUTS_CONTRAT.RENONCE.actions).not.toContain('declarerSinistre');
    // US-22 : un contrat renoncé garde ses factures des périodes antérieures (décision BA).
    expect(STATUTS_CONTRAT.RENONCE.actions).toEqual(['detail', 'genererFacture']);
    expect(STATUTS_CONTRAT.EN_ATTENTE_DE_SIGNATURE.actions).not.toContain('genererFacture');
    expect(STATUTS_CONTRAT.ACTIF.actions).toHaveLength(5);
  });

  it('US-21, RG2 : « Régénérer » pour tout contrat signé et en vigueur, sauf Suspendu', () => {
    const avecRegeneration = Object.entries(STATUTS_CONTRAT)
      .filter(([, description]) => description.actions.includes('regenerer'))
      .map(([statut]) => statut);
    expect(avecRegeneration).toEqual([
      'EN_ATTENTE_EFFET',
      'ACTIF',
      'ACTIF_JUSTIFICATIFS_ATTENDUS',
      'AVENANT_EN_ATTENTE',
      'IMPAYE',
      'RESILIATION_EN_COURS',
    ]);
  });

  it('affiche en texte un statut inconnu, sans autre action que le détail', () => {
    expect(decrireStatut('NOUVEAU')).toEqual({ libelle: 'NOUVEAU', ton: 'gris', message: '', actions: ['detail'] });
  });
});
