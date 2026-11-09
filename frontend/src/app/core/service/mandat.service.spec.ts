import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DemandeSignature, MandatService, ResultatPreparation, ResultatSignature } from './mandat.service';

describe('MandatService', () => {
  const url = '/api/assurance-iard-api/devis/mandats';
  const demande: DemandeSignature = {
    referenceDevis: 'ref-1',
    titulaire: 'Marie Durand',
    iban: 'FR7630006000011234567890189',
    dateDebut: '2026-12-01',
    jourPrelevement: 10,
    autorisation: true,
  };

  let service: MandatService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MandatService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const preparer = (): ResultatPreparation[] => {
    const resultats: ResultatPreparation[] = [];
    service.preparer('ref-1').subscribe((r) => resultats.push(r));
    return resultats;
  };

  const signer = (): ResultatSignature[] => {
    const resultats: ResultatSignature[] = [];
    service.signer(demande).subscribe((r) => resultats.push(r));
    return resultats;
  };

  it('prépare le mandat', () => {
    const resultats = preparer();
    const mandat = { creancier: "Assur'Easy", ics: 'FR00ZZZ000000', rum: 'AE-2026-ABCDEFGH' };
    const requete = http.expectOne(url);
    expect(requete.request.body).toEqual({ referenceDevis: 'ref-1' });
    requete.flush(mandat);
    expect(resultats).toEqual([{ statut: 'prepare', mandat }]);
  });

  it.each([
    [401, 'session-expiree'],
    [404, 'erreur-technique'],
    [500, 'erreur-technique'],
  ])('préparation refusée (%i) : %s', (status, statut) => {
    const resultats = preparer();
    http.expectOne(url).flush(null, { status, statusText: 'Erreur' });
    expect(resultats).toEqual([{ statut }]);
  });

  it('signe le mandat', () => {
    const resultats = signer();
    const requete = http.expectOne(`${url}/signature`);
    expect(requete.request.body).toEqual(demande);
    requete.flush({ rum: 'AE-2026-ABCDEFGH', datePremierPrelevement: '2026-12-10' });
    expect(resultats).toEqual([{ statut: 'signe' }]);
  });

  it('signale une session expirée', () => {
    const resultats = signer();
    http.expectOne(`${url}/signature`).flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(resultats).toEqual([{ statut: 'session-expiree' }]);
  });

  it('transmet le champ refusé et sa raison', () => {
    const resultats = signer();
    http
      .expectOne(`${url}/signature`)
      .flush({ champ: 'iban', detail: 'Refusé.' }, { status: 422, statusText: 'Unprocessable' });
    expect(resultats).toEqual([{ statut: 'refuse', champ: 'iban', message: 'Refusé.' }]);
  });

  it('signale toute autre erreur comme technique', () => {
    const resultats = signer();
    http.expectOne(`${url}/signature`).flush(null, { status: 500, statusText: 'Erreur' });
    expect(resultats).toEqual([{ statut: 'erreur-technique' }]);
  });
});
