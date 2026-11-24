import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TableauDeBord } from '../model/tableau-de-bord';
import { EspaceClientService } from './espace-client.service';

describe('EspaceClientService (US-18)', () => {
  let service: EspaceClientService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(EspaceClientService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lit le tableau de bord par le relais local', () => {
    const resultats: (TableauDeBord | null)[] = [];
    service.tableauDeBord().subscribe((r) => resultats.push(r));
    const requete = http.expectOne('/api/assurance-iard-api/espace-client/tableau-de-bord');
    expect(requete.request.method).toBe('GET');
    requete.flush({ prenom: 'Alice' });
    expect(resultats[0]?.prenom).toBe('Alice');
  });

  it("renvoie null si le tableau de bord n'a pas pu être chargé", () => {
    const resultats: (TableauDeBord | null)[] = [];
    service.tableauDeBord().subscribe((r) => resultats.push(r));
    http
      .expectOne('/api/assurance-iard-api/espace-client/tableau-de-bord')
      .flush({}, { status: 500, statusText: 'x' });
    expect(resultats).toEqual([null]);
  });
});
