import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { HelloService } from './hello.service';

describe('HelloService', () => {
  let service: HelloService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HelloService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('appelle GET /hello de assurance-iard-api et renvoie le texte', () => {
    let message: string | undefined;
    service.direBonjour().subscribe((m) => (message = m));

    // Les tests tournent sur localhost : adresse du proxy local
    const requete = http.expectOne('/api/assurance-iard-api/hello');
    expect(requete.request.method).toBe('GET');
    requete.flush('Hello Assurance IARD');

    expect(message).toBe('Hello Assurance IARD');
  });
});
