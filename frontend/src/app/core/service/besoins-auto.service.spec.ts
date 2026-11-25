import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { BesoinsAutoService, BesoinsAutoRequete } from './besoins-auto.service';

describe('BesoinsAutoService', () => {
  let service: BesoinsAutoService;
  let http: HttpTestingController;

  const requete: BesoinsAutoRequete = {
    valeurVehicule: 15000,
    vehiculeIndispensable: true,
    assistance0km: false,
    protectionConducteur: true,
    brisGlace: false,
    accompagnementLitige: false,
    priorite: 'PROTECTION_LARGE',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(BesoinsAutoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envoie la requête besoins vers le bon endpoint et retourne la réponse', () => {
    const reponse = { formuleConseillee: 'TOUS_RISQUES', raisons: ['Véhicule indispensable.'], prixMensuel: 45.00 };

    service.enregistrer(1, requete).subscribe(rep => {
      expect(rep.formuleConseillee).toBe('TOUS_RISQUES');
      expect(rep.prixMensuel).toBe(45.00);
    });

    const req = http.expectOne(r => r.url.includes('/v1/devis/auto/1/besoins'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body.valeurVehicule).toBe(15000);
    req.flush(reponse);
  });
});
