import {
  MESSAGE_DATE_FUTURE,
  MESSAGE_FORMAT,
  MESSAGE_NOMBRE,
  MESSAGE_TAILLE,
  ReponsesSinistre,
  aujourdhuiIso,
  declarantsPossibles,
  erreursCirconstances,
  heureFrancaise,
  jourFrancais,
  lienReprise,
  lienTelephone,
  messageDelai,
  messageNonCouvert,
  proposeDepannage,
  questionsDuType,
  refusFichier,
  reponsesDe,
  taille,
} from './sinistre';
import { BROUILLON_COMPLET, BROUILLON_VOL } from './sinistre.exemples';

const COMPLETES: ReponsesSinistre = reponsesDe(BROUILLON_COMPLET);

describe('US-24 : règles de l’écran de déclaration', () => {
  it('AC8 : un brouillon se reprend à son étape ; un dossier envoyé ouvre ses justificatifs', () => {
    expect(lienReprise({ id: 7, statut: 'BROUILLON', etape: 1 })).toEqual(['/espace-client/sinistres', '7', 'type']);
    expect(lienReprise({ id: 7, statut: 'BROUILLON', etape: 2 })).toEqual([
      '/espace-client/sinistres',
      '7',
      'circonstances',
    ]);
    expect(lienReprise({ id: 7, statut: 'BROUILLON', etape: 9 })[2]).toBe('justificatifs');
    expect(lienReprise({ id: 7, statut: 'DECLARE', etape: 3 })[2]).toBe('justificatifs');
  });

  it('AC13 : le conducteur secondaire n’est proposé qu’en auto', () => {
    expect(declarantsPossibles('AUTO').map((d) => d.valeur)).toEqual(['SOUSCRIPTEUR', 'CONDUCTEUR', 'PROCHE']);
    expect(declarantsPossibles('HABITATION').map((d) => d.valeur)).toEqual(['SOUSCRIPTEUR', 'PROCHE']);
  });

  it('AC4 : les questions dépendent du type ; aucune pour « Autre » (AC12)', () => {
    expect(questionsDuType('AUTO', 'VOL').map((q) => q.libelle)).toContain('Avez-vous déposé plainte ?');
    expect(questionsDuType('HABITATION', 'DEGAT_EAUX').map((q) => q.libelle)).toContain('D’où vient l’eau ?');
    expect(questionsDuType('AUTO', 'BRIS_GLACE')[0].options).toContain('Pare-brise');
    expect(questionsDuType('HABITATION', 'BRIS_GLACE')[0].options).toContain('Baie vitrée');
    expect(questionsDuType('AUTO', 'AUTRE')).toEqual([]);
    expect(questionsDuType('AUTO', 'INCONNU')).toEqual([]);
  });

  it('AC10 : « Demander un dépannage » pour un accident auto seulement', () => {
    expect(proposeDepannage('AUTO', 'ACCIDENT_TIERS')).toBe(true);
    expect(proposeDepannage('AUTO', 'ACCIDENT_SANS_TIERS')).toBe(true);
    expect(proposeDepannage('AUTO', 'VOL')).toBe(false);
    expect(proposeDepannage('HABITATION', 'ACCIDENT_TIERS')).toBe(false);
    expect(proposeDepannage('AUTO', null)).toBe(false);
    expect(lienTelephone('01 23 45 67 89')).toBe('tel:0123456789');
  });

  it('AC3 et AC6 : messages neutres, jamais bloquants', () => {
    expect(messageNonCouvert('Tiers')).toContain('pas couvert par votre formule Tiers');
    expect(messageNonCouvert('Tiers')).toContain('Vous pouvez quand même le déclarer');
    expect(messageDelai('2 jours ouvrés')).toContain('est de 2 jours ouvrés');
    expect(messageDelai(null)).toContain('est dépassé');
    expect(messageDelai(null)).toContain('expliquez-nous');
  });

  it('AC15 : format, taille et nombre de fichiers contrôlés avant l’envoi', () => {
    expect(refusFichier({ name: 'photo-degats.jpg', size: 2 * 1024 * 1024 }, 0)).toBeNull();
    expect(refusFichier({ name: 'scan.PDF', size: 1000 }, 0)).toBeNull();
    expect(refusFichier({ name: 'constat.pdf', size: 12 * 1024 * 1024 }, 0)).toBe(MESSAGE_TAILLE);
    expect(refusFichier({ name: 'facture.exe', size: 1000 }, 0)).toBe(MESSAGE_FORMAT);
    expect(refusFichier({ name: 'sans-extension', size: 1000 }, 0)).toBe(MESSAGE_FORMAT);
    expect(refusFichier({ name: 'photo.jpg', size: 1000 }, 20)).toBe(MESSAGE_NOMBRE);
  });

  it('AC4 : des réponses complètes n’ont aucune erreur', () => {
    expect(erreursCirconstances(COMPLETES, '2026-11-26', false)).toEqual({});
  });

  it('AC4, AC11, AC13 : chaque réponse obligatoire manquante a son message', () => {
    const erreurs = erreursCirconstances(reponsesDe(BROUILLON_VOL), '2026-11-26', false);
    expect(Object.keys(erreurs).sort()).toEqual(
      ['blesses', 'dateSurvenance', 'declarant', 'description', 'lieu', 'tiersImplique'].sort(),
    );
    expect(erreurs.lieu).toBe('Indiquez le lieu du sinistre.');

    expect(erreursCirconstances({ ...COMPLETES, blesses: true, nombreBlesses: null }, '2026-11-26', false)).toEqual({
      nombreBlesses: 'Indiquez le nombre de blessés.',
    });
    expect(Object.keys(erreursCirconstances({ ...COMPLETES, declarant: 'PROCHE' }, '2026-11-26', false))).toEqual([
      'declarantNom',
      'declarantLien',
    ]);
  });

  it('AC4 : la date ne peut pas être dans le futur ; AC6 : l’explication est demandée en retard', () => {
    expect(erreursCirconstances({ ...COMPLETES, dateSurvenance: '2026-11-27' }, '2026-11-26', false)).toEqual({
      dateSurvenance: MESSAGE_DATE_FUTURE,
    });
    expect(Object.keys(erreursCirconstances(COMPLETES, '2026-11-26', true))).toEqual(['explicationRetard']);
  });

  it('les réponses renvoyées au serveur gardent l’heure au format HH:MM', () => {
    expect(COMPLETES.heureSurvenance).toBe('08:30');
    expect(COMPLETES.details).not.toBe(BROUILLON_COMPLET.details);
  });

  it('dates JJ/MM/AAAA, heures HH:MM, tailles lisibles', () => {
    expect(jourFrancais('2026-11-24')).toBe('24/11/2026');
    expect(jourFrancais(null)).toBe('');
    expect(heureFrancaise('08:30:00')).toBe('08:30');
    expect(heureFrancaise(null)).toBe('');
    expect(aujourdhuiIso(new Date('2026-11-26T23:30:00Z'))).toBe('2026-11-27');
    expect(taille(2048)).toBe('2 Ko');
    expect(taille(2.5 * 1024 * 1024)).toBe('2,5 Mo');
  });
});
