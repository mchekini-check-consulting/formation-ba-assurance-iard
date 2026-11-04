import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
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

  it("déconnecte le client et revient à l'accueil", () => {
    page.querySelector('button')!.click();
    http.expectOne('/api/session').flush(null, { status: 204, statusText: 'No Content' });

    expect(connexionService.jeton()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });
});
