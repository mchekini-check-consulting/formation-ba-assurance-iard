import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { ContratService, ResultatGeneration, ResultatObtention, ResultatSignature } from './contrat.service';
import { DemandeContrat } from '../model/contrat';

const UNE_DEMANDE: DemandeContrat = {
  referenceDevis: '7c9e6679-7425-40de-944b-e07fc1f90ae7',
  adresseLogement: '12 rue des Lilas',
  codePostal: '75001',
  ville: 'Paris',
  typeLogement: 'appartement',
  surface: 45,
  franchise: 300,
  cotisationMensuelle: 18.5,
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
  ibanDebut: 'FR76',
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
      .flush({ ...UN_CONTRAT, statut: 'SIGNE' });
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
});
