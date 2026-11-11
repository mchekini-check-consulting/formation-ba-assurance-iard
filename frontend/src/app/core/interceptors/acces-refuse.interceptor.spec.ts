import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { accesRefuseInterceptor } from './acces-refuse.interceptor';

describe('accesRefuseInterceptor (US-04, AC2)', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([accesRefuseInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  it('affiche « Accès non autorisé » quand le serveur refuse', () => {
    let erreur: unknown;
    client.get('/api/assurance-iard-api/documents/9').subscribe({ error: (e: unknown) => (erreur = e) });
    http.expectOne('/api/assurance-iard-api/documents/9').flush({}, { status: 403, statusText: 'Forbidden' });

    expect(router.navigateByUrl).toHaveBeenCalledWith('/acces-non-autorise');
    expect(erreur).toBeDefined();
  });

  it('laisse passer les autres erreurs et les autres adresses', () => {
    client.get('/api/assurance-iard-api/documents/9').subscribe({ error: () => undefined });
    http.expectOne('/api/assurance-iard-api/documents/9').flush({}, { status: 404, statusText: 'Not Found' });
    client.get('https://autre.example/x').subscribe({ error: () => undefined });
    http.expectOne('https://autre.example/x').flush({}, { status: 403, statusText: 'Forbidden' });

    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
