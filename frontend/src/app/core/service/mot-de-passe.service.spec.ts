import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MotDePasseService, ResultatLien } from './mot-de-passe.service';

describe('MotDePasseService', () => {
  let service: MotDePasseService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MotDePasseService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it.each([
    [204, true],
    [500, false],
  ])('demande un lien : réponse %i -> %s', (status, attendu) => {
    let resultat: boolean | undefined;
    service.demanderLien(' marie@exemple.fr ').subscribe((r) => (resultat = r));
    const requete = http.expectOne('/api/assurance-iard-api/mot-de-passe/oubli');
    expect(requete.request.body).toEqual({ email: 'marie@exemple.fr' });
    requete.flush(null, { status, statusText: String(status) });

    expect(resultat).toBe(attendu);
  });

  it.each([
    [204, 'valide'],
    [410, 'invalide'],
    [500, 'erreur-technique'],
  ])('vérifie le lien : réponse %i -> %s', (status, attendu) => {
    let resultat: ResultatLien | undefined;
    service.verifierLien('jeton').subscribe((r) => (resultat = r));
    const requete = http.expectOne('/api/assurance-iard-api/mot-de-passe/lien');
    expect(requete.request.body).toEqual({ jeton: 'jeton' });
    requete.flush(null, { status, statusText: String(status) });

    expect(resultat).toBe(attendu);
  });

  it.each([
    [204, 'valide'],
    [410, 'invalide'],
    [500, 'erreur-technique'],
  ])('réinitialise : réponse %i -> %s', (status, attendu) => {
    let resultat: ResultatLien | undefined;
    service.reinitialiser('jeton', 'Nouveau123').subscribe((r) => (resultat = r));
    const requete = http.expectOne('/api/assurance-iard-api/mot-de-passe/reinitialisation');
    expect(requete.request.body).toEqual({ jeton: 'jeton', motDePasse: 'Nouveau123' });
    requete.flush(null, { status, statusText: String(status) });

    expect(resultat).toBe(attendu);
  });
});
