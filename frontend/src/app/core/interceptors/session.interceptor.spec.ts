import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { sessionInterceptor } from './session.interceptor';

describe('sessionInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([sessionInterceptor])), provideHttpClientTesting()],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('ajoute le jeton aux appels vers le backend', () => {
    sessionStorage.setItem('assureasy.session', 'session');
    client.get('/api/assurance-iard-api/session').subscribe();
    const requete = http.expectOne('/api/assurance-iard-api/session');
    expect(requete.request.headers.get('Authorization')).toBe('Bearer session');
    requete.flush({});
  });

  it("n'envoie jamais le jeton à une autre adresse", () => {
    sessionStorage.setItem('assureasy.session', 'session');
    client.get('https://autre.example/donnees').subscribe();
    const requete = http.expectOne('https://autre.example/donnees');
    expect(requete.request.headers.has('Authorization')).toBe(false);
    requete.flush({});
  });

  it("n'ajoute rien sans session", () => {
    client.get('/api/assurance-iard-api/session').subscribe();
    const requete = http.expectOne('/api/assurance-iard-api/session');
    expect(requete.request.headers.has('Authorization')).toBe(false);
    requete.flush({});
  });
});
