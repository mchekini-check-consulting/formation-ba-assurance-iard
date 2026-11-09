import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { logementVide } from '../model/devis';
import { Tarif } from '../model/tarif';
import { tarifEstime } from '../model/tarif.exemples';
import { TarifService } from './tarif.service';

describe('TarifService', () => {
  const BASE = '/api/assurance-iard-api/devis';
  const demande = { logement: logementVide() };
  let service: TarifService;
  let http: HttpTestingController;
  let resultat: Tarif | null | undefined;
  const garder = (tarif: Tarif | null): void => {
    resultat = tarif;
  };

  beforeEach(() => {
    resultat = undefined;
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TarifService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('estime le tarif des étapes 1 et 2', () => {
    service.estimer(demande).subscribe(garder);
    const requete = http.expectOne(`${BASE}/tarifs`);
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body).toEqual(demande);
    requete.flush(tarifEstime());
    expect(resultat).toEqual(tarifEstime());
  });

  it('retient le tarif du devis à l’étape 3', () => {
    service.tarifer('ref-1', demande).subscribe(garder);
    const requete = http.expectOne(`${BASE}/ref-1/tarif`);
    expect(requete.request.method).toBe('POST');
    requete.flush(tarifEstime());
    expect(resultat).toEqual(tarifEstime());
  });

  it('relit le tarif d’origine d’un devis repris', () => {
    service.tarifRetenu('ref-1').subscribe(garder);
    const requete = http.expectOne(`${BASE}/ref-1/tarif`);
    expect(requete.request.method).toBe('GET');
    requete.flush(tarifEstime());
    expect(resultat).toEqual(tarifEstime());
  });

  it.each([
    ['estimer', (): void => { service.estimer(demande).subscribe(garder); }, `${BASE}/tarifs`],
    ['tarifer', (): void => { service.tarifer('ref-1', demande).subscribe(garder); }, `${BASE}/ref-1/tarif`],
    ['tarifRetenu', (): void => { service.tarifRetenu('ref-1').subscribe(garder); }, `${BASE}/ref-1/tarif`],
  ])('%s ne donne aucun prix en cas d’échec', (_nom, appeler, adresse) => {
    appeler();
    http.expectOne(adresse).flush(null, { status: 503, statusText: 'Indisponible' });
    expect(resultat).toBeNull();
  });
});
