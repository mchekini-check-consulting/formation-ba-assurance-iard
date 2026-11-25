import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CriteresEligibilite, MarqueVehicule, SivRechercheResponse } from '../model/devis-auto';
import { SivService } from './siv.service';

describe('SivService', () => {
  let service: SivService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SivService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("envoie POST recherche-vehicule avec l'immatriculation", () => {
    let resultat: SivRechercheResponse | null | undefined;
    service.chercherParPlaque('AB-123-CD').subscribe((r) => (resultat = r));

    const req = http.expectOne('/api/assurance-iard-api/v1/devis/auto/recherche-vehicule');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ immatriculation: 'AB-123-CD' });
    req.flush({ statut: 'TROUVE', immatriculation: 'AB-123-CD', marque: 'Peugeot', modele: '308' });

    expect(resultat?.statut).toBe('TROUVE');
    expect(resultat?.marque).toBe('Peugeot');
  });

  it("renvoie null en cas d'erreur reseau", () => {
    let resultat: SivRechercheResponse | null | undefined;
    service.chercherParPlaque('AB-123-CD').subscribe((r) => (resultat = r));

    http.expectOne('/api/assurance-iard-api/v1/devis/auto/recherche-vehicule').flush('', { status: 500, statusText: 'Server Error' });

    expect(resultat).toBeNull();
  });

  it('appelle GET referentiels/vehicules et renvoie la liste des marques', () => {
    let marques: MarqueVehicule[] | undefined;
    service.referentielVehicules().subscribe((m) => (marques = m));

    const req = http.expectOne('/api/assurance-iard-api/v1/devis/auto/referentiels/vehicules');
    expect(req.request.method).toBe('GET');
    req.flush([{ marque: 'Peugeot', modeles: [{ id: 1, modele: '308', energie: 'ESSENCE', version: null, annee: 2021, valeurNeuve: 18500 }] }]);

    expect(marques?.length).toBe(1);
    expect(marques?.[0].marque).toBe('Peugeot');
  });

  it('appelle GET referentiels/criteres-eligibilite', () => {
    let criteres: CriteresEligibilite | null | undefined;
    service.criteresEligibilite().subscribe((c) => (criteres = c));

    const req = http.expectOne('/api/assurance-iard-api/v1/devis/auto/referentiels/criteres-eligibilite');
    expect(req.request.method).toBe('GET');
    req.flush({ bonusMalusMaxLigne: 1.5, resiliationBloquante: 'NON_PAIEMENT' });

    expect(criteres?.bonusMalusMaxLigne).toBe(1.5);
    expect(criteres?.resiliationBloquante).toBe('NON_PAIEMENT');
  });
});
