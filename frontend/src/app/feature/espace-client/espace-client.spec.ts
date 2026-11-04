import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AnnonceService } from '../../core/service/annonce.service';
import { ConnexionService } from '../../core/service/connexion.service';
import { EspaceClient } from './espace-client';

describe('EspaceClient', () => {
  let page: HTMLElement;
  let router: Router;
  let http: HttpTestingController;
  let connexionService: ConnexionService;

  beforeEach(() => {
    sessionStorage.setItem('assureasy.session', 'session');
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    router = TestBed.inject(Router);
    http = TestBed.inject(HttpTestingController);
    connexionService = TestBed.inject(ConnexionService);
    connexionService.emailClient.set('marie@exemple.fr');
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(EspaceClient);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('affiche le client connecté', () => {
    expect(page.querySelector('h1')?.textContent).toContain('Mon espace');
    expect(page.querySelector('strong')?.textContent).toBe('marie@exemple.fr');
  });

  it("propose « Se déconnecter » dans l'en-tête (US-07, AC3)", () => {
    expect(page.querySelector('header button')?.textContent).toContain('Se déconnecter');
  });

  it("ferme la session, revient à l'accueil et confirme la déconnexion (US-07, AC3)", () => {
    page.querySelector<HTMLButtonElement>('header button')!.click();
    const requete = http.expectOne('/api/session');
    expect(requete.request.method).toBe('DELETE');
    requete.flush(null, { status: 204, statusText: 'No Content' });

    expect(connexionService.jeton()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
    expect(TestBed.inject(AnnonceService).prendre()).toBe('Vous êtes bien déconnecté.');
  });
});
