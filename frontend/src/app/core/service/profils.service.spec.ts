import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProfilsService } from './profils.service';

describe('ProfilsService', () => {
  let service: ProfilsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProfilsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste les profils', () => {
    let resultat: unknown;
    service.lister().subscribe((r) => (resultat = r));
    const req = http.expectOne('/api/assurance-iard-api/admin/profils');
    expect(req.request.method).toBe('GET');
    req.flush([{ nom: 'PROSPECT', description: 'Prospect', nombreUtilisateurs: 0, permissions: [] }]);
    expect((resultat as { nom: string }[])[0].nom).toBe('PROSPECT');
  });

  it('met à jour les permissions', () => {
    let repondu = false;
    service.mettreAJourPermissions('ASSURE', [{ rubrique: 'DEVIS', droit: 'CREER', valeur: false }]).subscribe(() => (repondu = true));
    const req = http.expectOne('/api/assurance-iard-api/admin/profils/ASSURE/permissions');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ permissions: [{ rubrique: 'DEVIS', droit: 'CREER', valeur: false }] });
    req.flush(null, { status: 204, statusText: 'No Content' });
    expect(repondu).toBe(true);
  });

  it("lit l'historique d'un profil", () => {
    let resultat: unknown;
    service.lireHistorique('ASSURE').subscribe((r) => (resultat = r));
    const req = http.expectOne('/api/assurance-iard-api/admin/profils/ASSURE/historique');
    expect(req.request.method).toBe('GET');
    req.flush([]);
    expect(resultat).toEqual([]);
  });
});
