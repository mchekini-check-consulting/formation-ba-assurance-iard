import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { InactiviteService } from '../service/inactivite.service';
import { droitGuard } from './droit.guard';

@Component({ template: '' })
class PageVide {}

describe('droitGuard (US-04)', () => {
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
          { path: 'acces-non-autorise', component: PageVide },
          { path: 'contrats', component: PageVide, canActivate: [droitGuard('CONTRATS', 'CONSULTER')] },
          {
            path: 'back-office',
            component: PageVide,
            canActivate: [droitGuard('COMPTES_UTILISATEURS', 'CONSULTER', true)],
          },
        ]),
      ],
    });
    router = TestBed.inject(Router);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const naviguer = async (url: string, session: object): Promise<void> => {
    sessionStorage.setItem('assureasy.session', 'session');
    const navigation = router.navigateByUrl(url);
    await vi.waitFor(() => http.expectOne('/api/assurance-iard-api/session').flush(session));
    await navigation;
  };

  it('renvoie vers la connexion sans session', async () => {
    await router.navigateByUrl('/contrats');
    expect(router.url).toBe('/connexion?retour=%2Fcontrats');
  });

  it('ouvre la page avec le droit, relu auprès du serveur', async () => {
    await naviguer('/contrats', { email: 'a@exemple.fr', profil: 'ASSURE', droits: ['CONTRATS:CONSULTER'] });
    expect(router.url).toBe('/contrats');
    expect(surveiller).toHaveBeenCalled();
  });

  it('AC5 : un droit retiré mène à « Accès non autorisé »', async () => {
    await naviguer('/contrats', { email: 'a@exemple.fr', profil: 'ASSURE', droits: [] });
    expect(router.url).toBe('/acces-non-autorise');
  });

  it("AC3 : un assuré n'entre jamais dans le back-office, même avec le droit", async () => {
    await naviguer('/back-office', {
      email: 'a@exemple.fr',
      profil: 'ASSURE',
      droits: ['COMPTES_UTILISATEURS:CONSULTER'],
    });
    expect(router.url).toBe('/acces-non-autorise');
  });

  it('le gestionnaire entre dans le back-office avec le droit', async () => {
    await naviguer('/back-office', {
      email: 'g@exemple.fr',
      profil: 'GESTIONNAIRE',
      droits: ['COMPTES_UTILISATEURS:CONSULTER'],
    });
    expect(router.url).toBe('/back-office');
  });
});
