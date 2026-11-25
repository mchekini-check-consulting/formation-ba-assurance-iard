import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FactureGeneree, MesFactures } from '../model/facture';
import {
  FacturesService,
  MESSAGE_FACTURE_IMPOSSIBLE,
  MESSAGE_PERIODE_INCONNUE,
  ResultatFacture,
} from './factures.service';

describe('FacturesService (US-22)', () => {
  let service: FacturesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(FacturesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const GENEREE: FactureGeneree = {
    facture: {
      numero: 'FAC-2026-000042',
      periodeDebut: '2026-11-10',
      periodeFin: '2026-12-09',
      montantHt: 16.97,
      taxe: 1.53,
      montantTtc: 18.5,
      genereeLe: '2026-11-25T10:00:00Z',
      documentId: 7,
    },
    disponible: true,
    existante: false,
  };

  it('AC1 : lit Mes factures par le gateway (relais local /api/assurance-iard-api)', () => {
    let resultat: MesFactures | null | undefined;
    service.lister().subscribe((r) => (resultat = r));
    const corps: MesFactures = { contrats: [], aucuneCotisation: true };
    http.expectOne('/api/assurance-iard-api/espace-client/factures').flush(corps);
    expect(resultat).toEqual(corps);
  });

  it('AC1 : null si les factures n’ont pas pu être chargées', () => {
    let resultat: MesFactures | null | undefined;
    service.lister().subscribe((r) => (resultat = r));
    http.expectOne('/api/assurance-iard-api/espace-client/factures').flush(null, { status: 500, statusText: 'x' });
    expect(resultat).toBeNull();
  });

  const generer = (): ResultatFacture[] => {
    const resultats: ResultatFacture[] = [];
    service.generer('AE 1', 'ECHEANCE', '2026-11-10').subscribe((r) => resultats.push(r));
    return resultats;
  };

  it('AC2 : envoie la période et la date, jamais le montant', () => {
    const resultats = generer();
    const requete = http.expectOne('/api/assurance-iard-api/contrats/AE%201/factures');
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body).toEqual({ periode: 'ECHEANCE', date: '2026-11-10' });
    requete.flush(GENEREE);
    expect(resultats).toEqual([{ statut: 'genere', generation: GENEREE }]);
  });

  it('422 : génération impossible, avec le message du serveur ou le message par défaut', () => {
    const resultats = generer();
    http
      .expectOne('/api/assurance-iard-api/contrats/AE%201/factures')
      .flush({ detail: 'Pas pour ce contrat.' }, { status: 422, statusText: 'x' });
    expect(resultats).toEqual([{ statut: 'impossible', message: 'Pas pour ce contrat.' }]);

    const suivants = generer();
    http.expectOne('/api/assurance-iard-api/contrats/AE%201/factures').flush(null, { status: 422, statusText: 'x' });
    expect(suivants).toEqual([{ statut: 'impossible', message: MESSAGE_FACTURE_IMPOSSIBLE }]);
  });

  it('404 : la période n’est plus dans l’échéancier', () => {
    const resultats = generer();
    http.expectOne('/api/assurance-iard-api/contrats/AE%201/factures').flush(null, { status: 404, statusText: 'x' });
    expect(resultats).toEqual([{ statut: 'introuvable', message: MESSAGE_PERIODE_INCONNUE }]);
  });

  it('une autre erreur est technique', () => {
    const resultats = generer();
    http.expectOne('/api/assurance-iard-api/contrats/AE%201/factures').flush(null, { status: 500, statusText: 'x' });
    expect(resultats).toEqual([{ statut: 'erreur-technique' }]);
  });
});
