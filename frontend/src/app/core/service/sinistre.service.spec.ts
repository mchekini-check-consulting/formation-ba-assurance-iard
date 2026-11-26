import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MESSAGE_TAILLE, reponsesDe } from '../model/sinistre';
import { BROUILLON_COMPLET, BROUILLON_VOL, TYPES_AUTO } from '../model/sinistre.exemples';
import {
  MESSAGE_ERREUR_TECHNIQUE,
  MESSAGE_INTROUVABLE,
  MESSAGE_STOCKAGE,
  Resultat,
  SinistreService,
} from './sinistre.service';

/** En local, apiBase renvoie /api/assurance-iard-api (relais du serveur de dev, comme le gateway). */
const BASE = '/api/assurance-iard-api';

describe('SinistreService (US-24)', () => {
  let service: SinistreService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SinistreService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('AC1 : contexte de la déclaration', () => {
    let resultat: Resultat<unknown> | undefined;
    service.contexte().subscribe((r) => (resultat = r));
    const corps = { prenom: 'Camille', numeroAssistance: '01', contrats: [] };
    http.expectOne(`${BASE}/sinistres/declaration`).flush(corps);
    expect(resultat).toEqual({ statut: 'ok', valeur: corps });
  });

  it('AC2 : types de sinistres du contrat', () => {
    let resultat: Resultat<unknown> | undefined;
    service.types('AE-RECETTE-A04').subscribe((r) => (resultat = r));
    http.expectOne(`${BASE}/contrats/AE-RECETTE-A04/types-sinistre`).flush(TYPES_AUTO);
    expect(resultat).toEqual({ statut: 'ok', valeur: TYPES_AUTO });
  });

  it('AC8 : Mes sinistres, création et enregistrement du brouillon', () => {
    service.lister().subscribe();
    http.expectOne(`${BASE}/sinistres`).flush([]);

    service.creer('AE-RECETTE-A04', 'VOL').subscribe();
    const creation = http.expectOne(`${BASE}/sinistres`);
    expect(creation.request.method).toBe('POST');
    expect(creation.request.body).toEqual({ numeroContrat: 'AE-RECETTE-A04', type: 'VOL' });
    creation.flush(BROUILLON_VOL);

    service.enregistrer(42, reponsesDe(BROUILLON_COMPLET)).subscribe();
    const enregistrement = http.expectOne(`${BASE}/sinistres/42`);
    expect(enregistrement.request.method).toBe('PUT');
    expect(enregistrement.request.body.heureSurvenance).toBe('08:30');
    enregistrement.flush(BROUILLON_COMPLET);

    service.lire(42).subscribe();
    expect(http.expectOne(`${BASE}/sinistres/42`).request.method).toBe('GET');
  });

  it('AC9 : envoi avec la déclaration sur l’honneur', () => {
    service.envoyer(42, true).subscribe();
    const envoi = http.expectOne(`${BASE}/sinistres/42/envoi`);
    expect(envoi.request.method).toBe('POST');
    expect(envoi.request.body).toEqual({ declarationHonneur: true });
    envoi.flush({});
  });

  it('AC14 : dépôt, suppression et lecture d’un justificatif', () => {
    const fichier = new File(['%PDF-1.7'], 'recepisse.pdf', { type: 'application/pdf' });
    service.deposer(42, 'RECEPISSE_PLAINTE', fichier).subscribe();
    const depot = http.expectOne(`${BASE}/sinistres/42/pieces`);
    expect(depot.request.method).toBe('POST');
    expect((depot.request.body as FormData).get('typePiece')).toBe('RECEPISSE_PLAINTE');
    expect(((depot.request.body as FormData).get('fichier') as File).name).toBe('recepisse.pdf');
    depot.flush(BROUILLON_VOL);

    service.supprimer(42, 7).subscribe();
    expect(http.expectOne(`${BASE}/sinistres/42/pieces/7`).request.method).toBe('DELETE');

    service.telecharger(42, 7).subscribe();
    const lecture = http.expectOne(`${BASE}/sinistres/42/pieces/7`);
    expect(lecture.request.responseType).toBe('blob');
    lecture.flush(new Blob());
  });

  describe('messages d’erreur clairs, jamais techniques', () => {
    const refus = (status: number, corps: object | null): Resultat<unknown> | undefined => {
      let resultat: Resultat<unknown> | undefined;
      service.lire(42).subscribe((r) => (resultat = r));
      http.expectOne(`${BASE}/sinistres/42`).flush(corps, { status, statusText: 'x' });
      return resultat;
    };

    it('le message du serveur (déjà en français) est affiché tel quel', () => {
      expect(refus(400, { detail: 'La date du sinistre ne peut pas être dans le futur.' })).toEqual({
        statut: 'refuse',
        code: 400,
        message: 'La date du sinistre ne peut pas être dans le futur.',
      });
      expect(refus(422, { detail: 'Ce fichier n’est pas accepté : déposez un PDF, un JPG ou un PNG.' })).toMatchObject({
        message: 'Ce fichier n’est pas accepté : déposez un PDF, un JPG ou un PNG.',
      });
    });

    it('une réponse technique est remplacée par un message clair', () => {
      expect(refus(400, { detail: 'Failed to read request' })).toMatchObject({ message: MESSAGE_ERREUR_TECHNIQUE });
      expect(refus(400, { detail: 'Champs invalides', erreurs: [] })).toMatchObject({
        message: MESSAGE_ERREUR_TECHNIQUE,
      });
      expect(refus(500, { detail: 'NullPointerException' })).toMatchObject({ message: MESSAGE_ERREUR_TECHNIQUE });
      expect(refus(0, null)).toMatchObject({ message: MESSAGE_ERREUR_TECHNIQUE });
    });

    it('AC15, AC16 et brouillon expiré : messages par défaut', () => {
      expect(refus(413, { detail: 'Fichier trop volumineux.' })).toMatchObject({ message: MESSAGE_TAILLE });
      expect(refus(503, { detail: 'x' })).toMatchObject({ code: 503, message: MESSAGE_STOCKAGE });
      expect(refus(404, null)).toMatchObject({ message: MESSAGE_INTROUVABLE });
    });
  });
});
