import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  ContratService,
  MESSAGE_IMPOSSIBLE,
  MESSAGE_TROP_TOT,
  ResultatFiche,
  ResultatRegeneration,
  ResultatGeneration,
  ResultatObtention,
  ResultatSignature,
} from './contrat.service';
import { DemandeContrat } from '../model/contrat';

const UNE_DEMANDE: DemandeContrat = {
  referenceDevis: '7c9e6679-7425-40de-944b-e07fc1f90ae7',
  adresseLogement: '12 rue des Lilas',
  codePostal: '75001',
  ville: 'Paris',
  typeLogement: 'appartement',
  surface: 45,
  franchise: 300,
  resiliationHamon: false,
};

const UN_CONTRAT = {
  numero: 'AE-2026-000001',
  statut: 'EN_ATTENTE_DE_SIGNATURE',
  civilite: 'M.',
  prenom: 'Jean',
  nom: 'Dupont',
  telephone: '0612345678',
  adresseLogement: '12 rue des Lilas',
  codePostal: '75001',
  ville: 'Paris',
  typeLogement: 'appartement',
  surface: 45,
  formule: 'confort',
  options: ['assistance-depannage'],
  franchise: 300,
  cotisationMensuelle: 18.5,
  cotisationAnnuelle: 222.0,
  jourPrelevement: 10,
  ibanFin: '7890',
  rum: 'AE-2026-ABCDEF12',
  datePremierPrelevement: '2026-12-10',
  dateEffet: '2026-12-01',
  resiliationHamon: false,
  dateCreation: '2026-11-05',
};

describe('ContratService', () => {
  let service: ContratService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ContratService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('génère le contrat avec succès', () => {
    const resultats: ResultatGeneration[] = [];
    service.generer(UNE_DEMANDE).subscribe((r) => resultats.push(r));
    http.expectOne((req) => req.method === 'POST' && req.url.includes('/devis/contrat')).flush(UN_CONTRAT);
    expect(resultats).toEqual([{ statut: 'genere', contrat: UN_CONTRAT }]);
  });

  it.each([
    [401, 'session-expiree'],
    [422, 'mandat-non-signe'],
    [500, 'erreur-technique'],
  ])('génération refusée (%i) : %s', (status, statut) => {
    const resultats: ResultatGeneration[] = [];
    service.generer(UNE_DEMANDE).subscribe((r) => resultats.push(r));
    http.expectOne((req) => req.method === 'POST').flush({}, { status, statusText: 'Erreur' });
    expect(resultats).toEqual([{ statut }]);
  });

  it('obtient le contrat existant', () => {
    const resultats: ResultatObtention[] = [];
    service.obtenir('7c9e6679-7425-40de-944b-e07fc1f90ae7').subscribe((r) => resultats.push(r));
    http
      .expectOne((req) => req.method === 'GET' && req.url.includes('/devis/contrat/'))
      .flush(UN_CONTRAT);
    expect(resultats).toEqual([{ statut: 'obtenu', contrat: UN_CONTRAT }]);
  });

  it.each([
    [401, 'session-expiree'],
    [404, 'introuvable'],
    [500, 'erreur-technique'],
  ])('obtention refusée (%i) : %s', (status, statut) => {
    const resultats: ResultatObtention[] = [];
    service.obtenir('ref-1').subscribe((r) => resultats.push(r));
    http
      .expectOne((req) => req.method === 'GET')
      .flush({}, { status, statusText: 'Erreur' });
    expect(resultats).toEqual([{ statut }]);
  });

  it('signe le contrat avec succès', () => {
    const resultats: ResultatSignature[] = [];
    service.signer('7c9e6679-7425-40de-944b-e07fc1f90ae7', '123456').subscribe((r) => resultats.push(r));
    http
      .expectOne((req) => req.method === 'POST' && req.url.includes('/signature'))
      .flush({ ...UN_CONTRAT, statut: 'ACTIF' });
    expect(resultats[0].statut).toBe('signe');
  });

  it.each([
    [401, { statut: 'session-expiree' }],
    [422, { statut: 'code-incorrect', essaisRestants: null }],
    [409, { statut: 'essais-epuises' }],
    [410, { statut: 'code-expire' }],
    [500, { statut: 'erreur-technique' }],
  ])('signature refusee (%i)', (status, attendu) => {
    const resultats: ResultatSignature[] = [];
    service.signer('ref-1', '999999').subscribe((r) => resultats.push(r));
    http
      .expectOne((req) => req.method === 'POST' && req.url.includes('/signature'))
      .flush({}, { status: status as number, statusText: 'Erreur' });
    expect(resultats).toEqual([attendu]);
  });

  describe('US-18 : fiche par numéro', () => {
    it('lit la fiche du contrat', () => {
      const resultats: ResultatFiche[] = [];
      service.obtenirFiche('AE-RECETTE-A03').subscribe((r) => resultats.push(r));
      http.expectOne('/api/assurance-iard-api/contrats/AE-RECETTE-A03').flush({ numero: 'AE-RECETTE-A03' });
      expect(resultats[0]).toEqual({ statut: 'obtenu', fiche: { numero: 'AE-RECETTE-A03' } });
    });

    it.each([
      [404, 'introuvable'],
      [500, 'erreur-technique'],
    ])('erreur %i', (status, statut) => {
      const resultats: ResultatFiche[] = [];
      service.obtenirFiche('AE-X').subscribe((r) => resultats.push(r));
      http.expectOne('/api/assurance-iard-api/contrats/AE-X').flush({}, { status, statusText: 'Erreur' });
      expect(resultats).toEqual([{ statut }]);
    });
  });

  describe('US-21 : régénération des conditions particulières', () => {
    const URL = '/api/assurance-iard-api/contrats/AE-RECETTE-A03/conditions-particulieres';
    let resultats: ResultatRegeneration[];
    beforeEach(() => (resultats = []));

    it('demande la nouvelle version par POST', () => {
      service.regenerer('AE-RECETTE-A03').subscribe((r) => resultats.push(r));
      const requete = http.expectOne(URL);
      expect(requete.request.method).toBe('POST');
      requete.flush({ documentId: 55, version: 3, dateCreation: null, disponible: true, emailEnvoye: true });
      expect(resultats).toEqual([
        { statut: 'regenere', regeneration: { documentId: 55, version: 3, dateCreation: null, disponible: true, emailEnvoye: true } },
      ]);
    });

    it('429 : trop rapproché, avec le message du serveur', () => {
      service.regenerer('AE-RECETTE-A03').subscribe((r) => resultats.push(r));
      http.expectOne(URL).flush({ detail: 'Réessayez dans une minute.' }, { status: 429, statusText: 'x' });
      expect(resultats).toEqual([{ statut: 'trop-tot', message: 'Réessayez dans une minute.' }]);
    });

    it('422 : statut qui ne permet pas la régénération, message par défaut sans détail', () => {
      service.regenerer('AE-RECETTE-A03').subscribe((r) => resultats.push(r));
      http.expectOne(URL).flush(null, { status: 422, statusText: 'x' });
      expect(resultats).toEqual([{ statut: 'impossible', message: MESSAGE_IMPOSSIBLE }]);
    });

    it('429 sans détail : message par défaut', () => {
      service.regenerer('AE-RECETTE-A03').subscribe((r) => resultats.push(r));
      http.expectOne(URL).flush(null, { status: 429, statusText: 'x' });
      expect(resultats).toEqual([{ statut: 'trop-tot', message: MESSAGE_TROP_TOT }]);
    });

    it('autre erreur : erreur technique', () => {
      service.regenerer('AE-RECETTE-A03').subscribe((r) => resultats.push(r));
      http.expectOne(URL).flush({}, { status: 500, statusText: 'x' });
      expect(resultats).toEqual([{ statut: 'erreur-technique' }]);
    });
  });
});
