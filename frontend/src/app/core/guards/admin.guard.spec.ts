import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ConnexionService } from '../service/connexion.service';
import { adminGuard } from './admin.guard';

@Component({ template: '' })
class PageVide {}

describe('adminGuard', () => {
  let router: Router;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'connexion', component: PageVide },
          { path: 'espace-client', component: PageVide },
          { path: 'admin/profils', component: PageVide, canActivate: [adminGuard] },
        ]),
      ],
    });
    router = TestBed.inject(Router);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('redirige vers la connexion sans session', async () => {
    await router.navigateByUrl('/admin/profils');
    expect(router.url).toBe('/connexion');
  });

  it("redirige vers l'espace client si le profil n'est pas administrateur", async () => {
    sessionStorage.setItem('assureasy.session', 'session');
    const navigation = router.navigateByUrl('/admin/profils');
    await vi.waitFor(() =>
      http.expectOne('/api/assurance-iard-api/session').flush({ email: 'marie@exemple.fr', profil: 'ASSURE' }),
    );
    await navigation;
    expect(router.url).toBe('/espace-client');
  });

  it("ouvre la page pour un administrateur", async () => {
    sessionStorage.setItem('assureasy.session', 'session');
    TestBed.inject(ConnexionService).profilClient.set('ADMINISTRATEUR');
    const navigation = router.navigateByUrl('/admin/profils');
    await vi.waitFor(() =>
      http.expectOne('/api/assurance-iard-api/session').flush({ email: 'admin@exemple.fr', profil: 'ADMINISTRATEUR' }),
    );
    await navigation;
    expect(router.url).toBe('/admin/profils');
  });
});
