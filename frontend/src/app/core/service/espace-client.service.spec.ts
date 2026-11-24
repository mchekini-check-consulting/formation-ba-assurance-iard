import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TableauDeBord } from '../model/tableau-de-bord';
import { EspaceClientService } from './espace-client.service';

const URL_TABLEAU = '/api/assurance-iard-api/espace-client/tableau-de-bord';

describe('EspaceClientService (US-18)', () => {
  let service: EspaceClientService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(EspaceClientService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lit le tableau de bord par le relais local et retient l’identité du compte', () => {
    const resultats: (TableauDeBord | null)[] = [];
    service.tableauDeBord('camille@exemple.fr').subscribe((r) => resultats.push(r));
    const requete = http.expectOne(URL_TABLEAU);
    expect(requete.request.method).toBe('GET');
    requete.flush({ prenom: 'Camille', nom: 'Durand' });

    expect(resultats[0]?.prenom).toBe('Camille');
    expect(service.identite()).toEqual({ email: 'camille@exemple.fr', prenom: 'Camille', nom: 'Durand' });
  });

  it('deux demandes simultanées (page et en-tête) ne font qu’un appel', () => {
    const resultats: (TableauDeBord | null)[] = [];
    service.tableauDeBord().subscribe((r) => resultats.push(r));
    service.tableauDeBord().subscribe((r) => resultats.push(r));
    http.expectOne(URL_TABLEAU).flush({ prenom: 'Alice', nom: 'Martin' });
    expect(resultats).toHaveLength(2);

    // Une fois la réponse reçue, une nouvelle demande relit le serveur.
    service.tableauDeBord().subscribe();
    http.expectOne(URL_TABLEAU).flush({ prenom: 'Alice', nom: 'Martin' });
  });

  it("renvoie null si le tableau de bord n'a pas pu être chargé", () => {
    const resultats: (TableauDeBord | null)[] = [];
    service.tableauDeBord().subscribe((r) => resultats.push(r));
    http.expectOne(URL_TABLEAU).flush({}, { status: 500, statusText: 'x' });
    expect(resultats).toEqual([null]);
    expect(service.identite()).toBeNull();
  });
});
