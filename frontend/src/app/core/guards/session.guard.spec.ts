import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ConnexionService } from '../service/connexion.service';
import { InactiviteService } from '../service/inactivite.service';
import { sessionGuard, verificationGuard } from './session.guard';

@Component({ template: '' })
class PageVide {}

describe('gardes de connexion', () => {
  let router: Router;
  let http: HttpTestingController;
  const surveiller = vi.fn();

  beforeEach(() => {
    sessionStorage.clear();
    surveiller.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: InactiviteService, useValue: { surveiller } },
        provideRouter([
          { path: 'connexion', component: PageVide },
          { path: 'double-authentification', component: PageVide, canActivate: [verificationGuard] },
          { path: 'espace-client/documents', component: PageVide, canActivate: [sessionGuard] },
        ]),
      ],
    });
    router = TestBed.inject(Router);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('renvoie vers la connexion en retenant la page demandée (AC6)', async () => {
    await router.navigateByUrl('/espace-client/documents');
    expect(router.url).toBe('/connexion?retour=%2Fespace-client%2Fdocuments');
    expect(surveiller).not.toHaveBeenCalled();
  });

  it('ouvre la page demandée à un client connecté et surveille son inactivité', async () => {
    sessionStorage.setItem('assureasy.session', 'session');
    const navigation = router.navigateByUrl('/espace-client/documents');
    await vi.waitFor(() => http.expectOne('/api/session').flush({ email: 'marie@exemple.fr' }));
    await navigation;
    expect(router.url).toBe('/espace-client/documents');
    expect(surveiller).toHaveBeenCalled();
  });

  it('renvoie vers la connexion si la session a expiré', async () => {
    sessionStorage.setItem('assureasy.session', 'expiree');
    const navigation = router.navigateByUrl('/espace-client/documents');
    await vi.waitFor(() => http.expectOne('/api/session').flush({}, { status: 401, statusText: 'Unauthorized' }));
    await navigation;
    expect(router.url).toBe('/connexion?retour=%2Fespace-client%2Fdocuments');
  });

  it("refuse l'écran de code sans mot de passe validé (RG1)", async () => {
    await router.navigateByUrl('/double-authentification');
    expect(router.url).toBe('/connexion');
  });

  it("ouvre l'écran de code après la validation du mot de passe", async () => {
    TestBed.inject(ConnexionService).verification.set({ verification: 'v1', emailMasque: 'm***@exemple.fr' });
    await router.navigateByUrl('/double-authentification');
    expect(router.url).toBe('/double-authentification');
  });
});
