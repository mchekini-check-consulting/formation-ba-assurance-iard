import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TarifAutoService, TarifAutoRequete, TarifAutoReponse } from './tarif-auto.service';

describe('TarifAutoService', () => {
  const BASE = '/api/assurance-iard-api/v1/devis/auto/tarif';
  const requete: TarifAutoRequete = {
    refVehiculeId: 1,
    anneePermis: 2010,
    bonusMalus: 0.9,
    kmAnnuels: 11500,
    usage: 'PRIVE',
    stationnement: 'GARAGE_FERME',
  };
  const reponse: TarifAutoReponse = {
    tarifable: true,
    prix: [{ formule: 'TIERS', mensuel: 42.5 }],
  };

  let service: TarifAutoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TarifAutoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envoie la requete et retourne le tarif', () => {
    let resultat: TarifAutoReponse | null | undefined;
    service.calculer(requete).subscribe(r => (resultat = r));
    const req = http.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(requete);
    req.flush(reponse);
    expect(resultat).toEqual(reponse);
  });

  it('retourne null en cas d\'erreur', () => {
    let resultat: TarifAutoReponse | null | undefined;
    service.calculer(requete).subscribe(r => (resultat = r));
    http.expectOne(BASE).flush(null, { status: 503, statusText: 'Indisponible' });
    expect(resultat).toBeNull();
  });
});
